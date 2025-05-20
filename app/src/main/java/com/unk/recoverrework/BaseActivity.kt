// Recover - Hecho por UNK 2024
package com.unk.recoverrework

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

/**
 * BaseActivity se encarga de preparar la Toolbar para el resto de actividades.
 */
open class BaseActivity : AppCompatActivity() {
    protected open fun setToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
}