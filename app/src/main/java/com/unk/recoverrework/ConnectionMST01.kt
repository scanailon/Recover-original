package com.unk.recoverrework

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import com.unk.recoverrework.databinding.DeviceConnectedBinding
import com.ylwl.industry.enums.MSensorConnectionState
import com.ylwl.industry.manager.IndustrySensorBleManager
import com.ylwl.industry.utils.LogUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

class ConnectionMST01 : BaseActivity() {
    private var binding: DeviceConnectedBinding? = null // Binding para acceder a las vistas del layout
    private var mSensorBleManager: IndustrySensorBleManager? = null // Gestor BLE para manejar la conexión del dispositivo
    private var mMac: String? = "" // Dirección MAC del dispositivo conectado
    private var startTS: Long = 0 // Timestamp de inicio para la selección de fechas
    private var endTS: Long = 0 // Timestamp de fin para la selección de fechas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeviceConnectedBinding.inflate(layoutInflater) // Inflar el layout usando ViewBinding
        setContentView(binding!!.root) // Establecer el contenido de la vista
        initBleManager() // Inicializa el gestor BLE
        initData() // Inicializa los datos (MAC del dispositivo)
        initListener() // Configura los listeners

        // Configurar la toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        val goBack = findViewById<ImageView>(R.id.goBackBtn)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.textViewToolbar)
        val titleText = titleTextFromSomeSource

        // Establecer el texto de la toolbar
        toolbarTitle.text = titleText
        // Listener para el botón de volver atrás
        goBack.setOnClickListener {
            val i = Intent(
                this@ConnectionMST01,
                ScanActivity::class.java
            ) // Inicia la actividad de escaneo
            startActivity(i)
        }
    }

    // Obtiene el texto del título para la toolbar (muestra la MAC del dispositivo)
    private val titleTextFromSomeSource: String
        get() = "ID: $mMac"

    override fun onDestroy() {
        super.onDestroy()
        disConnected() // Desconecta el dispositivo BLE al destruir la actividad
    }

    // Inicializa el gestor BLE y configura el listener de estado de conexión
    private fun initBleManager() {
        mSensorBleManager = IndustrySensorBleManager.getInstance()
        mSensorBleManager?.setOnConnStateListener { _, mSensorConnectionState ->
            when (mSensorConnectionState) {
                MSensorConnectionState.Connecting -> Log.d("TAG", "Connecting") // Estado: Conectando
                MSensorConnectionState.Connected -> Log.d("TAG", "Connected") // Estado: Conectado
                MSensorConnectionState.ConnectComplete -> Log.d("TAG", "ConnectComplete") // Conexión completa
                MSensorConnectionState.Disconnect -> Log.d("TAG", "Disconnect") // Estado: Desconectado
                else -> {}
            }
        }
    }

    // Inicializa los datos obtenidos desde el intent (MAC del dispositivo)
    private fun initData() {
        mMac = intent.getStringExtra("mac")
    }

    // Configura el listener para abrir el selector de rango de fechas
    private fun initListener() {
        binding!!.selectDates.setOnClickListener { openDateRangePicker() }
    }

    /**
     * openDateRangePicker se encarga de abrir la ventana que permitirá seleccionar las fechas al usuario.
     */
    private fun openDateRangePicker() {
        val materialDatePicker = MaterialDatePicker.Builder.dateRangePicker().setSelection(
            Pair(
                MaterialDatePicker.thisMonthInUtcMilliseconds(), // Fecha de inicio
                MaterialDatePicker.todayInUtcMilliseconds() // Fecha actual
            )
        ).build()

        // Cuando el usuario selecciona una fecha, inicia la conversión y selección de tiempo
        materialDatePicker.addOnPositiveButtonClickListener { selection ->
            val startDateTS = Date(selection.first!!)
            val endDateTS = Date(selection.second!!)

            val startCal = Calendar.getInstance()
            startCal.time = startDateTS

            val endCal = Calendar.getInstance()
            endCal.time = endDateTS

            LogUtil.d("START: " + startCal.time + " | END: " + endCal.time)
            openStartTimePicker(startCal, endCal)
        }

        materialDatePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    /**
     * openStartTimePicker se encarga de abrir una ventana de horas para seleccionar la hora de inicio.
     * @param start Calendar que tiene la fecha de inicio
     * @param end Calendar que tiene la fecha de fin
     */
    private fun openStartTimePicker(start: Calendar, end: Calendar) {
        val calendar = Calendar.getInstance() // Instancia de un Calendar
        val startCal = calendarToDateToCalendar(start) // Variable que almacena el resultado de la función, basado en el Calendar con la fecha de inicio.

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val formattedDate = dateFormat.format(startCal.time)

        val materialTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H) // Formato de 24 horas
            .setHour(calendar[Calendar.HOUR_OF_DAY]) // Hora actual
            .setMinute(calendar[Calendar.MINUTE]) // Minuto actual
            .setInputMode(INPUT_MODE_KEYBOARD) // Modo de entrada desde teclado
            .setTitleText("Seleccione la hora para $formattedDate (inicio)") // Título del picker
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val hora = materialTimePicker.hour
            val minuto = materialTimePicker.minute
            Log.i("tag", "Hora inicio: $hora | Minuto inicio: $minuto")

            start[Calendar.HOUR_OF_DAY] = hora
            start[Calendar.MINUTE] = minuto
            openEndTimePicker(start, end) // Abrir selector de tiempo de fin
        }

        materialTimePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    /**
     * openEndTimePicker se encarga de abrir una ventana de horas para seleccionar la hora de término.
     * Realiza lo mismo que openStartTimePicker.
     * @param start Calendar que tiene la fecha de inicio
     * @param end Calendar que tiene la fecha de fin
     */
    private fun openEndTimePicker(start: Calendar, end: Calendar) {
        val calendar = Calendar.getInstance()
        val endCal = calendarToDateToCalendar(end)

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val formattedDate = dateFormat.format(endCal.time)

        val materialTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar[Calendar.HOUR_OF_DAY])
            .setMinute(calendar[Calendar.MINUTE])
            .setTitleText("Seleccione la hora para $formattedDate (término)")
            .setInputMode(INPUT_MODE_KEYBOARD)
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val hora = materialTimePicker.hour
            val minuto = materialTimePicker.minute
            Log.i("tag", "Hora fin: $hora | Minuto fin: $minuto")

            end[Calendar.HOUR_OF_DAY] = hora
            end[Calendar.MINUTE] = minuto
            prepareDates(start, end) // Preparar las fechas seleccionadas
        }

        materialTimePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    /**
     * calendarToDateToCalendar prepara la fecha que llega en modo Calendar,
     * le suma un día para tener la fecha correcta, y lo vuelve a convertir a Calendar.
     * @return Un Calendar con la fecha correcta.
     */
    private fun calendarToDateToCalendar(calItem: Calendar): Calendar {
        val dateItem = calItem.time
        var timestampItem = dateItem.time

        timestampItem += 86400000 // Aumenta un día (en milisegundos)

        val newCal = Calendar.getInstance()
        newCal.timeInMillis = timestampItem

        return newCal
    }

    /**
     * prepareDates se encarga de preparar las fechas de inicio y fin para el query,
     * una vez seleccionado el día y hora.
     * @param startCal Calendar que tiene la fecha de inicio
     * @param endCal Calendar que tiene la fecha de fin
     */
    private fun prepareDates(startCal: Calendar, endCal: Calendar) {
        val startDate = startCal.time // Tomamos el tiempo de la fecha inicial (Calendar)
        val endDate = endCal.time // Tomamos el tiempo de la fecha final (Calendar)

        Log.d("tag", "Fecha inicio: $startDate")
        Log.d("tag", "Fecha fin: $endDate")

        // Tomamos el timestamp basado en el Date de la variable
        var startTimestamp = startDate.time
        var endTimestamp = endDate.time

        // Sumamos un día
        startTimestamp += 86400000
        endTimestamp += 86400000

        Log.d("tag", "Timestamps alterados: $startTimestamp $endTimestamp")

        startTS = startTimestamp / 1000 // Convertir a segundos
        endTS = endTimestamp / 1000

        dataFromDevice // Obtener datos del dispositivo
    }

    // Obtiene datos históricos del dispositivo basado en las fechas seleccionadas
    private val dataFromDevice: Unit
        get() {
            val systemTime = System.currentTimeMillis() / 1000
            val startTime = startTS
            val endTime = endTS

            if (systemTime <= endTime) {
                runOnUiThread {
                    TipDialog.show("Error!", WaitDialog.TYPE.ERROR)
                    Toast.makeText(
                        this@ConnectionMST01,
                        "Ocurrió un error con la descarga de datos.", Toast.LENGTH_LONG
                    ).show()
                }
                return
            }

            LogUtil.d("SYSTEM TIME: $systemTime | START TIME: $startTime | END TIME: $endTime")

            runOnUiThread { WaitDialog.show("Espere...") }

            mSensorBleManager!!.readHtHistoryData(
                mMac, startTime, endTime, systemTime
            ) { _, list ->
                runOnUiThread {
                    WaitDialog.dismiss()
                    if (list == null || list.isEmpty()) {
                        TipDialog.show("No hay datos.", WaitDialog.TYPE.WARNING)
                        return@runOnUiThread
                    }

                    Toast.makeText(
                        this@ConnectionMST01,
                        "Datos recibidos con éxito. Tamaño: " + list.size,
                        Toast.LENGTH_LONG
                    ).show()

                    // Comprime los datos recibidos y los envía a GraphMST01
                    val intent = Intent(this@ConnectionMST01, GraphMST01::class.java)

                    val dataAsString = list.toString()

                    Log.d("DATA: ", "DATA BEING SENT: $dataAsString")
                    try {
                        val compressedData = compressData(dataAsString)
                        intent.putExtra("data", compressedData)
                        intent.putExtra("mac", mMac)

                        startActivity(intent)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

    // Funcion que se encarga de comprimir los datos a enviar
    @Throws(IOException::class)
    private fun compressData(data: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        val objectOutputStream = ObjectOutputStream(gzipOutputStream)

        objectOutputStream.writeObject(data)
        objectOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun disConnected() {
        mSensorBleManager!!.disConnect(mMac)
    }
}