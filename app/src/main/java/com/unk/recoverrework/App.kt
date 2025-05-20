// Recover - Hecho por UNK 2024
package com.unk.recoverrework

import android.app.Application
import com.kongzue.dialogx.DialogX

// Actividad la cual inicializa el CrashHandler, y DialogX
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * Genera un .txt con las últimas 500 líneas del logcat, en caso de falla/crasheo.
         */
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        // DialogX se inicializa para generar las ventanas de dialogo
        // "Conectando a [sensor]", "Subiendo datos", etc...

        /**
         * Inicialización del DialogX, encargado de mostrar las ventanas de "Conectando a SENSOR.." / "Envío Exitoso/Fallido", etc.
         */
        DialogX.init(this)
    }
}