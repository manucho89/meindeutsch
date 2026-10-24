# Mein Deutsch — servidor de ejercicios con IA

Este proyecto crea el endpoint `POST /api/exercises` que espera la app Android Mein Deutsch v2.1.

## Qué hace

- Recibe `unit` (1 o 2) y `recentQuestions`.
- Solicita a la API de OpenAI 10 ejercicios originales de alemán A1.1.
- Devuelve 2 ejercicios de cada tipo: `test`, `fill`, `match`, `order` y `translate`.
- No pretende reproducir literalmente las actividades del libro de texto.
- La clave de la API se lee de la variable de entorno `OPENAI_API_KEY`; nunca se debe poner en el código del repositorio.

## Despliegue general

1. Crea un repositorio NUEVO en GitHub para este servidor. No uses ni modifiques repositorios de otros proyectos.
2. Sube al repositorio `api/exercises.js`, `package.json` y este README.
3. Importa ese repositorio nuevo en Vercel y despliega.
4. En Vercel, abre el proyecto nuevo: **Settings → Environment Variables**.
5. Añade `OPENAI_API_KEY` con la clave secreta de tu cuenta de OpenAI API. No publiques ni compartas ese valor.
6. Opcionalmente, añade `OPENAI_MODEL` con el nombre de un modelo habilitado para tu cuenta. Por defecto se usa `gpt-4.1-mini`.
7. Vuelve a **Deployments** y haz un redeploy para que la variable se aplique.
8. Prueba en el navegador `https://TU-PROYECTO.vercel.app/api/exercises`. Una respuesta GET debe mostrar que el servicio está disponible. Esto comprueba el servidor, pero no por sí solo que la clave de IA funcione.

## Conectar Android

En `MainActivity.kt`, reemplaza el valor provisional de `AI_ENDPOINT` por:

`https://TU-PROYECTO.vercel.app/api/exercises`

Sustituye `TU-PROYECTO` por el dominio real de Vercel, sin añadir comillas extra y conservando las comillas Kotlin que ya rodean el texto. Después recompila e instala la app.

## Importante

- La API de OpenAI se factura por separado de una suscripción ChatGPT.
- No incluyas `OPENAI_API_KEY` en la app Android ni la subas a GitHub.
- Un endpoint público puede recibir peticiones de terceros. Para un despliegue público real conviene añadir autenticación y protección frente a abuso antes de compartirlo ampliamente.
