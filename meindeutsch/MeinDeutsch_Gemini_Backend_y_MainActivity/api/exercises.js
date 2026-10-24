// Mein Deutsch API — Gemini (nivel gratuito cuando esté disponible)
// Compatible con Mein Deutsch v2.3: 12 ejercicios (2 de cada tipo).

const ALLOWED_TYPES = new Set(["test", "fill", "match", "order", "translate", "conjugate"]);
const TYPES = ["test", "fill", "match", "order", "translate", "conjugate"];

function send(res, status, data) {
  return res.status(status).json(data);
}

function shuffle(items) {
  const result = [...items];
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
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

  if (exercises.length !== 12) return null;
  for (const type of TYPES) {
    if (exercises.filter((e) => e.type === type).length !== 2) return null;
  }
  for (const e of exercises) {
    if (e.type === "test" && (e.options.length < 3 || !e.correctAnswer)) return null;
    if (["fill", "translate", "conjugate"].includes(e.type) && !e.correctAnswer && e.acceptedAnswers.length === 0) return null;
    if (e.type === "match" && e.pairs.length < 3) return null;
    if (e.type === "order" && (e.options.length < 3 || !e.correctAnswer)) return null;
  }

  for (const e of exercises) {
    if (e.type === "order") e.options = shuffle(e.options);
  }
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
      service: "Mein Deutsch AI (Gemini)",
      message: "Servidor disponible. Para generar ejercicios se utiliza POST."
    });
  }
  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST, OPTIONS");
    return send(res, 405, { error: "Método no permitido." });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return send(res, 503, { error: "Falta configurar GEMINI_API_KEY en las variables de entorno de Vercel." });
  }

  try {
    const body = typeof req.body === "string" ? JSON.parse(req.body) : (req.body || {});
    const unit = Number(body.unit);
    if (unit !== 1 && unit !== 2) return send(res, 400, { error: "La unidad debe ser 1 o 2." });
    const recentQuestions = Array.isArray(body.recentQuestions)
      ? body.recentQuestions.filter((q) => typeof q === "string").slice(-25)
      : [];

    const unitFocus = unit === 1
      ? "Unidad 1 de Netzwerk neu A1.1: saludos y despedidas (Hallo, Guten Morgen, Guten Tag, Guten Abend, Tschüs, Bis bald, Auf Wiedersehen); preguntar y decir el nombre; trato informal du y formal Sie; preguntar cómo está una persona y responder; origen y residencia con kommen aus / wohnen in; idiomas y países; pronombres personales; conjugación inicial de sein, heißen, kommen, wohnen, sprechen; números 0-20, números de teléfono, alfabeto, deletrear, correo electrónico y pedir que repitan o hablen más despacio."
      : "Unidad 2 de Netzwerk neu A1.1: amigos y compañeros, aficiones y actividades cotidianas, expresar gustos con gern, quedar y preguntar cuándo, días de la semana, profesiones y conjugaciones sencillas de verbos regulares y verbos de la unidad como kochen, arbeiten, lesen, sprechen, fotografieren, gehen y spielen. Mantén el nivel A1.1.";

    const prompt = `Eres profesor de alemán para hispanohablantes de nivel A1.1. Crea ejercicios de práctica ORIGINALES basados solo en estos temas: ${unitFocus}\n\nUnidad: ${unit}. Evita repetir estas preguntas recientes si puedes: ${JSON.stringify(recentQuestions)}.\n\nDevuelve ÚNICAMENTE un objeto JSON válido con una propiedad "exercises" que contenga EXACTAMENTE 12 ejercicios: 2 "test", 2 "fill", 2 "match", 2 "order", 2 "translate" y 2 "conjugate". No añadas texto fuera del JSON.\nCada ejercicio debe incluir siempre: type, question, options (array), correctAnswer (cadena), acceptedAnswers (array), pairs (array de objetos {german,spanish}) y explanation (en español). Para campos no utilizados, usa arrays vacíos y cadenas vacías.\nReglas: \n- test: pregunta en español, 3 opciones como mínimo y correctAnswer idéntica a una opción.\n- fill: frase alemana con ___; correctAnswer es solo la palabra que falta.\n- match: instrucción en español y 4 pares en pairs, con palabras o frases alemanas y traducción española correcta.\n- order: opciones con fragmentos de una frase alemana correcta; correctAnswer contiene la frase final con puntuación. No incluyas en options la frase ya ordenada.\n- translate: pide traducir una expresión breve al alemán.\n- conjugate: pide conjugar un verbo específico para una persona concreta (ich, du, er/sie/es, wir, ihr, sie/Sie); incluye una frase con ___ y correctAnswer es solo la forma verbal. En la unidad 1 usa sobre todo sein, heißen, kommen, wohnen y sprechen; en la unidad 2 puedes usar kochen, arbeiten, lesen, sprechen, spielen, gehen y verbos sencillos de las aficiones. Respeta las formas reales: ich koche/arbeite/lese/spreche; du kochst/arbeitest/liest/sprichst; er/sie/es kocht/arbeitet/liest/spricht; wir y sie/Sie suelen terminar en -en; ihr termina en -t (por ejemplo, ihr kocht, arbeitet, lest, sprecht).\nLas explicaciones deben ser breves, en español y gramaticalmente correctas. No afirmes que son copias literales del libro. Evita ejercicios repetidos y no incluyas gramática por encima de A1.1.`;

    const model = process.env.GEMINI_MODEL || "gemini-3.8-flash";
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
    const upstream = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey
      },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: {
          temperature: 0.7,
          maxOutputTokens: 7000,
          responseMimeType: "application/json"
        }
      }),
      signal: AbortSignal.timeout(30000)
    });

    const rawText = await upstream.text();
    let upstreamData;
    try { upstreamData = JSON.parse(rawText); }
    catch { throw new Error(`Gemini devolvió una respuesta no JSON (HTTP ${upstream.status}).`); }

    if (!upstream.ok) {
      const message = upstreamData?.error?.message || `Error de Gemini API (HTTP ${upstream.status}).`;
      console.error("Gemini API error:", upstream.status, message);
      return send(res, 502, { error: "No se pudieron generar ejercicios con Gemini.", detail: message });
    }

    const content = (upstreamData?.candidates?.[0]?.content?.parts || [])
      .map((part) => typeof part.text === "string" ? part.text : "")
      .join("")
      .trim();
    if (!content) return send(res, 502, { error: "Gemini no devolvió contenido de ejercicios." });

    let decoded;
    try { decoded = JSON.parse(content); }
    catch {
      console.error("Gemini devolvió JSON no válido.");
      return send(res, 502, { error: "Gemini devolvió un formato de ejercicios no válido. Vuelve a intentarlo." });
    }
    const exercises = validateExercises(decoded);
    if (!exercises) {
      console.error("Gemini devolvió ejercicios que no cumplen la estructura requerida.");
      return send(res, 502, { error: "Gemini no devolvió los 12 ejercicios con el formato requerido. Vuelve a intentarlo." });
    }

    return send(res, 200, { exercises, generatedBy: "gemini", unit });
  } catch (error) {
    console.error("Mein Deutsch Gemini API error:", error?.message || error);
    return send(res, 500, { error: "Error al generar ejercicios.", detail: error?.message || "Error desconocido." });
  }
};
