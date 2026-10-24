# Mein Deutsch v2.0 — modo híbrido (Android + IA + modo local)

## Qué hace
- Al entrar en la sesión de ejercicios, comprueba la conectividad y, si el endpoint está configurado, solicita automáticamente una sesión nueva a la IA.
- Si no hay conexión o falla el servidor/API, compone una sesión local con banco de actividades integrado y ejercicios IA que se hayan guardado anteriormente.
- Alterna automáticamente cinco tipos: test, completar espacios, relacionar parejas, ordenar palabras y traducir.
- Guarda el último banco IA por unidad y un historial local de enunciados recientes para reducir repeticiones.
- La clave de OpenAI permanece en Vercel, nunca en el APK.

## Parte A — preparar el backend de Vercel
1. Crea un repositorio Git nuevo (recomendado: privado) en GitHub.
2. Sube al repositorio TODO el contenido de la carpeta `backend` de este paquete, manteniendo `api/exercises.js` y `package.json` en la raíz del repositorio.
3. Entra en https://vercel.com/ y crea un proyecto importando ese repositorio.
4. En Vercel, abre **Project → Settings → Environment Variables** y añade:
   - `OPENAI_API_KEY` = tu clave secreta de la plataforma API de OpenAI.
   - `OPENAI_MODEL` = `gpt-6-luna` (opcional; es el valor predeterminado).
5. Vuelve a desplegar el proyecto después de añadir las variables.
6. La función quedará normalmente disponible en `https://TU-PROYECTO.vercel.app/api/exercises`.

No pongas la clave de OpenAI en Kotlin, GitHub ni en el teléfono. La facturación de la API es independiente de ChatGPT. Configura límites/alertas de gasto en la plataforma API antes de usarla habitualmente.

## Parte B — conectar Android
1. En Android Studio, guarda primero una copia completa del proyecto que funciona.
2. Reemplaza únicamente el contenido de `app/src/main/java/com/manuel/meindeutsch/MainActivity.kt` por el archivo `android/MainActivity.kt` de este paquete.
3. En `AndroidManifest.xml`, añade las líneas del archivo `android/AndroidManifest_permissions.txt` dentro de `<manifest>` y antes de `<application>`. No reemplaces el manifiesto completo.
4. En la parte superior de `MainActivity.kt`, busca `AI_ENDPOINT` y cambia `https://CAMBIA-ESTE-DOMINIO.vercel.app/api/exercises` por la URL real de tu función Vercel.
5. Guarda y ejecuta con el botón verde. Al entrar en Unidad 1 o Unidad 2, la app solicitará los ejercicios automáticamente.

## Si todavía no se ha desplegado el backend
La aplicación seguirá funcionando en modo local, incluso si el teléfono está desconectado. Verás un mensaje indicando por qué se ha usado el modo local. La IA en la nube no estará activa hasta configurar la URL del backend y añadir las variables en Vercel.

## Seguridad y costes
- No compartas la clave de la API ni la pongas dentro de la aplicación.
- El backend lleva una limitación de peticiones básica por IP en memoria, que ayuda para pruebas personales pero no sustituye una limitación compartida/robusta para una app pública.
- Si más adelante publicas la aplicación, añade autenticación y rate limiting compartido, y revisa las políticas de gasto del proyecto API.
- Las respuestas de IA pueden contener errores. Los ejercicios se validan estructuralmente en servidor y cliente, pero eso no sustituye una revisión pedagógica.
