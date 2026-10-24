package com.manuel.meindeutsch

// Mein Deutsch v2.3: ejercicios basados en la unidad 1 del Kursbuch y Übungsbuch, conjugación explícita, modo offline y parejas corregidas.

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private val Azul = Color(0xFF2457A7)
private val Fondo = Color(0xFFF3F6FB)
private val Verde = Color(0xFF21865B)
private val Texto = Color(0xFF20314D)

private const val PREFS_NAME = "mein_deutsch_progress"

private fun guardarRespuesta(context: Context, unidad: Int, esCorrecta: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putInt("total_respuestas", prefs.getInt("total_respuestas", 0) + 1)
    if (esCorrecta) {
        editor.putInt("total_aciertos", prefs.getInt("total_aciertos", 0) + 1)
    }
    val claveTotalUnidad = "unidad_${unidad}_respuestas"
    val claveAciertosUnidad = "unidad_${unidad}_aciertos"
    editor.putInt(claveTotalUnidad, prefs.getInt(claveTotalUnidad, 0) + 1)
    if (esCorrecta) {
        editor.putInt(claveAciertosUnidad, prefs.getInt(claveAciertosUnidad, 0) + 1)
    }
    editor.apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MeinDeutschApp()
            }
        }
    }
}

private data class Lernkarte(
    val deutsch: String,
    val spanisch: String,
    val einheit: Int,
    val hinweis: String = ""
)

private data class Frage(
    val frage: String,
    val optionen: List<String>,
    val richtig: Int,
    val erklaerung: String
)

private val karten = listOf(
    Lernkarte("Hallo!", "¡Hola!", 1, "Saludo informal."),
    Lernkarte("Guten Morgen!", "¡Buenos días! (por la mañana)", 1),
    Lernkarte("Guten Tag!", "¡Buenos días! / ¡Buenas tardes!", 1, "Saludo habitual durante el día."),
    Lernkarte("Guten Abend!", "¡Buenas tardes! / ¡Buenas noches!", 1, "Se utiliza al llegar por la tarde-noche."),
    Lernkarte("Gute Nacht!", "¡Buenas noches! (para despedirse antes de dormir)", 1),
    Lernkarte("Tschüs!", "¡Adiós! / ¡Hasta luego!", 1, "Despedida informal."),
    Lernkarte("Auf Wiedersehen!", "¡Adiós!", 1, "Despedida más formal."),
    Lernkarte("Danke!", "¡Gracias!", 1),
    Lernkarte("Bitte!", "Por favor / De nada", 1, "El significado depende del contexto."),
    Lernkarte("Ich heiße …", "Me llamo …", 1),
    Lernkarte("Wie heißt du?", "¿Cómo te llamas?", 1, "Pregunta informal."),
    Lernkarte("Wie heißen Sie?", "¿Cómo se llama usted?", 1, "Pregunta formal; Sie se escribe con mayúscula."),
    Lernkarte("Woher kommst du?", "¿De dónde eres?", 1),
    Lernkarte("Ich komme aus Spanien.", "Soy de España.", 1),
    Lernkarte("Wo wohnst du?", "¿Dónde vives?", 1),
    Lernkarte("Ich wohne in Barcelona.", "Vivo en Barcelona.", 1),
    Lernkarte("die Sprache", "el idioma", 1, "Sustantivo femenino: die."),
    Lernkarte("die Zahl", "el número", 1, "Sustantivo femenino: die."),
    Lernkarte("der Freund", "el amigo", 2, "Femenino: die Freundin."),
    Lernkarte("die Freundin", "la amiga", 2),
    Lernkarte("der Kollege", "el compañero de trabajo", 2, "Femenino: die Kollegin."),
    Lernkarte("die Kollegin", "la compañera de trabajo", 2),
    Lernkarte("fotografieren", "hacer fotografías", 2),
    Lernkarte("joggen", "salir a correr", 2),
    Lernkarte("singen", "cantar", 2),
    Lernkarte("tanzen", "bailar", 2),
    Lernkarte("kochen", "cocinar", 2),
    Lernkarte("reisen", "viajar", 2),
    Lernkarte("schwimmen", "nadar", 2),
    Lernkarte("Musik hören", "escuchar música", 2),
    Lernkarte("Fußball spielen", "jugar al fútbol", 2),
    Lernkarte("gern", "con gusto; expresa que algo gusta", 2, "Ich schwimme gern. = Me gusta nadar."),
    Lernkarte("der Montag", "el lunes", 2),
    Lernkarte("der Dienstag", "el martes", 2),
    Lernkarte("das Wochenende", "el fin de semana", 2),
    Lernkarte("Wann?", "¿Cuándo?", 2),
    Lernkarte("Gehen wir ins Kino?", "¿Vamos al cine?", 2),
    Lernkarte("Ja, gern!", "¡Sí, encantado/a!", 2),
    Lernkarte("Das geht leider nicht.", "Por desgracia, no puedo / no me va bien.", 2),
    Lernkarte("der Beruf", "la profesión", 2),
    Lernkarte("der Arzt / die Ärztin", "el médico / la médica", 2)
)

private val fragenEinheit1 = listOf(
    Frage("¿Qué significa «Wie heißt du?»?", listOf("¿Dónde vives?", "¿Cómo te llamas?", "¿De dónde eres?"), 1, "«Wie heißt du?» es una pregunta informal para preguntar el nombre."),
    Frage("¿Qué saludo se utiliza normalmente por la mañana?", listOf("Gute Nacht!", "Guten Morgen!", "Auf Wiedersehen!"), 1, "«Guten Morgen!» significa buenos días por la mañana."),
    Frage("¿Qué significa «Ich komme aus Spanien»?", listOf("Vivo en España", "Hablo español", "Soy de España"), 2, "«kommen aus» expresa origen."),
    Frage("¿Cuál es la forma formal de «¿Cómo te llamas?»?", listOf("Wie heißen Sie?", "Wie heißt du?", "Woher kommst du?"), 0, "Se usa «Sie» para dirigirse formalmente a alguien."),
    Frage("¿Qué significa «die Sprache»?", listOf("el nombre", "el idioma", "el número"), 1, "«die Sprache» significa el idioma o la lengua.")
)

private val fragenEinheit2 = listOf(
    Frage("¿Qué significa «der Freund»?", listOf("el amigo", "el compañero de trabajo", "el profesor"), 0, "«der Freund» es el amigo; «die Freundin», la amiga."),
    Frage("¿Qué significa «Ich schwimme gern»?", listOf("No sé nadar", "Me gusta nadar", "Voy a nadar mañana"), 1, "«gern» expresa que te gusta hacer algo."),
    Frage("¿Cuál es la traducción de «Wann?»?", listOf("¿Quién?", "¿Dónde?", "¿Cuándo?"), 2, "«Wann?» pregunta por el momento o el día."),
    Frage("¿Qué significa «Gehen wir ins Kino?»?", listOf("¿Vamos al cine?", "¿Trabajamos en el cine?", "¿Dónde está el cine?"), 0, "En preguntas de sí/no, el verbo puede ir al principio: «Gehen wir …?»"),
    Frage("¿Qué significa «die Kollegin»?", listOf("la amiga", "la compañera de trabajo", "la profesión"), 1, "«die Kollegin» es la compañera de trabajo.")
)

@Composable
private fun MeinDeutschApp() {
    var bildschirm by remember { mutableStateOf("start") }
    var einheitAuswahl by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            Surface(color = Azul) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("🇩🇪 Mein Deutsch", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Lerne Deutsch jeden Tag!", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            when (bildschirm) {
                "start" -> Inicio(
                    alResumen = { einheitAuswahl = it; bildschirm = "resumen" },
                    alEjercicio = { einheitAuswahl = it; bildschirm = "sesionMixta" },
                    alTarjetas = { bildschirm = "tarjetas" },
                    alProgreso = { bildschirm = "progreso" }
                )
                "resumen" -> ResumenUnidad(einheit = einheitAuswahl, alVolver = { bildschirm = "start" })
                "tarjetas" -> TarjetasMemoria(alVolver = { bildschirm = "start" })
                "sesionMixta" -> SesionMixtaEjercicios(
                    einheit = einheitAuswahl,
                    alVolver = { bildschirm = "start" }
                )
                "tipoEjercicio" -> SeleccionarTipoEjercicio(
                    einheit = einheitAuswahl,
                    alTest = { bildschirm = "ejercicios" },
                    alCompletar = { bildschirm = "completar" },
                    alUnir = { bildschirm = "unir" },
                    alVolver = { bildschirm = "start" }
                )
                "ejercicios" -> Ejercicios(einheit = einheitAuswahl, alVolver = { bildschirm = "tipoEjercicio" })
                "completar" -> CompletarEspacios(einheit = einheitAuswahl, alVolver = { bildschirm = "tipoEjercicio" })
                "unir" -> UnirConFlechas(einheit = einheitAuswahl, alVolver = { bildschirm = "tipoEjercicio" })
                "progreso" -> Progreso(alVolver = { bildschirm = "start" })
            }
            Spacer(Modifier.height(24.dp))
            Text("Ein bisschen jeden Tag! · ¡Un poco cada día!", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun Inicio(
    alResumen: (Int) -> Unit,
    alEjercicio: (Int) -> Unit,
    alTarjetas: () -> Unit,
    alProgreso: () -> Unit
) {
    Text("Hallo! 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text("¿Qué quieres repasar hoy?", fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

    MenuCard("📘 Unidad 1 · Guten Tag!", "Resumen de lo estudiado: saludos, presentaciones, origen, idiomas y números.", Color(0xFFE4EDFF)) { alResumen(1) }
    MenuCard("📗 Unidad 2 · Freunde, Kollegen und ich", "Resumen: amigos, compañeros, aficiones, días de la semana y profesiones.", Color(0xFFE0F4E9)) { alResumen(2) }

    Text("Practicar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Texto, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
    MenuCard("🧠 Tarjetas de memoria", "Pulsa una tarjeta para ver la traducción. Elige Unidad 1, Unidad 2 o todas.", Color(0xFFFFEEDB), alTarjetas)
    MenuCard("✍️ Ejercicios · Unidad 1", "Test, conjugación de verbos, frases para completar, traducciones y vocabulario.", Color.White) { alEjercicio(1) }
    MenuCard("✍️ Ejercicios · Unidad 2", "Test, conjugación, frases para completar, traducciones y vocabulario.", Color.White) { alEjercicio(2) }
    MenuCard("📊 Mi progreso", "Consulta tus aciertos y los resultados guardados.", Color(0xFFE8E3FF), alProgreso)

    Spacer(Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6D8))) {
        Text(
            "Nota: los resúmenes y las tarjetas son material de repaso original basado en los temas y el glosario públicos de Netzwerk neu A1.1. Comprueba con tus apuntes qué apartados habéis trabajado ya en clase.",
            modifier = Modifier.padding(14.dp), fontSize = 13.sp, color = Texto
        )
    }
}

@Composable
private fun MenuCard(titulo: String, descripcion: String, color: Color, alPulsar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp).clickable { alPulsar() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(titulo, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(5.dp))
            Text(descripcion, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun ResumenUnidad(einheit: Int, alVolver: () -> Unit) {
    val titel = if (einheit == 1) "Unidad 1 · Guten Tag!" else "Unidad 2 · Freunde, Kollegen und ich"
    Text(titel, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text("Resumen para estudiar · explicación sencilla + ejemplos", fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))

    if (einheit == 1) {
        ResumenBloque(
            "1. Saludar, despedirse y preguntar cómo estás",
            "Hallo! = ¡Hola! (informal)\nGuten Morgen! = ¡Buenos días! (por la mañana)\nGuten Tag! = ¡Buenos días! / ¡Buenas tardes!\nGuten Abend! = ¡Buenas tardes/noches! (al llegar por la tarde-noche)\nGute Nacht! = ¡Buenas noches! (al despedirse antes de dormir)\nTschüs! / Ciao! = ¡Adiós! (informal)\nBis bald! = ¡Hasta pronto!\nAuf Wiedersehen! = ¡Adiós! (más formal)\n\nWie geht's? / Wie geht's dir? = ¿Qué tal? / ¿Cómo estás?\nWie geht es Ihnen? = ¿Cómo está usted? (formal)\nDanke, gut. / Danke, sehr gut. = Bien, gracias. / Muy bien, gracias.\nGanz gut. = Bastante bien.\nUnd dir? / Und Ihnen? = ¿Y tú? / ¿Y usted?",
            "Para hablar informalmente usa du/dir; para un trato formal, usa Sie/Ihnen. Gute Nacht se utiliza para despedirse antes de dormir."
        )
        ResumenBloque(
            "2. Presentarse: nombre y tratamiento informal/formal",
            "Wie heißt du? = ¿Cómo te llamas? (informal)\nWer bist du? = ¿Quién eres?\nIch heiße … = Me llamo …\nIch bin … = Soy …\nMein Name ist … = Mi nombre es …\n\nWie heißen Sie? = ¿Cómo se llama usted? (formal)\nWie ist Ihr Name? = ¿Cuál es su nombre? (formal)\nDas ist Frau … / Das ist Herr … = Esta es la señora … / Este es el señor …",
            "du se utiliza en situaciones informales. Sie se utiliza para dirigirse formalmente a una persona y siempre se escribe con mayúscula. En contextos formales se usa también Ihr/Ihre: Wie ist Ihr Name?"
        )
        ResumenBloque(
            "3. Preguntar y responder sobre origen y lugar de residencia",
            "Woher kommst du? = ¿De dónde eres?\nWoher kommen Sie? = ¿De dónde es usted?\nIch komme aus Spanien. = Soy de España.\nWo wohnst du? = ¿Dónde vives?\nWo wohnen Sie? = ¿Dónde vive usted?\nIch wohne in Barcelona. = Vivo en Barcelona.\nIch wohne in Zürich. = Vivo en Zúrich.\n\nWer? = ¿Quién?\nWie? = ¿Cómo?\nWo? = ¿Dónde?\nWoher? = ¿De dónde?\nWelche Sprache(n)? = ¿Qué idioma(s)?",
            "Wo pregunta por un lugar; Woher pregunta por la procedencia. En las respuestas del tema aparecen estructuras como Ich komme aus … y Ich wohne in …"
        )
        ResumenBloque(
            "4. Pronombres y conjugación de los verbos",
            "Pronombres usados en la unidad: ich = yo · du = tú · er = él · sie = ella · es = ello · Sie = usted/ustedes (formal).\n\nSEIN (ser/estar):\nich bin · du bist · er/sie/es ist · Sie sind\n\nHEISSEN (llamarse):\nich heiße · du heißt · er/sie/es heißt · Sie heißen\n\nKOMMEN (venir/ser de):\nich komme · du kommst · er/sie/es kommt · Sie kommen\n\nWOHNEN (vivir/residir):\nich wohne · du wohnst · er/sie/es wohnt · Sie wohnen\n\nSPRECHEN (hablar):\nich spreche · du sprichst · er/sie/es spricht · Sie sprechen",
            "Aprende cada forma junto con su persona. Por ejemplo: ich bin, du bist, er ist; ich komme, du kommst, sie kommt. En el trato formal: Sie sind, Sie heißen, Sie kommen, Sie wohnen y Sie sprechen."
        )
        ResumenBloque(
            "5. Países e idiomas",
            "Ich spreche Spanisch. = Hablo español.\nIch spreche Deutsch und Englisch. = Hablo alemán e inglés.\nIch lerne Deutsch. = Aprendo alemán.\n\nDeutschland → Deutsch (alemán)\nSpanien → Spanisch (español)\nÖsterreich → Deutsch (alemán)\nBrasilien → Portugiesisch (portugués)\nItalien → Italienisch (italiano)\nPolen → Polnisch (polaco)\nRussland → Russisch (ruso)\nJapan → Japanisch (japonés)\nAlgerien → Arabisch (árabe)\nDie Schweiz → Deutsch, Französisch, Italienisch und Rätoromanisch\nDie USA → Englisch (inglés)",
            "Los idiomas se escriben con mayúscula en alemán. Con aus se dice, por ejemplo, aus Spanien, aus Deutschland, aus der Schweiz, aus der Türkei y aus den USA."
        )
        ResumenBloque(
            "6. Números, teléfono y alfabeto",
            "Números del 0 al 20: null, eins, zwei, drei, vier, fünf, sechs, sieben, acht, neun, zehn, elf, zwölf, dreizehn, vierzehn, fünfzehn, sechzehn, siebzehn, achtzehn, neunzehn, zwanzig.\n\nWie ist deine Telefonnummer? = ¿Cuál es tu número de teléfono? (informal)\nWie ist Ihre Telefonnummer? = ¿Cuál es su número de teléfono? (formal)\nWie bitte? = ¿Cómo, perdón? / ¿Puede repetir?\nKannst du das buchstabieren? = ¿Puedes deletrearlo?",
            "Practica los números en voz alta y recuerda que los números de teléfono alemanes se suelen decir cifra a cifra o en grupos."
        )
        ResumenBloque(
            "7. Deletrear y decir una dirección de correo electrónico",
            "Wie ist deine E-Mail-Adresse? = ¿Cuál es tu dirección de correo?\nMan schreibt @; man sagt: at. = Se escribe @ y se dice at.\nPunkt = punto (.)\nMinus = guion (−)\nUnterstrich = guion bajo (_)\n\nEntschuldigung, noch einmal bitte. = Perdón, otra vez, por favor.\nDas verstehe ich nicht. = No lo entiendo.\nBitte ein bisschen langsamer. = Un poco más despacio, por favor.",
            "Al deletrear, puedes pedir que repitan o hablen más despacio. Practica también los nombres de las letras del alfabeto alemán, incluidas ä, ö, ü y ß."
        )
    } else {
        ResumenBloque("1. Amigos y compañeros", "der Freund = el amigo\ndie Freundin = la amiga\nder Kollege = el compañero de trabajo\ndie Kollegin = la compañera de trabajo\ndie Leute = la gente", "En muchos nombres de personas, la forma femenina añade -in: Kollege → Kollegin. Aprende el sustantivo con su artículo.")
        ResumenBloque("2. Hablar de aficiones", "fotografieren = hacer fotos\njoggen = salir a correr\nsingen = cantar\ntanzen = bailar\nkochen = cocinar\nreisen = viajar\nschwimmen = nadar\nMusik hören = escuchar música\nFußball spielen = jugar al fútbol\nins Kino gehen = ir al cine", "El infinitivo suele terminar en -en. Para hablar de ti, conjuga el verbo: Ich koche. / Ich schwimme.")
        ResumenBloque("3. Decir qué te gusta hacer: gern", "Ich schwimme gern. = Me gusta nadar.\nIch höre gern Musik. = Me gusta escuchar música.\nIch lese nicht so gern. = No me gusta mucho leer.\nEs geht so. = Más o menos / regular.\nJa, sehr gern! = Sí, mucho / encantado.", "«gern» suele colocarse después del verbo conjugado o al final del grupo verbal. Expresa que disfrutas haciendo algo.")
        ResumenBloque("4. Días de la semana y quedar", "Montag = lunes · Dienstag = martes\nMittwoch = miércoles · Donnerstag = jueves\nFreitag = viernes · Samstag = sábado\nSonntag = domingo · das Wochenende = el fin de semana\nWann? = ¿Cuándo?\nGehen wir ins Kino? = ¿Vamos al cine?\nJa, gern! = ¡Sí, encantado!\nDas geht leider nicht. = Por desgracia, no me va bien.", "En una pregunta de sí/no, el verbo suele ir al principio: «Gehen wir …?».")
        ResumenBloque("5. Profesiones y artículos", "der Beruf = la profesión\nder Arzt / die Ärztin = el médico / la médica\nder Kellner / die Kellnerin = el camarero / la camarera\nder Student / die Studentin = el estudiante / la estudiante", "Los sustantivos alemanes se escriben con mayúscula. Aprende cada sustantivo con su artículo: der (masculino), die (femenino), das (neutro). El género gramatical no siempre coincide con el español.")
        ResumenBloque("6. Preguntas útiles", "Was machst du gern? = ¿Qué te gusta hacer?\nWann hast du Zeit? = ¿Cuándo tienes tiempo?\nArbeitest du morgen? = ¿Trabajas mañana?\nWas bist du von Beruf? = ¿A qué te dedicas?", "Las preguntas que empiezan por una palabra interrogativa como «Was?» o «Wann?» normalmente colocan el verbo después de esa palabra. Las preguntas de sí/no suelen empezar por el verbo.")
    }

    Spacer(Modifier.height(12.dp))
    Button(onClick = alVolver, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Volver al menú") }
}

@Composable
private fun ResumenBloque(titulo: String, contenido: String, consejo: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Azul)
            Spacer(Modifier.height(9.dp))
            Text(contenido, fontSize = 16.sp, lineHeight = 23.sp, color = Texto)
            Spacer(Modifier.height(10.dp))
            Surface(color = Color(0xFFEAF2FF), shape = RoundedCornerShape(10.dp)) {
                Text("💡 $consejo", modifier = Modifier.padding(10.dp), fontSize = 14.sp, lineHeight = 20.sp, color = Texto)
            }
        }
    }
}

@Composable
private fun TarjetasMemoria(alVolver: () -> Unit) {
    var filtro by remember { mutableIntStateOf(1) }
    var indice by remember { mutableIntStateOf(0) }
    var reverso by remember { mutableStateOf(false) }
    val mazo = karten.filter { filtro == 0 || it.einheit == filtro }
    val posicion = indice.coerceIn(0, (mazo.size - 1).coerceAtLeast(0))
    val carta = mazo[posicion]

    Text("🧠 Tarjetas de memoria", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text("Pulsa la tarjeta para descubrir la traducción.", fontSize = 15.sp, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FiltroBoton("Unidad 1", filtro == 1, Modifier.weight(1f)) { filtro = 1; indice = 0; reverso = false }
        FiltroBoton("Unidad 2", filtro == 2, Modifier.weight(1f)) { filtro = 2; indice = 0; reverso = false }
        FiltroBoton("Todas", filtro == 0, Modifier.weight(1f)) { filtro = 0; indice = 0; reverso = false }
    }

    Spacer(Modifier.height(12.dp))
    Text("Tarjeta ${posicion + 1} de ${mazo.size}", fontSize = 14.sp, color = Color.DarkGray)
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { reverso = !reverso },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (reverso) Color(0xFFE0F4E9) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (reverso) "ESPAÑOL" else "ALEMÁN", fontSize = 13.sp, color = Azul, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text(if (reverso) carta.spanisch else carta.deutsch, fontSize = 25.sp, lineHeight = 32.sp, color = Texto, fontWeight = FontWeight.Bold)
            if (reverso && carta.hinweis.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(carta.hinweis, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 21.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(if (reverso) "Pulsa para volver al alemán" else "Pulsa para ver la traducción", fontSize = 13.sp, color = Color.Gray)
        }
    }

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { indice = if (indice == 0) mazo.lastIndex else indice - 1; reverso = false },
            modifier = Modifier.weight(1f)
        ) { Text("Anterior") }
        Button(
            onClick = { indice = (indice + 1) % mazo.size; reverso = false },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Azul)
        ) { Text("Siguiente") }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
}

@Composable
private fun FiltroBoton(texto: String, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (seleccionado) {
        Button(onClick = onClick, modifier = modifier, colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text(texto) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(texto) }
    }
}

private data class CompletarPregunta(
    val frase: String,
    val respuestas: List<String>,
    val explicacion: String
)

private data class ParejaVocabulario(
    val aleman: String,
    val espanol: String
)

private fun normalizarRespuesta(texto: String): String {
    return texto.lowercase()
        .trim()
        .replace("ß", "ss")
        .replace(Regex("[.!?,;:]+$"), "")
        .trim()
}

// El backend llama a Gemini. La clave de API se mantiene en Vercel, nunca en Android.
private const val AI_ENDPOINT = "https://mein-deutsch-ai-server.vercel.app/api/exercises"
private const val RECENT_AI_PREFIX = "mein_deutsch_recent_questions_unit_"
private val TIPOS_EJERCICIO = listOf("test", "fill", "match", "order", "translate", "conjugate")

private data class EjercicioIA(
    val type: String,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val pairs: List<ParejaVocabulario> = emptyList(),
    val explanation: String = ""
)

private fun hayConexionInternet(context: Context): Boolean {
    return try {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        false
    }
}

private fun parsearEjerciciosGemini(array: JSONArray): List<EjercicioIA> {
    val result = mutableListOf<EjercicioIA>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val type = item.optString("type", "").lowercase()
        if (type !in TIPOS_EJERCICIO) continue

        val optionsJson = item.optJSONArray("options") ?: JSONArray()
        val options = (0 until optionsJson.length())
            .mapNotNull { index -> optionsJson.optString(index).takeIf { value -> value.isNotBlank() } }
        val acceptedJson = item.optJSONArray("acceptedAnswers") ?: JSONArray()
        val accepted = (0 until acceptedJson.length())
            .mapNotNull { index -> acceptedJson.optString(index).takeIf { value -> value.isNotBlank() } }
        val pairsJson = item.optJSONArray("pairs") ?: JSONArray()
        val pairs = (0 until pairsJson.length()).mapNotNull { pIndex ->
            val pair = pairsJson.optJSONObject(pIndex) ?: return@mapNotNull null
            val german = pair.optString("german", "").trim()
            val spanish = pair.optString("spanish", "").trim()
            if (german.isBlank() || spanish.isBlank()) null else ParejaVocabulario(german, spanish)
        }
        val question = item.optString("question", "").trim()
        val correct = item.optString("correctAnswer", "").trim()
        val explanation = item.optString("explanation", "").trim()
        if (question.isBlank() || explanation.isBlank()) continue
        if (type == "test" && (options.size < 3 || correct.isBlank())) continue
        if (type in setOf("fill", "translate", "conjugate") && correct.isBlank() && accepted.isEmpty()) continue
        if (type == "match" && pairs.size < 3) continue
        if (type == "order" && (options.size < 3 || correct.isBlank())) continue
        result += EjercicioIA(type, question, options, correct, accepted, pairs, explanation)
    }
    return result
}

private suspend fun solicitarEjerciciosGemini(context: Context, unit: Int): List<EjercicioIA>? = withContext(Dispatchers.IO) {
    if (!hayConexionInternet(context)) return@withContext null
    var connection: HttpURLConnection? = null
    try {
        val recentArray = JSONArray()
        leerPreguntasRecientes(context, unit).takeLast(25).forEach { recentArray.put(it) }
        val requestBody = JSONObject()
            .put("unit", unit)
            .put("numberOfExercises", 12)
            .put("recentQuestions", recentArray)

        val conn = (URL(AI_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        connection = conn
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(requestBody.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.use {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
        } ?: return@withContext null
        if (code !in 200..299) return@withContext null

        val response = JSONObject(responseText)
        val array = response.optJSONArray("exercises") ?: return@withContext null
        val parsed = parsearEjerciciosGemini(array)
        if (parsed.size != 12) return@withContext null
        if (TIPOS_EJERCICIO.any { type -> parsed.count { it.type == type } != 2 }) return@withContext null

        // Distribuye los seis tipos de ejercicio alternadamente, dos veces cada uno.
        val pool = parsed.toMutableList()
        val ordered = mutableListOf<EjercicioIA>()
        repeat(2) {
            for (type in TIPOS_EJERCICIO) {
                val index = pool.indexOfFirst { it.type == type }
                if (index < 0) return@withContext null
                ordered += pool.removeAt(index)
            }
        }
        ordered
    } catch (_: Exception) {
        // Si Gemini no está disponible, la aplicación seguirá usando ejercicios locales sin mostrar avisos técnicos.
        null
    } finally {
        connection?.disconnect()
    }
}

private fun leerPreguntasRecientes(context: Context, unit: Int): List<String> {
    return try {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(RECENT_AI_PREFIX + unit, "[]") ?: "[]"
        val arr = JSONArray(raw)
        (0 until arr.length()).map { arr.optString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun guardarPreguntasRecientes(context: Context, unit: Int, exercises: List<EjercicioIA>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val combined = (leerPreguntasRecientes(context, unit) + exercises.map { it.question })
        .distinct().takeLast(40)
    val array = JSONArray()
    combined.forEach { array.put(it) }
    prefs.edit().putString(RECENT_AI_PREFIX + unit, array.toString()).apply()
}

private fun crearBancoLocal(unit: Int): List<EjercicioIA> {
    val tests = (if (unit == 1) fragenEinheit1 else fragenEinheit2).map { q ->
        EjercicioIA("test", q.frage, q.optionen, q.optionen[q.richtig], listOf(q.optionen[q.richtig]), explanation = q.erklaerung)
    }.toMutableList()
    if (unit == 1) {
        tests += listOf(
            EjercicioIA("test", "¿Qué significa «Auf Wiedersehen»?", listOf("¡Gracias!", "¡Adiós!", "¡Buenos días!"), "¡Adiós!", explanation = "Es una despedida formal."),
            EjercicioIA("test", "¿Qué significa «Wo wohnst du?»", listOf("¿Dónde vives?", "¿De dónde eres?", "¿Cómo te llamas?"), "¿Dónde vives?", explanation = "«Wo» pregunta por el lugar."),
            EjercicioIA("test", "¿Qué pronombre significa «yo»?", listOf("du", "ich", "wir"), "ich", explanation = "«Ich» significa yo."),
            EjercicioIA("test", "Completa mentalmente: ich ___ (sein).", listOf("bist", "bin", "sind"), "bin", explanation = "El verbo sein con ich es bin."),
            EjercicioIA("test", "¿Cuál es la forma formal de «¿Cómo se llama usted?»?", listOf("Wie heißt du?", "Wie heißen Sie?", "Wer bist du?"), "Wie heißen Sie?", explanation = "Para el trato formal se usa Sie: Wie heißen Sie?"),
            EjercicioIA("test", "¿Qué significa «Wie geht's dir?»?", listOf("¿Dónde vives?", "¿Cómo estás?", "¿De dónde eres?"), "¿Cómo estás?", explanation = "Es una forma informal de preguntar cómo está alguien."),
            EjercicioIA("test", "¿Qué significa «Bis bald!»?", listOf("Hasta pronto", "Buenos días", "Muchas gracias"), "Hasta pronto", explanation = "Bis bald! es una despedida."),
            EjercicioIA("test", "¿Qué significa «Welche Sprachen sprichst du?»?", listOf("¿Qué idiomas hablas?", "¿Dónde vives?", "¿Cómo te llamas?"), "¿Qué idiomas hablas?", explanation = "Welche Sprachen pregunta por los idiomas que hablas."),
            EjercicioIA("test", "¿Qué significa «buchstabieren»?", listOf("Vivir", "Deletrear", "Venir"), "Deletrear", explanation = "Buchstabieren es decir las letras de una palabra."),
            EjercicioIA("test", "¿Cómo se dice 14 en alemán?", listOf("vierzehn", "vierzig", "vier"), "vierzehn", explanation = "14 se dice vierzehn."),
            EjercicioIA("test", "¿Cómo se dice «punto» en una dirección de correo?", listOf("Punkt", "Unterstrich", "Minus"), "Punkt", explanation = "Punkt significa punto."),
            EjercicioIA("test", "¿Qué forma corresponde a «Sie» con el verbo wohnen?", listOf("wohnst", "wohne", "wohnen"), "wohnen", explanation = "En la forma formal: Sie wohnen."),
            EjercicioIA("test", "¿Qué frase significa «Hablo español»?", listOf("Ich wohne Spanisch.", "Ich spreche Spanisch.", "Ich heiße Spanisch."), "Ich spreche Spanisch.", explanation = "Sprechen se usa para decir qué idioma hablas."),
            EjercicioIA("test", "¿Cómo se dice «Bastante bien, gracias»?", listOf("Ganz gut, danke.", "Gute Nacht.", "Auf Wiedersehen."), "Ganz gut, danke.", explanation = "Ganz gut, danke = bastante bien, gracias.")
        )
    } else {
        tests += listOf(
            EjercicioIA("test", "¿Qué significa «schwimmen»?", listOf("cantar", "nadar", "viajar"), "nadar", explanation = "«Schwimmen» significa nadar."),
            EjercicioIA("test", "¿Qué expresa «gern»?", listOf("que algo gusta", "el día de la semana", "una despedida"), "que algo gusta", explanation = "«Ich lese gern» = Me gusta leer."),
            EjercicioIA("test", "¿Qué significa «das Wochenende»?", listOf("el trabajo", "el fin de semana", "el amigo"), "el fin de semana", explanation = "«Das Wochenende» significa fin de semana."),
            EjercicioIA("test", "¿Qué palabra significa «¿Cuándo?»?", listOf("Wann?", "Wo?", "Wer?"), "Wann?", explanation = "Wann? pregunta por el momento.")
        )
    }

    val fills = if (unit == 1) listOf(
        EjercicioIA("fill", "Ich ___ Manuel.", correctAnswer = "heiße", acceptedAnswers = listOf("heiße", "heisse"), explanation = "«Ich heiße …» significa «Me llamo …»"),
        EjercicioIA("fill", "Wie ___ du?", correctAnswer = "heißt", acceptedAnswers = listOf("heißt", "heisst"), explanation = "Con du, heißen se conjuga heißt."),
        EjercicioIA("fill", "Ich ___ aus Spanien.", correctAnswer = "komme", explanation = "«Ich komme aus …» expresa origen."),
        EjercicioIA("fill", "Wo ___ du?", correctAnswer = "wohnst", explanation = "«Wo wohnst du?» significa «¿Dónde vives?»"),
        EjercicioIA("fill", "Ich ___ in Barcelona.", correctAnswer = "wohne", explanation = "Con ich, wohnen se conjuga wohne."),
        EjercicioIA("fill", "Guten ___! (por la mañana)", correctAnswer = "Morgen", explanation = "El saludo de la mañana es Guten Morgen!"),
        EjercicioIA("fill", "Wie ___ Sie? (forma formal)", correctAnswer = "heißen", acceptedAnswers = listOf("heißen", "heissen"), explanation = "Con Sie, se utiliza heißen."),
        EjercicioIA("fill", "Welche Sprache ___ du?", correctAnswer = "sprichst", explanation = "Con du, sprechen se conjuga sprichst."),
        EjercicioIA("fill", "Wie geht's? — Ganz gut, danke. Und ___?", correctAnswer = "dir", explanation = "Dir es el pronombre que se usa con la pregunta informal Wie geht's dir?"),
        EjercicioIA("fill", "Ich spreche Deutsch und ___. (español)", correctAnswer = "Spanisch", explanation = "Los nombres de idiomas se escriben con mayúscula en alemán."),
        EjercicioIA("fill", "Er ___ aus Deutschland. (kommen)", correctAnswer = "kommt", explanation = "Con er/sie/es, kommen se conjuga kommt."),
        EjercicioIA("fill", "Sie ___ in Frankfurt. (wohnen, forma formal)", correctAnswer = "wohnen", explanation = "Con Sie formal se usa wohnen."),
        EjercicioIA("fill", "Ich komme ___ Spanien.", correctAnswer = "aus", explanation = "Kommen aus expresa procedencia: Ich komme aus Spanien."),
        EjercicioIA("fill", "Bitte ein bisschen ___. (más despacio)", correctAnswer = "langsamer", explanation = "Bitte ein bisschen langsamer = Un poco más despacio, por favor."),
        EjercicioIA("fill", "En una dirección de correo, @ se dice ___.", correctAnswer = "at", explanation = "El libro presenta la expresión: Man sagt at."),
        EjercicioIA("fill", "¿Cómo se dice 20 en alemán? ___", correctAnswer = "zwanzig", explanation = "20 = zwanzig.")
    ) else listOf(
        EjercicioIA("fill", "Ich schwimme ___.", correctAnswer = "gern", explanation = "Gern expresa que te gusta hacer algo."),
        EjercicioIA("fill", "Ich ___ gern Musik.", correctAnswer = "höre", acceptedAnswers = listOf("höre", "hoere"), explanation = "Con ich, hören se conjuga höre."),
        EjercicioIA("fill", "Ich ___ Fußball.", correctAnswer = "spiele", explanation = "«Fußball spielen» significa jugar al fútbol."),
        EjercicioIA("fill", "der Kollege – die ___.", correctAnswer = "Kollegin", acceptedAnswers = listOf("Kollegin", "kollegin"), explanation = "La forma femenina es die Kollegin."),
        EjercicioIA("fill", "Gehen wir ___ Kino?", correctAnswer = "ins", explanation = "La expresión es ins Kino gehen."),
        EjercicioIA("fill", "Ich ___ gern.", correctAnswer = "koche", explanation = "Con ich, kochen se conjuga koche."),
        EjercicioIA("fill", "Am ___ habe ich Zeit. (lunes)", correctAnswer = "Montag", explanation = "Los días de la semana se escriben con mayúscula."),
        EjercicioIA("fill", "Wir ___ ins Kino.", correctAnswer = "gehen", explanation = "Con wir, el verbo gehen es gehen.")
    )

    fun match(question: String, words: List<Pair<String, String>>) = EjercicioIA(
        "match", question, pairs = words.map { ParejaVocabulario(it.first, it.second) },
        explanation = "Lee cada expresión alemana y encuentra su traducción correcta."
    )
    val matches = if (unit == 1) listOf(
        match("Relaciona los saludos con su significado.", listOf("Hallo!" to "¡Hola!", "Danke!" to "¡Gracias!", "Bitte!" to "Por favor / De nada", "Gute Nacht!" to "¡Buenas noches! (antes de dormir)")),
        match("Relaciona las preguntas y expresiones.", listOf("Wie heißt du?" to "¿Cómo te llamas?", "Wo wohnst du?" to "¿Dónde vives?", "Woher kommst du?" to "¿De dónde eres?", "Auf Wiedersehen!" to "¡Adiós! (formal)")),
        match("Relaciona cada palabra con su traducción.", listOf("ich" to "yo", "du" to "tú", "die Sprache" to "el idioma", "die Zahl" to "el número")),
        match("Relaciona las frases.", listOf("Ich heiße …" to "Me llamo …", "Ich komme aus Spanien." to "Soy de España.", "Ich wohne in Barcelona." to "Vivo en Barcelona.", "Guten Morgen!" to "¡Buenos días! (mañana)")),
        match("Relaciona cada país con un idioma.", listOf("Deutschland" to "Deutsch", "Brasilien" to "Portugiesisch", "Italien" to "Italienisch", "Japan" to "Japanisch")),
        match("Relaciona las preguntas.", listOf("Wer?" to "¿Quién?", "Wie?" to "¿Cómo?", "Wo?" to "¿Dónde?", "Woher?" to "¿De dónde?")),
        match("Relaciona las expresiones de una conversación.", listOf("Wie geht's?" to "¿Qué tal?", "Danke, gut." to "Bien, gracias.", "Bis bald!" to "¡Hasta pronto!", "Auf Wiedersehen!" to "¡Adiós! (formal)")),
        match("Relaciona los términos de correo electrónico.", listOf("at" to "@", "Punkt" to "punto", "Minus" to "guion", "Unterstrich" to "guion bajo"))
    ) else listOf(
        match("Relaciona a las personas.", listOf("der Freund" to "el amigo", "die Freundin" to "la amiga", "der Kollege" to "el compañero de trabajo", "die Kollegin" to "la compañera de trabajo")),
        match("Relaciona las aficiones.", listOf("schwimmen" to "nadar", "kochen" to "cocinar", "singen" to "cantar", "reisen" to "viajar")),
        match("Relaciona las expresiones.", listOf("Musik hören" to "escuchar música", "Fußball spielen" to "jugar al fútbol", "gern" to "con gusto / expresa gusto", "das Wochenende" to "el fin de semana")),
        match("Relaciona las frases.", listOf("Wann?" to "¿Cuándo?", "Gehen wir ins Kino?" to "¿Vamos al cine?", "Ja, gern!" to "¡Sí, encantado/a!", "Das geht leider nicht." to "Por desgracia, no me va bien."))
    )

    val orders = if (unit == 1) listOf(
        EjercicioIA("order", "Ordena las palabras para decir tu nombre.", listOf("heiße", "Manuel.", "Ich"), "Ich heiße Manuel.", explanation = "El orden básico es sujeto + verbo + complemento."),
        EjercicioIA("order", "Ordena la pregunta: ¿Cómo te llamas?", listOf("du?", "heißt", "Wie"), "Wie heißt du?", explanation = "En esta pregunta, Wie va al principio."),
        EjercicioIA("order", "Ordena la frase para decir de dónde eres.", listOf("Spanien.", "aus", "komme", "Ich"), "Ich komme aus Spanien.", explanation = "La estructura es Ich + komme + aus + país."),
        EjercicioIA("order", "Ordena la frase: Vivo en Barcelona.", listOf("Barcelona.", "in", "wohne", "Ich"), "Ich wohne in Barcelona.", explanation = "Ich wohne in + ciudad."),
        EjercicioIA("order", "Ordena el saludo de la mañana.", listOf("Morgen!", "Guten"), "Guten Morgen!", explanation = "El saludo de la mañana es Guten Morgen!"),
        EjercicioIA("order", "Ordena la pregunta informal: ¿Cómo estás?", listOf("dir?", "geht's", "Wie"), "Wie geht's dir?", explanation = "Wie geht's dir? es una pregunta informal sobre cómo está alguien."),
        EjercicioIA("order", "Ordena la frase: Hablo alemán.", listOf("Deutsch.", "spreche", "Ich"), "Ich spreche Deutsch.", explanation = "Ich spreche … expresa los idiomas que hablas."),
        EjercicioIA("order", "Ordena la pregunta formal: ¿Cómo se llama usted?", listOf("Sie?", "heißen", "Wie"), "Wie heißen Sie?", explanation = "Sie es el tratamiento formal y se escribe con mayúscula.")
    ) else listOf(
        EjercicioIA("order", "Ordena la frase: Me gusta nadar.", listOf("gern.", "schwimme", "Ich"), "Ich schwimme gern.", explanation = "Gern expresa que te gusta hacer algo."),
        EjercicioIA("order", "Ordena la frase: Me gusta escuchar música.", listOf("Musik.", "gern", "höre", "Ich"), "Ich höre gern Musik.", explanation = "El verbo conjugado va en segunda posición."),
        EjercicioIA("order", "Ordena la pregunta: ¿Vamos al cine?", listOf("Kino?", "wir", "ins", "Gehen"), "Gehen wir ins Kino?", explanation = "Las preguntas de sí/no pueden comenzar con el verbo."),
        EjercicioIA("order", "Ordena la frase: Juego al fútbol.", listOf("Fußball.", "spiele", "Ich"), "Ich spiele Fußball.", explanation = "Con ich, spielen se conjuga spiele."),
        EjercicioIA("order", "Ordena la expresión de aceptación.", listOf("gern!", "Ja,"), "Ja, gern!", explanation = "Ja, gern! significa ¡Sí, encantado/a!")
    )

    val translations = if (unit == 1) listOf(
        EjercicioIA("translate", "Traduce al alemán: ¡Hola!", correctAnswer = "Hallo!", acceptedAnswers = listOf("Hallo"), explanation = "Hallo! es un saludo informal."),
        EjercicioIA("translate", "Traduce al alemán: ¿Cómo te llamas? (informal)", correctAnswer = "Wie heißt du?", acceptedAnswers = listOf("Wie heisst du?"), explanation = "Usa Wie heißt du? para preguntar el nombre de manera informal."),
        EjercicioIA("translate", "Traduce al alemán: Soy de España.", correctAnswer = "Ich komme aus Spanien.", explanation = "La expresión de origen es Ich komme aus …"),
        EjercicioIA("translate", "Traduce al alemán: ¿Dónde vives?", correctAnswer = "Wo wohnst du?", explanation = "Wo pregunta por el lugar."),
        EjercicioIA("translate", "Traduce al alemán: Gracias.", correctAnswer = "Danke!", acceptedAnswers = listOf("Danke"), explanation = "Danke significa gracias."),
        EjercicioIA("translate", "Traduce al alemán: ¿De dónde eres? (informal)", correctAnswer = "Woher kommst du?", explanation = "Woher pregunta por la procedencia."),
        EjercicioIA("translate", "Traduce al alemán: Hablo español.", correctAnswer = "Ich spreche Spanisch.", explanation = "Sprechen se utiliza para hablar de los idiomas."),
        EjercicioIA("translate", "Traduce al alemán: ¡Hasta pronto!", correctAnswer = "Bis bald!", explanation = "Bis bald! es una despedida informal."),
        EjercicioIA("translate", "Traduce al alemán: ¿Cómo está usted? (formal)", correctAnswer = "Wie geht es Ihnen?", explanation = "Ihnen pertenece al tratamiento formal Sie.")
    ) else listOf(
        EjercicioIA("translate", "Traduce al alemán: Me gusta nadar.", correctAnswer = "Ich schwimme gern.", explanation = "Gern se utiliza para expresar que te gusta hacer algo."),
        EjercicioIA("translate", "Traduce al alemán: el amigo.", correctAnswer = "der Freund", explanation = "Aprende el sustantivo junto con el artículo der."),
        EjercicioIA("translate", "Traduce al alemán: ¿Cuándo?", correctAnswer = "Wann?", acceptedAnswers = listOf("Wann"), explanation = "Wann? pregunta por el momento."),
        EjercicioIA("translate", "Traduce al alemán: ¿Vamos al cine?", correctAnswer = "Gehen wir ins Kino?", explanation = "La pregunta puede comenzar con el verbo Gehen."),
        EjercicioIA("translate", "Traduce al alemán: escuchar música.", correctAnswer = "Musik hören", acceptedAnswers = listOf("Musik hoeren"), explanation = "Musik hören significa escuchar música.")
    )

    val conjugations = if (unit == 1) listOf(
        EjercicioIA("conjugate", "Conjuga «sein» para «ich». Escribe solo la forma verbal: Ich ___. ", correctAnswer = "bin", explanation = "sein: ich bin. «Ich» lleva la forma bin."),
        EjercicioIA("conjugate", "Conjuga «sein» para «du». Escribe solo la forma verbal: Du ___. ", correctAnswer = "bist", explanation = "sein: du bist."),
        EjercicioIA("conjugate", "Conjuga «sein» para «er/sie/es». Escribe solo la forma verbal: Er ___. ", correctAnswer = "ist", explanation = "sein: er/sie/es ist."),
        EjercicioIA("conjugate", "Conjuga «sein» para «Sie» formal. Escribe solo la forma verbal: Sie ___. ", correctAnswer = "sind", explanation = "La forma formal es Sie sind."),
        EjercicioIA("conjugate", "Conjuga «heißen» para «ich». Escribe solo la forma verbal: Ich __ Manu.", correctAnswer = "heiße", acceptedAnswers = listOf("heisse"), explanation = "heißen: ich heiße."),
        EjercicioIA("conjugate", "Conjuga «heißen» para «du». Escribe solo la forma verbal: Wie ___ du?", correctAnswer = "heißt", acceptedAnswers = listOf("heisst"), explanation = "heißen: du heißt."),
        EjercicioIA("conjugate", "Conjuga «heißen» para «Sie» formal. Escribe solo la forma verbal: Wie ___ Sie?", correctAnswer = "heißen", acceptedAnswers = listOf("heissen"), explanation = "heißen: Sie heißen."),
        EjercicioIA("conjugate", "Conjuga «kommen» para «ich». Escribe solo la forma verbal: Ich ___ aus Spanien.", correctAnswer = "komme", explanation = "kommen: ich komme."),
        EjercicioIA("conjugate", "Conjuga «kommen» para «du». Escribe solo la forma verbal: Woher ___ du?", correctAnswer = "kommst", explanation = "kommen: du kommst."),
        EjercicioIA("conjugate", "Conjuga «kommen» para «er/sie/es». Escribe solo la forma verbal: Er ___ aus Deutschland.", correctAnswer = "kommt", explanation = "kommen: er/sie/es kommt."),
        EjercicioIA("conjugate", "Conjuga «kommen» para «Sie» formal. Escribe solo la forma verbal: Woher ___ Sie?", correctAnswer = "kommen", explanation = "kommen: Sie kommen."),
        EjercicioIA("conjugate", "Conjuga «wohnen» para «ich». Escribe solo la forma verbal: Ich ___ in Barcelona.", correctAnswer = "wohne", explanation = "wohnen: ich wohne."),
        EjercicioIA("conjugate", "Conjuga «wohnen» para «du». Escribe solo la forma verbal: Wo ___ du?", correctAnswer = "wohnst", explanation = "wohnen: du wohnst."),
        EjercicioIA("conjugate", "Conjuga «wohnen» para «er/sie/es». Escribe solo la forma verbal: Er ___ in Frankfurt.", correctAnswer = "wohnt", explanation = "wohnen: er/sie/es wohnt."),
        EjercicioIA("conjugate", "Conjuga «wohnen» para «Sie» formal. Escribe solo la forma verbal: Wo ___ Sie?", correctAnswer = "wohnen", explanation = "wohnen: Sie wohnen."),
        EjercicioIA("conjugate", "Conjuga «sprechen» para «ich». Escribe solo la forma verbal: Ich ___ Deutsch.", correctAnswer = "spreche", explanation = "En la unidad: Ich spreche Deutsch."),
        EjercicioIA("conjugate", "Conjuga «sprechen» para «du». Escribe solo la forma verbal: Welche Sprachen ___ du?", correctAnswer = "sprichst", explanation = "Con du, sprechen cambia la vocal: du sprichst."),
        EjercicioIA("conjugate", "Conjuga «sprechen» para «Sie» formal. Escribe solo la forma verbal: Welche Sprache ___ Sie?", correctAnswer = "sprechen", explanation = "Con Sie formal: Sie sprechen.")
    ) else listOf(
        EjercicioIA("conjugate", "Conjuga «kochen» para «ich». Completa: Ich ___.", correctAnswer = "koche", explanation = "kochen: ich koche."),
        EjercicioIA("conjugate", "Conjuga «kochen» para «du». Completa: Du ___.", correctAnswer = "kochst", explanation = "kochen: du kochst."),
        EjercicioIA("conjugate", "Conjuga «hören» para «ich». Completa: Ich ___ gern Musik.", correctAnswer = "höre", acceptedAnswers = listOf("hoere"), explanation = "hören: ich höre."),
        EjercicioIA("conjugate", "Conjuga «spielen» para «ich». Completa: Ich ___ Fußball.", correctAnswer = "spiele", explanation = "spielen: ich spiele."),
        EjercicioIA("conjugate", "Conjuga «reisen» para «du». Completa: Du ___.", correctAnswer = "reist", explanation = "reisen: du reist."),
        EjercicioIA("conjugate", "Conjuga «singen» para «er». Completa: Er ___.", correctAnswer = "singt", explanation = "singen: er singt."),
        EjercicioIA("conjugate", "Conjuga «tanzen» para «sie». Completa: Sie ___.", correctAnswer = "tanzt", explanation = "tanzen: sie tanzt."),
        EjercicioIA("conjugate", "Conjuga «schwimmen» para «ich». Completa: Ich ___.", correctAnswer = "schwimme", explanation = "schwimmen: ich schwimme.")
    )

    return tests + fills + matches + orders + translations + conjugations
}

private fun crearSesionLocal(context: Context, unit: Int): List<EjercicioIA> {
    val bank = crearBancoLocal(unit).distinctBy { it.type + "|" + it.question }
    val recent = leerPreguntasRecientes(context, unit).toSet()
    val types = listOf("test", "fill", "match", "order", "translate", "conjugate")
    val selectedByType = types.associateWith { type ->
        val all = bank.filter { it.type == type }
        val newOnes = all.filter { it.question !in recent }
        (if (newOnes.size >= 2) newOnes else all).shuffled().take(2)
    }
    val ordered = mutableListOf<EjercicioIA>()
    repeat(2) { round -> types.forEach { type -> selectedByType[type]?.getOrNull(round)?.let { ordered.add(it) } } }
    return ordered
}

@Composable
private fun SesionMixtaEjercicios(einheit: Int, alVolver: () -> Unit) {
    val context = LocalContext.current
    var nuevaSesion by remember(einheit) { mutableIntStateOf(0) }
    var ejercicios by remember(einheit, nuevaSesion) { mutableStateOf<List<EjercicioIA>>(emptyList()) }
    var cargando by remember(einheit, nuevaSesion) { mutableStateOf(true) }
    var paso by remember(einheit, nuevaSesion) { mutableIntStateOf(0) }
    var respuestaOpcion by remember(einheit, nuevaSesion) { mutableStateOf<String?>(null) }
    var respuestaTexto by remember(einheit, nuevaSesion) { mutableStateOf("") }
    var palabrasOrdenadas by remember(einheit, nuevaSesion) { mutableStateOf<List<String>>(emptyList()) }
    var parejaAlemanaSeleccionada by remember(einheit, nuevaSesion) { mutableIntStateOf(-1) }
    var parejasResueltas by remember(einheit, nuevaSesion) { mutableStateOf<Set<Int>>(emptySet()) }
    var mensajeRelacion by remember(einheit, nuevaSesion) { mutableStateOf("") }
    var evaluacion by remember(einheit, nuevaSesion) { mutableStateOf<Boolean?>(null) }
    var aciertos by remember(einheit, nuevaSesion) { mutableIntStateOf(0) }
    var terminado by remember(einheit, nuevaSesion) { mutableStateOf(false) }

    // Intenta generar una sesión nueva con Gemini; si no responde, usa el banco local sin mostrar avisos técnicos.
    LaunchedEffect(einheit, nuevaSesion) {
        cargando = true
        val ejerciciosGemini = solicitarEjerciciosGemini(context, einheit)
        ejercicios = ejerciciosGemini ?: crearSesionLocal(context, einheit)
        guardarPreguntasRecientes(context, einheit, ejercicios)
        cargando = false
    }

    val title = if (einheit == 1) "Unidad 1 · Guten Tag!" else "Unidad 2 · Freunde, Kollegen und ich"
    Text("✍️ Ejercicios · $title", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Texto)
    Spacer(Modifier.height(12.dp))

    if (cargando) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Preparando tu sesión…", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Azul)
                Spacer(Modifier.height(8.dp))
                Text("Estamos preparando tus ejercicios.", fontSize = 15.sp, color = Texto)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
        return
    }

    if (ejercicios.isEmpty()) {
        Text("No se pudo preparar una sesión. Vuelve al menú e inténtalo de nuevo.", color = Color(0xFFB3261E))
        OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
        return
    }

    if (terminado) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡Gut gemacht! 🎉", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(Modifier.height(10.dp))
                Text("Resultado: $aciertos de ${ejercicios.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Azul)
                Text("Has practicado vocabulario, frases y conjugación de verbos.", fontSize = 15.sp, color = Texto, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(14.dp))
                Button(onClick = { nuevaSesion++ }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Practicar otra sesión") }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
        return
    }

    val current = ejercicios[paso]
    val typeLabel = when (current.type) {
        "test" -> "🔘 Tipo test"
        "fill" -> "✍️ Rellenar espacios"
        "match" -> "↔️ Relacionar palabras"
        "order" -> "🔀 Ordenar palabras"
        "conjugate" -> "🔤 Conjugar verbos"
        else -> "💬 Traducir al alemán"
    }
    Text("Actividad ${paso + 1} de ${ejercicios.size}", fontSize = 14.sp, color = Color.DarkGray)
    Spacer(Modifier.height(6.dp))
    Surface(color = Color(0xFFE4EDFF), shape = RoundedCornerShape(10.dp)) {
        Text(typeLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 15.sp, color = Azul, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(current.question, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(12.dp))
            when (current.type) {
                "test" -> {
                    current.options.forEach { option ->
                        val isRight = normalizarRespuesta(option) == normalizarRespuesta(current.correctAnswer)
                        val bg = when {
                            evaluacion != null && isRight -> Color(0xFFDDF5E5)
                            respuestaOpcion == option && evaluacion == false -> Color(0xFFFFE2E2)
                            else -> Fondo
                        }
                        Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = evaluacion == null) {
                            respuestaOpcion = option
                            val right = isRight
                            evaluacion = right
                            if (right) aciertos++
                            guardarRespuesta(context, einheit, right)
                        }, color = bg, shape = RoundedCornerShape(12.dp)) {
                            Text(option, Modifier.padding(14.dp), fontSize = 16.sp, color = Texto)
                        }
                    }
                }
                "fill", "translate", "conjugate" -> {
                    OutlinedTextField(
                        value = respuestaTexto,
                        onValueChange = { if (evaluacion == null) respuestaTexto = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(when (current.type) {
                            "fill" -> "Palabra que falta"
                            "conjugate" -> "Forma verbal conjugada"
                            else -> "Tu traducción en alemán"
                        }) },
                        enabled = evaluacion == null,
                        singleLine = true
                    )
                    if (evaluacion == null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val validAnswers = (listOf(current.correctAnswer) + current.acceptedAnswers).filter { it.isNotBlank() }
                            val right = validAnswers.any { normalizarRespuesta(it) == normalizarRespuesta(respuestaTexto) }
                            evaluacion = right
                            if (right) aciertos++
                            guardarRespuesta(context, einheit, right)
                        }, enabled = respuestaTexto.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Comprobar respuesta") }
                    }
                }
                "order" -> {
                    val words = current.options
                    Text("Toca las palabras en el orden correcto:", fontSize = 14.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFEAF2FF), shape = RoundedCornerShape(10.dp)) {
                        Text(if (palabrasOrdenadas.isEmpty()) "Tu frase aparecerá aquí" else palabrasOrdenadas.joinToString(" "), Modifier.fillMaxWidth().padding(12.dp), fontSize = 18.sp, color = Texto)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Si hay palabras repetidas, se permite escoger cada ficha una vez mediante índices de lista.
                    val selectedIndices = remember(einheit, nuevaSesion, paso) { mutableListOf<Int>() }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        words.forEachIndexed { wordIndex, word ->
                            val picked = selectedIndices.contains(wordIndex)
                            OutlinedButton(onClick = {
                                if (evaluacion == null && !picked) {
                                    selectedIndices.add(wordIndex)
                                    palabrasOrdenadas = palabrasOrdenadas + word
                                }
                            }, enabled = evaluacion == null && !picked) { Text(word) }
                        }
                    }
                    if (evaluacion == null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedIndices.clear(); palabrasOrdenadas = emptyList() }, modifier = Modifier.weight(1f)) { Text("Borrar") }
                            Button(onClick = {
                                val right = normalizarRespuesta(palabrasOrdenadas.joinToString(" ")) == normalizarRespuesta(current.correctAnswer)
                                evaluacion = right
                                if (right) aciertos++
                                guardarRespuesta(context, einheit, right)
                            }, enabled = palabrasOrdenadas.size == words.size, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Comprobar") }
                        }
                    }
                }
                "match" -> {
                    Text("Toca una palabra alemana y después su traducción.", fontSize = 14.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Alemán", fontWeight = FontWeight.Bold, color = Azul)
                            current.pairs.forEachIndexed { index, pair ->
                                val done = index in parejasResueltas
                                Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = evaluacion == null && !done) { parejaAlemanaSeleccionada = index; mensajeRelacion = "Ahora elige la traducción." }, color = when { done -> Color(0xFFDDF5E5); parejaAlemanaSeleccionada == index -> Color(0xFFE4EDFF); else -> Fondo }, shape = RoundedCornerShape(9.dp)) {
                                    Text(pair.aleman, Modifier.padding(9.dp), fontSize = 14.sp, color = Texto)
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Español", fontWeight = FontWeight.Bold, color = Azul)
                            val spanishOrder = remember(einheit, nuevaSesion, paso) { current.pairs.indices.shuffled() }
                            spanishOrder.forEach { index ->
                                val done = index in parejasResueltas
                                Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(enabled = evaluacion == null && !done && parejaAlemanaSeleccionada >= 0) {
                                    if (parejaAlemanaSeleccionada == index) {
                                        val parejasResueltasActualizadas = parejasResueltas + index
                                        parejasResueltas = parejasResueltasActualizadas
                                        mensajeRelacion = "✅ Pareja correcta: ${current.pairs[index].aleman}"
                                        if (parejasResueltasActualizadas.size == current.pairs.size) {
                                            evaluacion = true
                                            aciertos++
                                            guardarRespuesta(context, einheit, true)
                                        }
                                    } else {
                                        mensajeRelacion = "No coincide todavía. Prueba otra traducción."
                                    }
                                    parejaAlemanaSeleccionada = -1
                                }, color = if (done) Color(0xFFDDF5E5) else Color.White, shape = RoundedCornerShape(9.dp)) {
                                    Text(current.pairs[index].espanol, Modifier.padding(9.dp), fontSize = 14.sp, color = Texto)
                                }
                            }
                        }
                    }
                    if (mensajeRelacion.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(mensajeRelacion, fontSize = 13.sp, color = Texto)
                    }
                }
            }
            if (evaluacion != null) {
                Spacer(Modifier.height(12.dp))
                val correctText = when (current.type) {
                    "test" -> current.correctAnswer
                    "fill", "translate", "order", "conjugate" -> current.correctAnswer.ifBlank { current.acceptedAnswers.firstOrNull().orEmpty() }
                    else -> "Todas las parejas están relacionadas correctamente."
                }
                Text(if (evaluacion == true) "✅ ¡Correcto!" else "❌ Respuesta correcta: $correctText", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (evaluacion == true) Verde else Color(0xFFB3261E))
                if (current.explanation.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(current.explanation, fontSize = 14.sp, lineHeight = 20.sp, color = Texto)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (paso < ejercicios.lastIndex) {
                        paso++
                        respuestaOpcion = null
                        respuestaTexto = ""
                        palabrasOrdenadas = emptyList()
                        parejaAlemanaSeleccionada = -1
                        parejasResueltas = emptySet()
                        mensajeRelacion = ""
                        evaluacion = null
                    } else terminado = true
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) {
                    Text(if (paso < ejercicios.lastIndex) "Siguiente ejercicio" else "Finalizar sesión")
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Salir de los ejercicios") }
}

@Composable
private fun SeleccionarTipoEjercicio(
    einheit: Int,
    alTest: () -> Unit,
    alCompletar: () -> Unit,
    alUnir: () -> Unit,
    alVolver: () -> Unit
) {
    val titulo = if (einheit == 1) "Unidad 1 · Guten Tag!" else "Unidad 2 · Freunde, Kollegen und ich"
    Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text(
        "Elige cómo quieres practicar. No todos los ejercicios son tipo test.",
        fontSize = 16.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
    )
    MenuCard("🔘 Tipo test", "Elige una respuesta entre varias opciones.", Color.White, alTest)
    MenuCard("✍️ Rellenar espacios", "Escribe la palabra que falta en una frase alemana.", Color(0xFFE4EDFF), alCompletar)
    MenuCard("↔️ Unir con flechas", "Selecciona una expresión alemana y después su traducción.", Color(0xFFE0F4E9), alUnir)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
}

@Composable
private fun CompletarEspacios(einheit: Int, alVolver: () -> Unit) {
    val context = LocalContext.current
    val preguntas = if (einheit == 1) {
        listOf(
            CompletarPregunta("Ich ___ Manuel.", listOf("heiße", "heisse"), "«Ich heiße …» significa «Me llamo …»."),
            CompletarPregunta("Wie ___ du?", listOf("heißt", "heisst"), "Con «du», el verbo heißen se conjuga «heißt»."),
            CompletarPregunta("Ich ___ aus Spanien.", listOf("komme"), "«Ich komme aus …» se utiliza para decir de dónde eres."),
            CompletarPregunta("Wo ___ du?", listOf("wohnst"), "«Wo wohnst du?» significa «¿Dónde vives?»."),
            CompletarPregunta("Ich ___ in Barcelona.", listOf("wohne"), "Con «ich», el verbo wohnen es «wohne».")
        )
    } else {
        listOf(
            CompletarPregunta("Ich schwimme ___.", listOf("gern"), "«Gern» expresa que te gusta hacer algo."),
            CompletarPregunta("Ich ___ gern Musik.", listOf("höre", "hoere"), "«Ich höre gern Musik» significa «Me gusta escuchar música»."),
            CompletarPregunta("Ich ___ Fußball.", listOf("spiele"), "«Fußball spielen» significa jugar al fútbol; con «ich»: «ich spiele»."),
            CompletarPregunta("der Kollege – die ___.", listOf("kollegin"), "La forma femenina es «die Kollegin»."),
            CompletarPregunta("Gehen wir ___ Kino?", listOf("ins"), "La expresión habitual es «ins Kino gehen», ir al cine.")
        )
    }

    var indice by remember(einheit) { mutableIntStateOf(0) }
    var respuestaUsuario by remember(einheit) { mutableStateOf("") }
    var evaluacion by remember(einheit) { mutableStateOf<Boolean?>(null) }
    var aciertos by remember(einheit) { mutableIntStateOf(0) }
    var terminado by remember(einheit) { mutableStateOf(false) }

    Text("✍️ Rellenar espacios · Unidad $einheit", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text("Escribe la palabra o expresión que falta.", fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))

    if (terminado) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡Gut gemacht! 🎉", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Resultado: $aciertos de ${preguntas.size}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Azul)
                Text("Puedes repetir el ejercicio para practicar de nuevo.", fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { indice = 0; respuestaUsuario = ""; evaluacion = null; aciertos = 0; terminado = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Repetir ejercicios") }
    } else {
        Text("Pregunta ${indice + 1} de ${preguntas.size}", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(preguntas[indice].frase, fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = respuestaUsuario,
                    onValueChange = { if (evaluacion == null) respuestaUsuario = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra que falta") },
                    singleLine = true,
                    enabled = evaluacion == null
                )
                if (evaluacion != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (evaluacion == true) "✅ ¡Correcto!" else "❌ Respuesta correcta: ${preguntas[indice].respuestas.first()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (evaluacion == true) Verde else Color(0xFFB3261E)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(preguntas[indice].explicacion, fontSize = 15.sp, lineHeight = 21.sp, color = Texto)
                }
                Spacer(Modifier.height(14.dp))
                if (evaluacion == null) {
                    Button(
                        onClick = {
                            if (respuestaUsuario.isNotBlank()) {
                                val correcta = preguntas[indice].respuestas.any {
                                    normalizarRespuesta(it) == normalizarRespuesta(respuestaUsuario)
                                }
                                evaluacion = correcta
                                if (correcta) aciertos++
                                guardarRespuesta(context, einheit, correcta)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = respuestaUsuario.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Azul)
                    ) { Text("Comprobar respuesta") }
                } else {
                    Button(
                        onClick = {
                            if (indice < preguntas.lastIndex) {
                                indice++
                                respuestaUsuario = ""
                                evaluacion = null
                            } else {
                                terminado = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Azul)
                    ) { Text(if (indice < preguntas.lastIndex) "Siguiente frase" else "Ver resultado") }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver a los tipos de ejercicio") }
}

@Composable
private fun UnirConFlechas(einheit: Int, alVolver: () -> Unit) {
    val context = LocalContext.current
    val parejas = if (einheit == 1) {
        listOf(
            ParejaVocabulario("Hallo!", "¡Hola!"),
            ParejaVocabulario("Guten Morgen!", "¡Buenos días!"),
            ParejaVocabulario("Danke!", "¡Gracias!"),
            ParejaVocabulario("Wie heißt du?", "¿Cómo te llamas?"),
            ParejaVocabulario("Ich komme aus Spanien.", "Soy de España.")
        )
    } else {
        listOf(
            ParejaVocabulario("der Freund", "el amigo"),
            ParejaVocabulario("die Kollegin", "la compañera de trabajo"),
            ParejaVocabulario("schwimmen", "nadar"),
            ParejaVocabulario("Musik hören", "escuchar música"),
            ParejaVocabulario("Wann?", "¿Cuándo?")
        )
    }
    val ordenOpciones = if (einheit == 1) listOf(2, 4, 0, 3, 1) else listOf(3, 0, 4, 1, 2)
    var seleccionAleman by remember(einheit) { mutableStateOf<Int?>(null) }
    var emparejadas by remember(einheit) { mutableStateOf(setOf<Int>()) }
    var aciertos by remember(einheit) { mutableIntStateOf(0) }
    var mensaje by remember(einheit) { mutableStateOf("Toca una frase alemana y luego su traducción en español.") }

    Text("↔️ Unir con flechas · Unidad $einheit", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text("Primero elige una tarjeta de la columna izquierda y después su pareja de la derecha.", fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
    Text("Alemán", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Azul)
    parejas.forEachIndexed { idx, pareja ->
        val hecha = emparejadas.contains(idx)
        val seleccionada = seleccionAleman == idx
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !hecha) {
                seleccionAleman = idx
                mensaje = "Ahora busca la traducción de «${pareja.aleman}» en la columna derecha."
            },
            color = when { hecha -> Color(0xFFDDF5E5); seleccionada -> Color(0xFFE4EDFF); else -> Color.White },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(pareja.aleman, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = if (seleccionada || hecha) FontWeight.Bold else FontWeight.Normal, color = Texto)
                if (hecha) Text("↔ ✓", color = Verde, fontWeight = FontWeight.Bold)
                else if (seleccionada) Text("→", color = Azul, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("Español", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Azul)
    ordenOpciones.forEach { indicePareja ->
        val usada = emparejadas.contains(indicePareja)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !usada && seleccionAleman != null) {
                val elegido = seleccionAleman
                if (elegido != null) {
                    val correcta = elegido == indicePareja
                    guardarRespuesta(context, einheit, correcta)
                    if (correcta) {
                        emparejadas = emparejadas + elegido
                        aciertos++
                        mensaje = "✅ ¡Correcto! ${parejas[elegido].aleman} ↔ ${parejas[elegido].espanol}"
                        seleccionAleman = null
                    } else {
                        mensaje = "❌ No coinciden. Mantén la frase seleccionada y prueba otra traducción."
                    }
                }
            },
            color = if (usada) Color(0xFFDDF5E5) else if (seleccionAleman == null) Fondo else Color.White,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(parejas[indicePareja].espanol, modifier = Modifier.weight(1f), fontSize = 16.sp, color = Texto)
                if (usada) Text("✓", color = Verde, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("Parejas correctas: ${emparejadas.size} de ${parejas.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(5.dp))
            Text(mensaje, fontSize = 14.sp, lineHeight = 20.sp, color = Texto)
        }
    }
    if (emparejadas.size == parejas.size) {
        Spacer(Modifier.height(10.dp))
        Text("¡Todas las parejas están unidas! Gut gemacht! 🎉", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Verde)
        Button(onClick = { seleccionAleman = null; emparejadas = emptySet(); aciertos = 0; mensaje = "Toca una frase alemana y luego su traducción en español." }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Volver a practicar") }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver a los tipos de ejercicio") }
}

@Composable
private fun Ejercicios(einheit: Int, alVolver: () -> Unit) {
    val context = LocalContext.current
    val preguntas = if (einheit == 1) fragenEinheit1 else fragenEinheit2
    var indice by remember(einheit) { mutableIntStateOf(0) }
    var respuesta by remember(einheit) { mutableStateOf<Int?>(null) }
    var aciertos by remember(einheit) { mutableIntStateOf(0) }
    var terminado by remember(einheit) { mutableStateOf(false) }

    Text("✍️ Ejercicios · Unidad $einheit", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
    Spacer(Modifier.height(6.dp))

    if (terminado) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡Gut gemacht! 🎉", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Resultado: $aciertos de ${preguntas.size}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Azul)
                Spacer(Modifier.height(8.dp))
                Text("Vuelve al resumen si quieres repasar antes de intentarlo otra vez.", fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { indice = 0; respuesta = null; aciertos = 0; terminado = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Repetir ejercicios") }
    } else {
        Text("Pregunta ${indice + 1} de ${preguntas.size}", fontSize = 14.sp, color = Color.DarkGray)
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text(preguntas[indice].frage, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(Modifier.height(14.dp))
                preguntas[indice].optionen.forEachIndexed { opcion, texto ->
                    val elegida = respuesta == opcion
                    val correcta = opcion == preguntas[indice].richtig
                    val color = when {
                        respuesta != null && correcta -> Color(0xFFDDF5E5)
                        elegida && !correcta -> Color(0xFFFFE2E2)
                        else -> Fondo
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = respuesta == null) {
                            val correcta = opcion == preguntas[indice].richtig
                            respuesta = opcion
                            if (correcta) aciertos++
                            guardarRespuesta(context, einheit, correcta)
                        },
                        color = color, shape = RoundedCornerShape(12.dp)
                    ) { Text(texto, modifier = Modifier.padding(14.dp), fontSize = 16.sp, color = Texto) }
                }
                if (respuesta != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(if (respuesta == preguntas[indice].richtig) "✅ ¡Correcto!" else "❌ La respuesta correcta es: ${preguntas[indice].optionen[preguntas[indice].richtig]}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(preguntas[indice].erklaerung, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { if (indice < preguntas.lastIndex) { indice++; respuesta = null } else { terminado = true } },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Azul)
                    ) { Text(if (indice < preguntas.lastIndex) "Siguiente pregunta" else "Ver resultado") }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") }
}


@Composable
private fun Progreso(alVolver: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val total = prefs.getInt("total_respuestas", 0)
    val aciertos = prefs.getInt("total_aciertos", 0)
    val totalU1 = prefs.getInt("unidad_1_respuestas", 0)
    val aciertosU1 = prefs.getInt("unidad_1_aciertos", 0)
    val totalU2 = prefs.getInt("unidad_2_respuestas", 0)
    val aciertosU2 = prefs.getInt("unidad_2_aciertos", 0)

    Text("📊 Mi progreso", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Texto)
    Text(
        "Tus resultados se guardan en este teléfono, incluso cuando cierras la aplicación.",
        fontSize = 15.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Resumen general", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Azul)
            Spacer(Modifier.height(10.dp))
            Text("Preguntas respondidas: $total", fontSize = 17.sp, color = Texto)
            Text("Respuestas correctas: $aciertos", fontSize = 17.sp, color = Texto)
            Text(
                "Porcentaje de aciertos: ${if (total > 0) aciertos * 100 / total else 0}%",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Verde
            )
            if (total == 0) {
                Spacer(Modifier.height(8.dp))
                Text("Todavía no hay resultados. Completa algunos ejercicios y vuelve aquí.", fontSize = 14.sp, color = Color.DarkGray)
            }
        }
    }

    TarjetaProgresoUnidad("Unidad 1 · Guten Tag!", totalU1, aciertosU1)
    TarjetaProgresoUnidad("Unidad 2 · Freunde, Kollegen und ich", totalU2, aciertosU2)

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) {
        Text("Volver al menú")
    }
}

@Composable
private fun TarjetaProgresoUnidad(titulo: String, total: Int, aciertos: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Azul)
            Spacer(Modifier.height(8.dp))
            Text("Preguntas respondidas: $total", fontSize = 16.sp, color = Texto)
            Text("Aciertos: $aciertos", fontSize = 16.sp, color = Texto)
            Text(
                "Resultado: ${if (total > 0) aciertos * 100 / total else 0}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Verde
            )
        }
    }
}
