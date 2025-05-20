// Recover - Hecho por UNK Latam 2024
package com.unk.recoverrework

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.minew.ble.mst03.bean.HtData
import com.minew.ble.mst03.manager.MST03SensorBleManager
import com.minew.ble.v3.enums.BleConnectionState
import com.minew.ble.v3.utils.LogUtil
import com.unk.recoverrework.databinding.DeviceConnectedBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

// Clase que gestiona la conexión con dispositivos MST03 a través de BLE
class ConnectionMST03 : BaseActivity() {

    // Inicialización de variables
    private var binding: DeviceConnectedBinding? = null // Enlace de vista utilizando ViewBinding
    private var mBleManager: MST03SensorBleManager? = null // Gestor de BLE para dispositivos MST03
    private var mMac: String? = "" // Dirección MAC del dispositivo BLE
    private var startTS: Long = 0 // Timestamp de inicio de la fecha seleccionada
    private var endTS: Long = 0 // Timestamp de fin de la fecha seleccionada
    private var systemTime: Long = 0 // Hora del sistema en segundos
    private var startTime: Long = 0 // Tiempo de inicio en segundos
    private var endTime: Long = 0 // Tiempo de fin en segundos
    private var htDataList: List<HtData>? = null // Lista de datos de historial del dispositivo

    // Método onCreate que inicializa la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeviceConnectedBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Inicializa el gestor BLE y los datos
        initBleManager()
        initData()
        initListener()

        // Configura la barra de herramientas (Toolbar)
        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        val goBack = findViewById<ImageView>(R.id.goBackBtn)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.textViewToolbar)
        val titleText = titleTextFromSomeSource

        // Establece el texto del título del Toolbar
        toolbarTitle.text = titleText

        // Listener para el botón de retroceso
        goBack.setOnClickListener {
            val i = Intent(this@ConnectionMST03, ScanActivity::class.java)
            startActivity(i)
        }
    }

    // Metodo para obtener el texto del título basado en la dirección MAC
    private val titleTextFromSomeSource: String
        get() = "ID: $mMac"

    // Metodo llamado al destruir la actividad
    override fun onDestroy() {
        super.onDestroy()
        disConnected() // Desconecta el dispositivo BLE
    }

    // Inicializa el gestor BLE y establece el listener de estado de conexión
    private fun initBleManager() {
        mBleManager = MST03SensorBleManager.getInstance()

        // Listener para los diferentes estados de conexión BLE
        mBleManager?.setOnConnStateListener { _, sensorConnectionState ->
            when (sensorConnectionState) {
                BleConnectionState.Connecting -> Log.d("TAG", "Connecting")
                BleConnectionState.Connected -> Log.d("TAG", "Connected")
                BleConnectionState.ConnectComplete -> Log.d("TAG", "ConnectComplete")
                BleConnectionState.Disconnect -> {
                    Log.d("TAG", "Disconnect")
                    finish() // Finaliza la actividad si se desconecta
                }
                else -> {}
            }
        }
    }

    // Recupera la dirección MAC del Intent
    private fun initData() {
        mMac = intent.getStringExtra("mac")
    }

    // Establece listeners de los elementos de la interfaz
    private fun initListener() {
        binding!!.selectDates.setOnClickListener { openDateRangePicker() }
    }

    // Abre el selector de rango de fechas usando MaterialDatePicker
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

            com.ylwl.industry.utils.LogUtil.d("START: " + startCal.time + " | END: " + endCal.time)
            openStartTimePicker(startCal, endCal)
        }

        materialDatePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    // Abre el selector de tiempo de inicio
    private fun openStartTimePicker(start: Calendar, end: Calendar) {
        val calendar = Calendar.getInstance()
        val startCal = calendarToDateToCalendar(start)

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

    // Abre el selector de tiempo de fin
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

    // Convierte el Calendar a timestamp y luego lo ajusta a un día más
    private fun calendarToDateToCalendar(calItem: Calendar): Calendar {
        val dateItem = calItem.time
        var timestampItem = dateItem.time

        timestampItem += 86400000 // Aumenta un día (en milisegundos)

        val newCal = Calendar.getInstance()
        newCal.timeInMillis = timestampItem

        return newCal
    }

    // Prepara las fechas seleccionadas, las convierte a timestamps y actualiza las variables
    private fun prepareDates(startCal: Calendar, endCal: Calendar) {
        val startDate = startCal.time
        val endDate = endCal.time

        Log.d("tag", "Fecha inicio: $startDate")
        Log.d("tag", "Fecha fin: $endDate")

        var startTimestamp = startDate.time
        var endTimestamp = endDate.time

        startTimestamp += 86400000
        endTimestamp += 86400000

        Log.d("tag", "Timestamps alterados: $startTimestamp $endTimestamp")

        startTS = startTimestamp / 1000 // Convertir a segundos
        endTS = endTimestamp / 1000

        dataFromDevice // Obtener datos del dispositivo
    }

    // Metodo para comprimir los datos obtenidos
    @Throws(IOException::class)
    private fun compressData(data: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        val objectOutputStream = ObjectOutputStream(gzipOutputStream)

        objectOutputStream.writeObject(data)
        objectOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    // Obtiene los datos históricos del dispositivo BLE
    private val dataFromDevice: Unit
        get() {
            // Las fechas deben quedar en segundos.
            systemTime = System.currentTimeMillis() / 1000 // Tiempo actual en segundos.
            startTime = startTS // Un día atrás (24 horas) en segundos.
            endTime = endTS

            val rule =
                1 // 0 es para conseguir TODA la data. 1 es para un cierto tiempo (startTime y endTime).

            Thread {
                runOnUiThread { WaitDialog.show("Espere...") }
                mBleManager!!.queryHistoryData(
                    mMac, rule, startTime, endTime, systemTime
                ) { success, historyHtData ->
                    runOnUiThread {
                        WaitDialog.dismiss()
                        LogUtil.d("selectHTHistoryData Result: $success")
                        if (success) {
                            htDataList = historyHtData.historyDataList
                            val sizeDataList = (htDataList as MutableList<HtData>?)?.size

                            LogUtil.d("Lista almacenada: $htDataList")
                            LogUtil.d("Tamaño data: $sizeDataList")

                            if (sizeDataList == 0) {
                                TipDialog.show("No hay datos", WaitDialog.TYPE.WARNING)
                            } else {
                                val intent = Intent(this@ConnectionMST03, GraphMST03::class.java)

                                val dataAsString = htDataList.toString()

                                try {
                                    val compressedData = compressData(dataAsString)
                                    intent.putExtra("data", compressedData)
                                    intent.putExtra("mac", mMac)

                                    startActivity(intent)

                                    Toast.makeText(
                                        this@ConnectionMST03,
                                        "Datos recibidos con éxito. Tamaño: $sizeDataList", Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            TipDialog.show("Error!", WaitDialog.TYPE.ERROR)
                            Toast.makeText(
                                this@ConnectionMST03,
                                "Ocurrió un error con la descarga de datos.", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }.start()
        }

    // Metodo para desconectar el dispositivo BLE
    private fun disConnected() {
        mBleManager!!.disConnect(mMac!!)
    }
}