package com.unk.recoverrework

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



/**
 * CrashHandler es la clase la cuál, cuando la aplicación falle y ocurra un "crash" (o similares),
 * guardará la salida del logcat en un .txt
 * @param ctx Context. Si bien no es requerido, mejor dejarlo ahí, o el Handler no funciona.
 */
class CrashHandler (private val ctx: Context) : Thread.UncaughtExceptionHandler {
    val TAG = "CrashHandler"
    private val handler: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()!!

    /**
     * uncaughtException agarra la excepción (error) y prepara el .txt
     * @param thread Thread de ejecución del programa.
     * @param throwable La clase base para cualquier tipo de error y excepciones.
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val logOutput = getLogcatOutput() // Variable local "logOutput" que almacena el resultado de la función
        saveLogToFile(logOutput) // Función que se encarga de guardar la salida de la variable, en el archivo .txt local

        handler.uncaughtException(thread, throwable)
    }

    /**
     * getLogcatOutput es la función encargada de tomar las últimas 500 líneas del logcat,
     * prepararlas en un .txt, y devuelve dicha variable como un string.
     * @return Las últimas 500 líneas del logcat en formato String.
     */
    fun getLogcatOutput(): String? {
        return try {
            // "Process" contiene el comando que irá por las 500 lineas del Logcat.
            val process = Runtime.getRuntime().exec("logcat -d -t 500")

            // BufferedReader contiene un InputStreamReader con el proceso de la variable "process"
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

            // Log y Line son variables String. Log es un StringBuilder.
            val log = StringBuilder()
            var line: String?

            // Se prepara la información del Logcat en la variable "log", insertando cada linea.
            while (bufferedReader.readLine().also { line = it } != null) {
                log.append(line).append("\n")
            }

            // Se devuelve la variable como un String.
            log.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * saveLogToFile es la función encargada de guardar el log, entregado previamente por getLogcatOutput,
     * en un archivo .txt, para después guardarlo en la carpeta Recover/Logs.
     * @param log String, que es generalmente las líneas del log.
     * @return Un archivo .txt en la carpeta Recover/Logs, encontrada en Documentos.
     */
    fun saveLogToFile(log: String?) {
        if (log == null) return // En caso de que "log" no contenga nada, retornamos para que no siga.

        try {
            // Directory es una variable que va por el directorio de "Documentos" en el dispositivo.
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            // Preparamos la fecha de ahora, en un formato "año-mes-dia_hora-minuto-segundo".
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val sysTimeDate = dateFormatter.format(Date())

            // Al directorio de "Documentos", creamos una carpeta que almacenará los Logs en caso de que no exista.
            val logsDirectory = File(directory, "Recover-Logs")
            Log.i(TAG, "Creating directory at $logsDirectory")
            if (!logsDirectory.exists() && logsDirectory.mkdirs()) Log.e(TAG, "Failed to create directory: $logsDirectory")

            // En la carpeta "Recover/Logs", generamos nuestro archivo .txt
            val logFile = File(logsDirectory, "log_recover_${sysTimeDate}.txt")
            if (!logFile.exists() && !logFile.createNewFile()) Log.e(TAG, "Failed to create file: $logFile")

            // Si ha salido bien, se crea el archivo en el directorio especificado.
            val fos = FileOutputStream(logFile)
            fos.write(log.toByteArray())
            fos.close()

            Log.i(TAG, "File was successfully written in $logsDirectory")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error writing file: $e")
        }
    }
}