// ============================================================
// MEIN DEUTSCH — API DE EJERCICIOS CON GEMINI
// Versión mejorada: 6 ejercicios por petición
//
// Tipos:
// 1. test       - Opción múltiple
// 2. fill       - Completar frases
// 3. match      - Relacionar palabras
// 4. order      - Ordenar frases
// 5. translate  - Traducir al alemán
// 6. conjugate  - Conjugar verbos
//
// Variables de entorno de Vercel:
// GEMINI_API_KEY
// GEMINI_MODEL (opcional)
// ============================================================

const ALLOWED_TYPES = new Set([
  "test",
  "fill",
  "match",
  "order",
  "translate",
  "conjugate"
]);

const TYPES = [
  "test",
  "fill",
  "match",
  "order",
  "translate",
  "conjugate"
];

const TOTAL_EXERCISES = 6;
const MAX_ATTEMPTS = 2;
const REQUEST_TIMEOUT_MS = 15000;

// ------------------------------------------------------------
// RESPUESTAS HTTP
// ------------------------------------------------------------

function send(res, status, data) {
  return res.status(status).json(data);
}

// ------------------------------------------------------------
// UTILIDADES
// ------------------------------------------------------------

function shuffle(items) {
  const result = [...items];

  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));

    [result[i], result[j]] = [result[j], result[i]];
  }

  return result;
}

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ------------------------------------------------------------
// VALIDACIÓN DE LOS EJERCICIOS
// ------------------------------------------------------------

function validateExercises(input) {
  if (!input || !Array.isArray(input.exercises)) {
    return null;
  }

  const exercises = input.exercises
    .filter(e =>
      e &&
      typeof e === "object" &&
      ALLOWED_TYPES.has(e.type)
    )
    .map(e => ({
      type: e.type,

      question:
        typeof e.question === "string"
          ? e.question.trim()
          : "",

      options:
        Array.isArray(e.options)
          ? e.options
              .filter(v => typeof v === "string")
              .map(v => v.trim())
              .slice(0, 12)
          : [],

      correctAnswer:
        typeof e.correctAnswer === "string"
          ? e.correctAnswer.trim()
          : "",

      acceptedAnswers:
        Array.isArray(e.acceptedAnswers)
          ? e.acceptedAnswers
              .filter(v => typeof v === "string")
              .map(v => v.trim())
              .slice(0, 8)
          : [],

      pairs:
        Array.isArray(e.pairs)
          ? e.pairs
              .filter(p =>
                p &&
                typeof p.german === "string" &&
                typeof p.spanish === "string" &&
                p.german.trim() &&
                p.spanish.trim()
              )
              .slice(0, 6)
              .map(p => ({
                german: p.german.trim(),
                spanish: p.spanish.trim()
              }))
          : [],

      explanation:
        typeof e.explanation === "string"
          ? e.explanation.trim()
          : ""
    }))
    .filter(e => e.question && e.explanation);

  // Debe haber exactamente seis ejercicios:
  // uno de cada tipo.

  if (exercises.length !== TOTAL_EXERCISES) {
    return null;
  }

  for (const type of TYPES) {
    const count = exercises.filter(
      e => e.type === type
    ).length;

    if (count !== 1) {
      return null;
    }
  }

  // Comprobar que los ejercicios tengan los datos necesarios.

  for (const e of exercises) {
    if (
      e.type === "test" &&
      (
        e.options.length < 3 ||
        !e.correctAnswer ||
        !e.options.includes(e.correctAnswer)
      )
    ) {
      return null;
    }

    if (
      ["fill", "translate", "conjugate"].includes(e.type) &&
      !e.correctAnswer &&
      e.acceptedAnswers.length === 0
    ) {
      return null;
    }

    if (
      e.type === "match" &&
      e.pairs.length < 3
    ) {
      return null;
    }

    if (
      e.type === "order" &&
      (
        e.options.length < 3 ||
        !e.correctAnswer
      )
    ) {
      return null;
    }
  }

  // Mezclar los fragmentos de las frases.

  for (const e of exercises) {
    if (e.type === "order") {
      e.options = shuffle(e.options);
    }

    if (e.type === "match") {
      // No mezclamos los pares aquí porque algunas
      // interfaces esperan que cada pareja vaya junta.
      // La aplicación puede mezclarlos al mostrarlos.
    }
  }

  // Ordenar los tipos para que la estructura sea estable.

  return TYPES.map(type =>
    exercises.find(e => e.type === type)
  );
}

// ------------------------------------------------------------
// TEMARIO
// ------------------------------------------------------------

function getUnitFocus(unit) {
  if (unit === 1) {
    return `
UNIDAD 1: Guten Tag!
Nivel alemán A1.1.

Temas:
- Saludos y despedidas.
- Hallo, Guten Morgen, Guten Tag, Guten Abend.
- Tschüs, Bis bald, Auf Wiedersehen.
- Presentarse y preguntar el nombre.
- heißen.
- Trato informal du y formal Sie.
- Preguntar cómo está una persona.
- sein, heißen, kommen, wohnen y sprechen.
- Preguntar y decir de dónde es una persona.
- Países, lugares e idiomas.
- Pronombres personales.
- Conjugación verbal según la persona.
- Números del 0 al 20.
- Números de teléfono.
- Alfabeto alemán y deletrear.
- Correo electrónico.
- Pedir que repitan o hablen más despacio.

En conjugación, utiliza especialmente:
ich bin, du bist, er/sie/es ist,
wir sind, ihr seid, sie/Sie sind;

ich heiße, du heißt, er/sie/es heißt,
wir heißen, ihr heißt, sie/Sie heißen;

ich komme, du kommst, er/sie/es kommt,
wir kommen, ihr kommt, sie/Sie kommen;

ich wohne, du wohnst, er/sie/es wohnt,
wir wohnen, ihr wohnt, sie/Sie wohnen;

ich spreche, du sprichst, er/sie/es spricht,
wir sprechen, ihr sprecht, sie/Sie sprechen.
`;
  }

  return `
UNIDAD 2: Freunde, Kollegen und ich.
Nivel alemán A1.1.

Temas:
- Amigos y compañeros.
- Presentar a otras personas.
- Aficiones y actividades.
- Expresar gustos con gern.
- Preguntar por actividades y preferencias.
- Días de la semana y quedar.
- Profesiones y actividades cotidianas.
- Pronombres personales.
- Conjugación verbal según la persona.

Utiliza verbos sencillos como:
kochen, arbeiten, lesen, sprechen,
fotografieren, gehen y spielen.

Ejemplos de conjugación:
ich koche, du kochst, er kocht,
wir kochen, ihr kocht, sie kochen;

ich arbeite, du arbeitest, er arbeitet,
wir arbeiten, ihr arbeitet, sie arbeiten;

ich lese, du liest, er liest,
wir lesen, ihr lest, sie lesen;

ich spreche, du sprichst, er spricht,
wir sprechen, ihr sprecht, sie sprechen.

Respeta siempre la conjugación correcta.
`;
}

// ------------------------------------------------------------
// PROMPT PARA GEMINI
// ------------------------------------------------------------

function createPrompt(unit, recentQuestions) {
  const unitFocus = getUnitFocus(unit);

  return `
Eres un profesor de alemán para estudiantes hispanohablantes
que están aprendiendo alemán de nivel A1.1.

Crea ejercicios originales para la aplicación Mein Deutsch.

UNIDAD: ${unit}

CONTENIDO:
${unitFocus}

PREGUNTAS RECIENTES QUE DEBES EVITAR REPETIR:
${JSON.stringify(recentQuestions)}

GENERA EXACTAMENTE 6 EJERCICIOS.

Debe haber exactamente uno de cada tipo:
1. test
2. fill
3. match
4. order
5. translate
6. conjugate

FORMATO DE RESPUESTA:

Devuelve exclusivamente JSON válido con esta estructura:

{
  "exercises": [
    {
      "type": "test",
      "question": "Pregunta en español",
      "options": ["Opción A", "Opción B", "Opción C"],
      "correctAnswer": "Respuesta correcta",
      "acceptedAnswers": [],
      "pairs": [],
      "explanation": "Explicación breve en español"
    }
  ]
}

Cada ejercicio debe contener siempre:
- type
- question
- options
- correctAnswer
- acceptedAnswers
- pairs
- explanation

Cuando un campo no se utilice, devuelve una cadena vacía
o un array vacío según corresponda.

REGLAS PARA CADA TIPO:

TEST:
- Pregunta en español.
- Al menos tres opciones.
- correctAnswer debe coincidir exactamente con una opción.

FILL:
- Presenta una frase alemana con un hueco representado por ___.
- correctAnswer debe contener únicamente la palabra o forma verbal
  que falta.
- acceptedAnswers puede contener otras respuestas válidas.

MATCH:
- Instrucciones en español.
- Incluye cuatro parejas en pairs.
- Cada pareja debe tener german y spanish.
- Las traducciones deben ser correctas.

ORDER:
- Incluye fragmentos desordenados en options.
- El estudiante debe construir una frase alemana correcta.
- correctAnswer debe contener la frase completa.
- No incluyas la frase completa ya ordenada en options.

TRANSLATE:
- Pide traducir una palabra o frase breve al alemán.
- correctAnswer debe ser la traducción alemana.
- acceptedAnswers puede incluir traducciones igualmente correctas.

CONJUGATE:
- Este tipo de ejercicio es MUY IMPORTANTE.
- Pide conjugar un verbo para una persona concreta.
- Indica claramente el pronombre personal.
- Presenta una frase con un hueco.
- correctAnswer debe ser únicamente la forma verbal.
- No preguntes por una persona sin especificarla.
- Varía las personas: ich, du, er/sie/es, wir, ihr, sie/Sie.
- Practica verbos adecuados a la unidad.
- Comprueba cuidadosamente la conjugación.

Ejemplos correctos:
ich lese
du liest
er liest
wir lesen
ihr lest
sie lesen

ich spreche
du sprichst
er spricht
wir sprechen
ihr sprecht
sie sprechen

ich arbeite
du arbeitest
er arbeitet
wir arbeiten
ihr arbeitet
sie arbeiten

CALIDAD:
- Utiliza alemán correcto.
- Mantén el nivel A1.1.
- Escribe explicaciones breves en español.
- No inventes reglas gramaticales.
- No repitas las preguntas recientes si es posible.
- Varía los verbos, las personas y las frases.
- No afirmes que los ejercicios son copias literales del libro.
- Evita vocabulario o gramática avanzada.
- Devuelve exactamente seis ejercicios.
- No escribas nada fuera del JSON.
`;
}

// ------------------------------------------------------------
// LLAMADA A GEMINI CON REINTENTOS
// ------------------------------------------------------------

async function callGemini(apiKey, model, prompt) {
  const endpoint =
    `https://generativelanguage.googleapis.com/v1beta/models/` +
    `${encodeURIComponent(model)}:generateContent`;

  let lastError = null;

  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    let controller;

    try {
      controller = new AbortController();

      const timeout = setTimeout(
        () => controller.abort(),
        REQUEST_TIMEOUT_MS
      );

      let upstream;

      try {
        upstream = await fetch(endpoint, {
          method: "POST",

          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey
          },

          body: JSON.stringify({
            contents: [
              {
                role: "user",
                parts: [
                  {
                    text: prompt
                  }
                ]
              }
            ],

            generationConfig: {
              temperature: 0.8,
              maxOutputTokens: 4500,
              responseMimeType: "application/json"
            }
          }),

          signal: controller.signal
        });
      } finally {
        clearTimeout(timeout);
      }

      const rawText = await upstream.text();

      let data;

      try {
        data = JSON.parse(rawText);
      } catch {
        throw new Error(
          `Gemini devolvió una respuesta no válida. HTTP ${upstream.status}.`
        );
      }

      if (!upstream.ok) {
        const message =
          data?.error?.message ||
          `Error HTTP ${upstream.status}.`;

        const error = new Error(message);

        error.status = upstream.status;

        throw error;
      }

      const content = (
        data?.candidates?.[0]?.content?.parts || []
      )
        .map(part =>
          typeof part.text === "string" ? part.text : ""
        )
        .join("")
        .trim();

      if (!content) {
        throw new Error(
          "Gemini no devolvió ejercicios."
        );
      }

      let decoded;

      try {
        decoded = JSON.parse(content);
      } catch {
        throw new Error(
          "Gemini devolvió un JSON de ejercicios no válido."
        );
      }

      return decoded;
    } catch (error) {
      lastError = error;

      const status = error?.status;

      const retryable =
        error?.name === "AbortError" ||
        status === 429 ||
        status === 500 ||
        status === 502 ||
        status === 503 ||
        status === 504 ||
        !status;

      console.error(
        `Intento Gemini ${attempt}/${MAX_ATTEMPTS}:`,
        error?.message || error
      );

      if (!retryable || attempt >= MAX_ATTEMPTS) {
        break;
      }

      // Un único reintento después de una pausa breve.
      await wait(800);
    }
  }

  throw lastError || new Error(
    "No se pudo contactar con Gemini."
  );
}

// ------------------------------------------------------------
// ENDPOINT PRINCIPAL
// ------------------------------------------------------------

module.exports = async function handler(req, res) {
  // CORS para permitir llamadas desde la aplicación Android.

  res.setHeader(
    "Access-Control-Allow-Origin",
    "*"
  );

  res.setHeader(
    "Access-Control-Allow-Methods",
    "GET, POST, OPTIONS"
  );

  res.setHeader(
    "Access-Control-Allow-Headers",
    "Content-Type"
  );

  // Peticiones previas de CORS.

  if (req.method === "OPTIONS") {
    return res.status(204).end();
  }

  // Comprobación básica del servidor.

  if (req.method === "GET") {
    return send(res, 200, {
      ok: true,
      service: "Mein Deutsch AI (Gemini)",
      model: process.env.GEMINI_MODEL || "gemini-3.8-flash",
      exercisesPerRequest: TOTAL_EXERCISES,
      message: "Servidor disponible. Utiliza POST para generar ejercicios."
    });
  }

  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST, OPTIONS");

    return send(res, 405, {
      error: "Método no permitido."
    });
  }

  // Leer la clave privada de Vercel.

  const apiKey = process.env.GEMINI_API_KEY;

  if (!apiKey) {
    return send(res, 503, {
      error: "Falta configurar GEMINI_API_KEY en Vercel."
    });
  }

  try {
    const body =
      typeof req.body === "string"
        ? JSON.parse(req.body)
        : (req.body || {});

    const unit = Number(body.unit);

    if (unit !== 1 && unit !== 2) {
      return send(res, 400, {
        error: "La unidad debe ser 1 o 2."
      });
    }

    const recentQuestions =
      Array.isArray(body.recentQuestions)
        ? body.recentQuestions
            .filter(q => typeof q === "string")
            .slice(-25)
        : [];

    const model =
      process.env.GEMINI_MODEL || "gemini-3.8-flash";

    const prompt = createPrompt(
      unit,
      recentQuestions
    );

    // Solicitar los ejercicios a Gemini.

    const generated = await callGemini(
      apiKey,
      model,
      prompt
    );

    // Validar que la respuesta tenga la estructura correcta.

    const exercises = validateExercises(generated);

    if (!exercises) {
      console.error(
        "Gemini devolvió ejercicios con estructura incorrecta."
      );

      return send(res, 502, {
        error: "Gemini no devolvió los seis ejercicios correctamente.",
        detail: "Puedes volver a intentarlo."
      });
    }

    // Respuesta para la aplicación Android.

    return send(res, 200, {
      ok: true,
      exercises,
      generatedBy: "gemini",
      unit,
      model,
      count: exercises.length
    });
  } catch (error) {
    const status = error?.status;

    console.error(
      "Mein Deutsch Gemini API error:",
      error?.message || error
    );

    // No devolver detalles internos ni la clave de API.

    if (status === 429) {
      return send(res, 503, {
        error: "Gemini ha alcanzado temporalmente su límite de uso.",
        retryable: true
      });
    }

    if (status === 503) {
      return send(res, 503, {
        error: "Gemini está temporalmente saturado.",
        retryable: true
      });
    }

    if (
      error?.name === "AbortError" ||
      /timeout|aborted/i.test(error?.message || "")
    ) {
      return send(res, 504, {
        error: "Gemini ha tardado demasiado en responder.",
        retryable: true
      });
    }

    if (
      status === 400 ||
      status === 401 ||
      status === 403 ||
      status === 404
    ) {
      return send(res, 502, {
        error: "Gemini ha rechazado la petición.",
        detail: error?.message || "Comprueba el modelo y la clave API."
      });
    }

    return send(res, 502, {
      error: "No se pudieron generar los ejercicios.",
      detail: "Comprueba los logs de Vercel y vuelve a intentarlo.",
      retryable: true
    });
  }
};
