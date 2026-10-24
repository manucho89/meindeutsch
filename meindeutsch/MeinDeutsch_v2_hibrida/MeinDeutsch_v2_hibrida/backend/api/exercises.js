const VALID_TYPES = ["test", "fill", "match", "order", "translate"];
const WINDOW_MS = 60 * 60 * 1000;
const MAX_REQUESTS_PER_WINDOW = 12;

// Best-effort per-instance limiter. For a public production app, replace with
// a shared rate limiter (e.g. Vercel KV/Upstash) and authenticated users.
const bucket = globalThis.__meinDeutschRateBucket || new Map();
globalThis.__meinDeutschRateBucket = bucket;

const schema = {
  type: "object",
  additionalProperties: false,
  required: ["exercises"],
  properties: {
    exercises: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["type", "question", "options", "correctAnswer", "acceptedAnswers", "pairs", "explanation"],
        properties: {
          type: { type: "string", enum: VALID_TYPES },
          question: { type: "string" },
          options: { type: "array", items: { type: "string" } },
          correctAnswer: { type: "string" },
          acceptedAnswers: { type: "array", items: { type: "string" } },
          pairs: {
            type: "array",
            items: {
              type: "object",
              additionalProperties: false,
              required: ["german", "spanish"],
              properties: {
                german: { type: "string" },
                spanish: { type: "string" }
              }
            }
          },
          explanation: { type: "string" }
        }
      }
    }
  }
};

function getClientIp(req) {
  const forwarded = req.headers["x-forwarded-for"];
  if (typeof forwarded === "string" && forwarded.length) return forwarded.split(",")[0].trim();
  return "unknown";
}

function allowRequest(ip) {
  const now = Date.now();
  const entry = bucket.get(ip);
  if (!entry || now - entry.start >= WINDOW_MS) {
    bucket.set(ip, { start: now, count: 1 });
    return true;
  }
  if (entry.count >= MAX_REQUESTS_PER_WINDOW) return false;
  entry.count += 1;
  // Prevent the in-memory map from growing without bound.
  if (bucket.size > 5000) {
    for (const [key, value] of bucket.entries()) {
      if (now - value.start >= WINDOW_MS) bucket.delete(key);
    }
  }
  return true;
}

function validateExerciseSet(exercises) {
  if (!Array.isArray(exercises) || exercises.length !== 10) return false;
  const counts = Object.fromEntries(VALID_TYPES.map((type) => [type, 0]));
  for (const ex of exercises) {
    if (!ex || !VALID_TYPES.includes(ex.type) || typeof ex.question !== "string" || !ex.question.trim()) return false;
    counts[ex.type] += 1;
    if (ex.type === "test") {
      if (!Array.isArray(ex.options) || ex.options.length < 3 || !ex.correctAnswer || !ex.options.includes(ex.correctAnswer)) return false;
    }
    if (ex.type === "fill" || ex.type === "translate") {
      if (!ex.correctAnswer && (!Array.isArray(ex.acceptedAnswers) || ex.acceptedAnswers.length === 0)) return false;
    }
    if (ex.type === "match") {
      if (!Array.isArray(ex.pairs) || ex.pairs.length < 3 || ex.pairs.length > 4) return false;
      if (ex.pairs.some((pair) => !pair.german || !pair.spanish)) return false;
    }
    if (ex.type === "order") {
      if (!Array.isArray(ex.options) || ex.options.length < 3 || !ex.correctAnswer) return false;
    }
    if (!Array.isArray(ex.acceptedAnswers) || !Array.isArray(ex.options) || !Array.isArray(ex.pairs)) return false;
    if (typeof ex.correctAnswer !== "string" || typeof ex.explanation !== "string") return false;
  }
  return VALID_TYPES.every((type) => counts[type] === 2);
}

function extractOutputText(responseJson) {
  const chunks = [];
  for (const item of responseJson.output || []) {
    if (item.type !== "message") continue;
    for (const part of item.content || []) {
      if (part.type === "output_text" && typeof part.text === "string") chunks.push(part.text);
    }
  }
  return chunks.join("\n").trim();
}

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");

  if (req.method === "OPTIONS") return res.status(204).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Usa POST para solicitar ejercicios." });

  if (!process.env.OPENAI_API_KEY) {
    return res.status(503).json({ error: "Falta configurar OPENAI_API_KEY en Vercel." });
  }

  const ip = getClientIp(req);
  if (!allowRequest(ip)) {
    return res.status(429).json({ error: "Se ha alcanzado el límite temporal de generación. Prueba más tarde." });
  }

  try {
    const body = req.body && typeof req.body === "object" ? req.body : {};
    const unit = Number(body.unit);
    if (![1, 2].includes(unit)) return res.status(400).json({ error: "La unidad debe ser 1 o 2." });

    const recentQuestions = Array.isArray(body.recentQuestions)
      ? body.recentQuestions.filter((q) => typeof q === "string").slice(-25).map((q) => q.slice(0, 180))
      : [];

    const unitContext = unit === 1
      ? `UNIDAD 1: Guten Tag! Nivel inicial A1.1. Limítate a saludos y despedidas, presentarse y preguntar nombres, origen y lugar de residencia, pronombres personales, formas básicas de sein y heißen, países/idiomas y expresiones sencillas para deletrear o pedir que repitan. Usa únicamente frases de principiante. Vocabulario orientativo: Hallo, Guten Morgen, Guten Tag, Guten Abend, Gute Nacht, Tschüs, Auf Wiedersehen, Danke, Bitte, Ich heiße…, Wie heißt du?, Wie heißen Sie?, Woher kommst du?, Ich komme aus Spanien, Wo wohnst du?, Ich wohne in Barcelona, ich/du/er/sie/es/wir/ihr/sie/Sie, bin/bist/ist/sind/seid, heiße/heißt/heißen, Sprache, Zahl.`
      : `UNIDAD 2: Freunde, Kollegen und ich. Nivel inicial A1.1. Limítate a amigos y compañeros, aficiones, gustos con gern, días de la semana, propuestas sencillas para quedar y algunas profesiones/pronombres con artículos. Vocabulario orientativo: der Freund, die Freundin, der Kollege, die Kollegin, fotografieren, joggen, singen, tanzen, kochen, reisen, schwimmen, Musik hören, Fußball spielen, gern, Montag, Dienstag, Wochenende, Wann?, Gehen wir ins Kino?, Ja, gern!, Das geht leider nicht, der Beruf, der Arzt, die Ärztin.`;

    const instructions = `Eres el generador de actividades de una app de alemán para una persona hispanohablante que estudia Netzwerk neu A1.1. Crea ejercicios ORIGINALES, no copies preguntas del libro ni afirmes que son actividades literales. Todo debe estar en nivel A1.1 y limitado al vocabulario/estructuras permitidas abajo. Si una frase puede tener varias respuestas válidas, incluye todas en acceptedAnswers o elige otra frase inequívoca. Las explicaciones deben ser breves y en español.\n\nGenera exactamente 10 ejercicios: exactamente dos de cada type: test, fill, match, order y translate. Devuélvelos en el orden: test, fill, match, order, translate y luego repite ese orden.\n\nEsquema de cada ejercicio:\n- test: question en español o pregunta sencilla, options con exactamente 3 opciones, correctAnswer que coincida exactamente con una opción.\n- fill: question es una frase con ___, correctAnswer es la palabra que falta; acceptedAnswers contiene variantes ortográficas válidas si las hay.\n- match: question es una instrucción; pairs debe contener 3 o 4 parejas german/spanish únicas.\n- order: question pide ordenar palabras; options contiene las palabras desordenadas como fichas, correctAnswer es la frase final con puntuación correcta. No uses más de una respuesta razonable.\n- translate: question empieza con “Traduce al alemán:” y muestra una frase en español; correctAnswer es la traducción alemana y acceptedAnswers incluye variantes válidas cuando proceda.\n\nEvita repetir literalmente estos enunciados recientes: ${JSON.stringify(recentQuestions)}\n\nCONTENIDO AUTORIZADO DE LA UNIDAD:\n${unitContext}`;

    const aiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: process.env.OPENAI_MODEL || "gpt-6-luna",
        reasoning: { effort: "none" },
        instructions,
        input: `Prepara ahora la sesión nueva para la unidad ${unit}. Devuelve únicamente el objeto que cumpla el esquema JSON indicado.`,
        max_output_tokens: 6000,
        store: false,
        text: {
          format: {
            type: "json_schema",
            name: "mein_deutsch_exercises",
            strict: true,
            schema
          }
        }
      })
    });

    const aiText = await aiResponse.text();
    let aiJson;
    try { aiJson = JSON.parse(aiText); } catch { aiJson = null; }
    if (!aiResponse.ok) {
      console.error("OpenAI error", aiResponse.status, aiText.slice(0, 1500));
      return res.status(502).json({ error: "El servicio de IA no ha podido generar los ejercicios." });
    }
    const outputText = extractOutputText(aiJson || {});
    if (!outputText) return res.status(502).json({ error: "La IA no devolvió el contenido de los ejercicios." });

    let generated;
    try { generated = JSON.parse(outputText); } catch {
      return res.status(502).json({ error: "La respuesta de IA no tenía un JSON válido." });
    }
    if (!validateExerciseSet(generated.exercises)) {
      console.error("Invalid AI exercise set", JSON.stringify(generated).slice(0, 2000));
      return res.status(502).json({ error: "Los ejercicios generados no pasaron la validación." });
    }
    return res.status(200).json({ source: "ai", unit, exercises: generated.exercises });
  } catch (error) {
    console.error("Exercise generation failed", error);
    return res.status(500).json({ error: "Error interno al generar ejercicios." });
  }
}
