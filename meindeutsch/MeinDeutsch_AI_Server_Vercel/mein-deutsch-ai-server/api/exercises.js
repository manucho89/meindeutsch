const ALLOWED_TYPES = new Set(["test", "fill", "match", "order", "translate"]);
const TYPES = ["test", "fill", "match", "order", "translate"];

function send(res, status, data) {
  res.status(status).json(data);
}

function shuffle(items) {
  const result = [...items];
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

function extractJsonContent(response) {
  // Chat Completions normally puts the generated JSON in message.content.
  const content = response?.choices?.[0]?.message?.content;
  if (typeof content !== "string" || !content.trim()) {
    throw new Error("La IA no devolvió contenido JSON.");
  }
  return JSON.parse(content);
}

function validateExercises(input) {
  if (!input || !Array.isArray(input.exercises)) return null;
  const exercises = input.exercises
    .filter((e) => e && typeof e === "object" && ALLOWED_TYPES.has(e.type))
    .map((e) => ({
      type: e.type,
      question: typeof e.question === "string" ? e.question.trim() : "",
      options: Array.isArray(e.options) ? e.options.filter((v) => typeof v === "string").slice(0, 12) : [],
      correctAnswer: typeof e.correctAnswer === "string" ? e.correctAnswer.trim() : "",
      acceptedAnswers: Array.isArray(e.acceptedAnswers) ? e.acceptedAnswers.filter((v) => typeof v === "string").slice(0, 8) : [],
      pairs: Array.isArray(e.pairs) ? e.pairs
        .filter((p) => p && typeof p.german === "string" && typeof p.spanish === "string" && p.german.trim() && p.spanish.trim())
        .slice(0, 6)
        .map((p) => ({ german: p.german.trim(), spanish: p.spanish.trim() })) : [],
      explanation: typeof e.explanation === "string" ? e.explanation.trim() : ""
    }))
    .filter((e) => e.question && e.explanation);

  // The Android app expects two exercises of each of these five types.
  if (exercises.length !== 10) return null;
  for (const type of TYPES) {
    if (exercises.filter((e) => e.type === type).length !== 2) return null;
  }
  for (const e of exercises) {
    if (e.type === "test" && (e.options.length < 3 || !e.correctAnswer)) return null;
    if (e.type === "fill" && !e.correctAnswer && e.acceptedAnswers.length === 0) return null;
    if (e.type === "match" && e.pairs.length < 3) return null;
    if (e.type === "order" && (e.options.length < 3 || !e.correctAnswer)) return null;
    if (e.type === "translate" && !e.correctAnswer && e.acceptedAnswers.length === 0) return null;
  }

  // Shuffle word tiles so the learner has to reconstruct the sentence.
  for (const e of exercises) {
    if (e.type === "order") e.options = shuffle(e.options);
  }
  // Fixed alternating sequence matches the Android session's expected presentation.
  const pool = [...exercises];
  return TYPES.flatMap((type) => [0, 1].map(() => {
    const idx = pool.findIndex((e) => e.type === type);
    return pool.splice(idx, 1)[0];
  }));
}

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(204).end();
  if (req.method === "GET") {
    return send(res, 200, {
      ok: true,
      service: "Mein Deutsch AI",
      message: "Servidor disponible. La generación de ejercicios se solicita mediante POST."
    });
  }
  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST, OPTIONS");
    return send(res, 405, { error: "Método no permitido." });
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return send(res, 503, { error: "Falta configurar la variable OPENAI_API_KEY en Vercel." });
  }

  try {
    const body = typeof req.body === "string" ? JSON.parse(req.body) : (req.body || {});
    const unit = Number(body.unit);
    if (unit !== 1 && unit !== 2) {
      return send(res, 400, { error: "La unidad debe ser 1 o 2." });
    }
    const recentQuestions = Array.isArray(body.recentQuestions)
      ? body.recentQuestions.filter((q) => typeof q === "string").slice(-25)
      : [];

    const unitFocus = unit === 1
      ? "Unidad 1: saludos y despedidas; presentarse; preguntar y decir nombre; origen y lugar donde se vive; idiomas; pronombres personales; conjugaciones iniciales de sein y heißen; alfabeto, números y datos personales."
      : "Unidad 2: amigos y compañeros; aficiones; actividades en tiempo libre; expresar gustos con gern; quedar y preguntar cuándo; días de la semana; frases sencillas sobre personas y profesiones cuando encaje con A1.1.";

    const instructions = `Eres un profesor cuidadoso de alemán para hispanohablantes que estudian Netzwerk neu A1.1. Crea material de práctica ORIGINAL para nivel A1.1, basado solo en los temas indicados; no afirmes que los ejercicios son copias literales del libro. Unidad solicitada: ${unit}. Temas: ${unitFocus}
Devuelve un objeto JSON con una propiedad exercises que contenga EXACTAMENTE 10 ejercicios: EXACTAMENTE 2 de cada tipo: test, fill, match, order y translate.
Cada ejercicio debe tener estos campos: type, question, options (array), correctAnswer (string), acceptedAnswers (array de strings), pairs (array de objetos {german,spanish}), explanation (string). Incluye todos los campos siempre. Para los campos no usados, utiliza arrays vacíos o cadena vacía.
Reglas por tipo:
- test: pregunta en español, exactamente 3 opciones en español o alemán según tenga sentido, correctAnswer debe coincidir exactamente con una opción.
- fill: una frase alemana con ___ donde falta una palabra o forma verbal; correctAnswer debe ser solo la palabra/frase que falta. Añade acceptedAnswers solo para variantes ortográficas válidas.
- match: instrucción en español y 4 parejas claras en pairs; cada objeto tiene german y spanish.
- order: instrucción en español, options debe contener cada palabra o fragmento de la frase alemana correcta, y correctAnswer debe ser la frase final con la puntuación. Revuelve las palabras de options.
- translate: pregunta en español que pida traducir una expresión breve al alemán; correctAnswer es una respuesta alemana natural y válida. Añade variantes aceptadas cuando proceda.
Todas las explicaciones deben estar en español, ser breves y correctas. Usa alemán natural y gramática adecuada. No incluyas materia por encima de A1.1. Evita repetir las preguntas recientes si es posible, y crea ejemplos diferentes cada vez. No añadas texto fuera del JSON.`;

    const prompt = `Genera los 10 ejercicios indicados para la unidad ${unit}. Evita estas preguntas ya usadas: ${JSON.stringify(recentQuestions)}. Recuerda: 2 de cada tipo, 10 en total. Devuelve JSON con exercises.`;

    const upstream = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: process.env.OPENAI_MODEL || "gpt-4.1-mini",
        temperature: 0.8,
        response_format: { type: "json_object" },
        messages: [
          { role: "system", content: instructions },
          { role: "user", content: prompt }
        ]
      }),
      signal: AbortSignal.timeout(30000)
    });

    const rawText = await upstream.text();
    let upstreamData;
    try { upstreamData = JSON.parse(rawText); }
    catch { throw new Error(`La API de IA devolvió una respuesta no JSON (HTTP ${upstream.status}).`); }

    if (!upstream.ok) {
      const message = upstreamData?.error?.message || `Error de la API de IA (HTTP ${upstream.status}).`;
      console.error("OpenAI API error:", upstream.status, message);
      return send(res, 502, { error: "No se pudieron generar ejercicios con IA.", detail: message });
    }

    const decoded = extractJsonContent(upstreamData);
    const exercises = validateExercises(decoded);
    if (!exercises) {
      console.error("La IA devolvió ejercicios que no cumplen la estructura requerida.");
      return send(res, 502, { error: "La IA no devolvió los 10 ejercicios con el formato requerido. Vuelve a intentarlo." });
    }

    return send(res, 200, { exercises, generatedBy: "openai", unit });
  } catch (error) {
    console.error("Mein Deutsch API error:", error?.message || error);
    return send(res, 500, { error: "Error al generar ejercicios.", detail: error?.message || "Error desconocido." });
  }
};
