package com.unk.recoverrework

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.formatToSinglePrecision
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import com.fasterxml.jackson.databind.ObjectMapper
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import com.minew.ble.v3.utils.LogUtil
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.Device
import org.thingsboard.server.common.data.EntityType
import org.thingsboard.server.common.data.User
import org.thingsboard.server.common.data.id.EntityId
import org.thingsboard.server.common.data.page.PageLink
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

data class HistoryHtData(
    val macAddress: String,
    val temperature: Float,
    val humidity: Float,
    val timestamp: Long
)

private var url = ""
private var tbUser = ""
private var tbPassword = ""
private var entityID = ""
private var pageLink = PageLink(10, 0)
private var listOfDevices: MutableList<Device>? = mutableListOf()

private var historyHTData: List<HistoryHtData> = emptyList()
private var mMac: String = ""
class GraphMST01 : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val compressedData = intent.getByteArrayExtra("data")
        mMac = intent.getStringExtra("mac").toString()

        setContentView(R.layout.generate_report)

        val textTempMin = findViewById<TextView>(R.id.textTempMinValue)
        val textTempMax = findViewById<TextView>(R.id.textTempMaxValue)
        val textTempAvg = findViewById<TextView>(R.id.textTempAvgValue)
        val textStartDate = findViewById<TextView>(R.id.textStartDateValue)
        val textEndDate = findViewById<TextView>(R.id.textEndDateValue)

        // val pngDownload = findViewById<Button>(R.id.pngDownload)
        val excelDownload = findViewById<Button>(R.id.excelDownload)
        val tbSend = findViewById<Button>(R.id.tbSend)
        val sensorsBtn = findViewById<Button>(R.id.sensorsBtn)

        val goBackBtn = findViewById<ImageView>(R.id.goBackBtn)

        val centeredText = findViewById<TextView>(R.id.centeredText)

        val dataAsString = compressedData?.let {
            try {
                decompressData(it)
            } catch (e: IOException) {
                Log.e("DecompressionError", "Failed to decompress data", e)
                null
            } catch (e: ClassNotFoundException) {
                Log.e("ClassError", "Class not found during decompression", e)
                null
            }
        }

        dataAsString?.let { data ->
            historyHTData = parseData(data)

            findViewById<ComposeView>(R.id.composeViewContainer).setContent {
                LineChartScreen01(onGraphReady = {
                    Handler(Looper.getMainLooper()).post {
                        updateTextViews(textTempMin, textTempMax, textTempAvg, textStartDate, textEndDate)
                    }
                })
            }
        }

        /* pngDownload.setOnClickListener {
            Log.d("ButtonClick", "Download PNG button clicked")
        } */

        excelDownload.setOnClickListener {
            exportExcel01(historyHTData)
            Log.d("ButtonClick", "Download Excel button clicked")
        }

        tbSend.setOnClickListener {
            sendToTB01(historyHTData, this@GraphMST01)
            Log.d("ButtonClick", "Send to TB button clicked")
        }

        sensorsBtn.setOnClickListener {
            Log.d("ButtonClick", "Sensors button clicked")
            val intent = Intent(this@GraphMST01, ScanActivity::class.java)
            startActivity(intent)
        }

        goBackBtn.setOnClickListener {
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.textViewToolbar)
        val titleText = getTitleTextFromSomeSource()

        // Set the text of the TextView
        toolbarTitle.text = titleText
        centeredText.text = "MST01"
    }

    private fun getTitleTextFromSomeSource(): String {
        return "ID: $mMac"
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun decompressData(compressedData: ByteArray): String {
        val byteArrayInputStream = ByteArrayInputStream(compressedData)
        val gzipInputStream = GZIPInputStream(byteArrayInputStream)
        val objectInputStream = ObjectInputStream(gzipInputStream)

        val data = objectInputStream.readObject() as String
        objectInputStream.close()
        return data
    }

    @SuppressLint("SetTextI18n")
    private fun updateTextViews(
        textTempMin: TextView,
        textTempMax: TextView,
        textTempAvg: TextView,
        textStartDate: TextView,
        textEndDate: TextView
    ) {
        if (historyHTData.isNotEmpty()) {
            val minTemperature = historyHTData.minOf { it.temperature }
            val maxTemperature = historyHTData.maxOf { it.temperature }
            val avgTemperature = historyHTData.map { it.temperature }.average().toFloat()
            val startDate = Date(historyHTData.first().timestamp)
            val endDate = Date(historyHTData.last().timestamp)

            textTempMin.text = "${minTemperature.formatToSinglePrecision()}°C"
            textTempMax.text = "${maxTemperature.formatToSinglePrecision()}°C"
            textTempAvg.text = "${avgTemperature.formatToSinglePrecision()}°C"
            textStartDate.text = formatDate(startDate)
            textEndDate.text = formatDate(endDate)
        } else {
            textTempMin.text = "N/A"
            textTempMax.text = "N/A"
            textTempAvg.text = "N/A"
            textStartDate.text = "N/A"
            textEndDate.text = "N/A"
        }
    }
}

@Composable
fun LineChartScreen01(onGraphReady: () -> Unit) {
    LaunchedEffect(Unit) {
        onGraphReady()
    }

    val pointsData: List<Point> = historyHTData.mapIndexed { index, htData ->
        val xVal = index.toFloat()
        val yVal = htData.temperature
        Point(xVal, yVal)
    }

    val steps = 1

    val xAxisData = AxisData.Builder()
        .axisStepSize(100.dp)
        .backgroundColor(Color.Transparent)
        .steps(steps)
        .labelAndAxisLinePadding(15.dp)
        .build()

    val yAxisData = AxisData.Builder()
        .steps(steps + 2)
        .backgroundColor(Color.Transparent)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            val yMin = pointsData.minOf { it.y }
            val yMax = pointsData.maxOf { it.y }

            val yScale = (yMax - yMin) / (steps + 2).toFloat()
            (i * yScale + yMin).formatToSinglePrecision()
        }
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = pointsData,
                    LineStyle(width = 3f),
                    null,
                    SelectionHighlightPoint(),
                    null,
                    SelectionHighlightPopUp()
                )
            ),
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = Color.White
    )

    LineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        lineChartData = lineChartData
    )
}

fun parseData(data: String): List<HistoryHtData> {
    val historyDataList = mutableListOf<HistoryHtData>()
    val pattern = Pattern.compile("HtHistoryData\\{macAddress=(.*?), temperature=(.*?), humidity=(.*?), time=(.*?), thState=(.*?)\\}")
    val matcher = pattern.matcher(data)
    while (matcher.find()) {
        val macAddress = matcher.group(1).replace("'", "")
        val temperature = matcher.group(2).toFloat()
        val humidity = matcher.group(3).toFloat()
        val timestamp = matcher.group(4).toLong()
        historyDataList.add(HistoryHtData(macAddress, temperature, humidity, timestamp))
    }
    return historyDataList
}

private fun formatDate(date: Date): String {
    val format = SimpleDateFormat("dd-MM-yyyy HH:mm")
    return format.format(date)
}

private fun exportExcel01(data: List<HistoryHtData>) {
    WaitDialog.show("Descargando...")

    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var systemTimeDate = dateFormat.format(Date())

    val workbook = HSSFWorkbook()
    val sheet = workbook.createSheet("DATOS $systemTimeDate")

    val header = sheet.createRow(0)

    header.createCell(0).setCellValue("TIMESTAMP")
    header.createCell(1).setCellValue("MAC")
    header.createCell(2).setCellValue("TEMPERATURA")
    header.createCell(3).setCellValue("HUMEDAD")

    val decimals = DecimalFormat("#.##")
    decimals.isDecimalSeparatorAlwaysShown = false

    val dateTimeFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    data?.forEachIndexed { i, htData ->
        val row = sheet.createRow(i + 1)

        val date = dateTimeFormat.format(Date(htData.timestamp))

        row.createCell(0).setCellValue(date)
        row.createCell(1).setCellValue(htData.macAddress)
        row.createCell(2).setCellValue(
            decimals.format(htData.temperature.toDouble()).toDouble().toString() + "°C"
        )
        row.createCell(3).setCellValue(htData.humidity.toString() + "°C")
    }

    try {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        systemTimeDate = dateTimeFormat.format(Date()).replace(" ", "_").replace(":", "-")

        val file = File(directory, "DATOS_${systemTimeDate}_RECOVER.xls")

        if (!file.parentFile?.exists()!!) {
            file.parentFile?.mkdirs()
        }

        FileOutputStream(file).use { fileOut ->
            workbook.write(fileOut)
        }

        TipDialog.show("Descarga exitosa!", WaitDialog.TYPE.SUCCESS)

        Log.d("ExcelTest", "File path: " + file.absolutePath)
        Log.d("CREATED.", "EXCEL WAS CREATED.")
    } catch (e: Exception) {
        e.printStackTrace()
        Log.d("FAILED.", "ERROR CREATING EXCEL.")
        TipDialog.show("Error de descarga.", WaitDialog.TYPE.ERROR)
    }

    println("Data recibida: $data")
}

@RequiresApi(Build.VERSION_CODES.N)
private fun sendToTB01(data: List<HistoryHtData>, context: Context) {
    WaitDialog.show("Enviando datos...")
    val systemTime = System.currentTimeMillis() / 1000 // Current system time in seconds

    pageLink = PageLink(40, 0) // Initialize pageLink
    macAddress = historyHTData[0].macAddress

    Log.d("mac", "the mac address: $macAddress")

    Thread {
        try {
            val client = RestClient(url)
            client.login(tbUser, tbPassword)

            client.user.ifPresent { user: User ->
                LogUtil.d(user.toString())
            }

            var matchFound = false

            while (!matchFound) {
                try {
                    Log.d("pagelink", "PageLink: $pageLink")

                    val customers = client.getCustomers(pageLink).data
                    LogUtil.d("Customers: $customers")

                    if (customers.isEmpty()) {
                        break
                    }

                    listOfDevices = mutableListOf()

                    for (customer in customers) {
                        LogUtil.d("CUSTOMER: ${customer.name} | CUSTOMER ID: ${customer.id}")

                        var customerDevicesPageLink = PageLink(40, 0)

                        while (true) {
                            val devices = client.getCustomerDevices(customer.id, "SS-Sensor", customerDevicesPageLink)

                            if (devices?.data.isNullOrEmpty()) {
                                LogUtil.d("NO DEVICES FOUND FOR CUSTOMER.")
                                break
                            } else {
                                listOfDevices?.addAll(devices.data ?: emptyList())
                                println("DEVICES: $listOfDevices")

                                for ((index, device) in (listOfDevices ?: emptyList()).withIndex()) {
                                    LogUtil.d("DEVICE: ${device.name} | DEVICE ID: ${device.id}")

                                    val newMac = macAddress.replace(":", "")
                                    Log.d("newmac", "NEW MAC: $newMac")

                                    if (newMac == device.name) {
                                        Log.d("equal", "same")
                                        entityID = device.id.toString()
                                        matchFound = true
                                        break
                                    } else {
                                        Log.d("not equal", "not same, sobs.")
                                    }
                                }

                                if (matchFound) {
                                    break
                                }

                                customerDevicesPageLink = PageLink(customerDevicesPageLink.pageSize, customerDevicesPageLink.page + 1)
                            }
                        }

                        if (matchFound) {
                            break
                        }
                    }

                    if (!matchFound) {
                        LogUtil.d("No match found, re-running the query with new pageLink.")
                        pageLink = PageLink(pageLink.pageSize, pageLink.page + 1)
                    }

                } catch (e: Exception) {
                    LogUtil.d("ERROR: $e")
                    break
                }
            }

            if (matchFound) {
                val chunkSize = 20000
                val chunks = data.chunked(chunkSize)

                for (chunk in chunks) {
                    val fullJson = StringBuilder("[")

                    for (htData in chunk) {
                        try {
                            val jsonFormat = String.format(
                                "{ \"ts\": %d, \"values\": { \"temperature\": %.1f, \"humidity\": %.1f, \"mac\": \"%s\" }}",
                                htData.timestamp, htData.temperature, htData.humidity, htData.macAddress
                            )

                            if (fullJson.length > 1) {
                                fullJson.append(", ")
                            }
                            fullJson.append(jsonFormat)
                        } catch (e: Exception) {
                            LogUtil.d("ERROR: $e")
                        }
                    }

                    fullJson.append("]")
                    LogUtil.d("FULL JSON: $fullJson")

                    val objectMapper = ObjectMapper()
                    val jsonArray = objectMapper.readTree(fullJson.toString())

                    val eID: EntityId = object : EntityId {
                        override fun getId(): UUID {
                            return UUID.fromString(entityID)
                        }

                        override fun getEntityType(): EntityType {
                            return EntityType.DEVICE
                        }
                    }

                    println("ID: ${eID.id}")
                    println("Entity Type: ${eID.entityType}")

                    try {
                        client.saveEntityTelemetry(eID, (systemTime * 1000).toString(), jsonArray)
                        LogUtil.d("DATA SENT. BREAKING LOOP.")
                    } catch (e: Exception) {
                        LogUtil.d("Fail: $e")
                        triggerLogSave(context)
                    }
                }

                TipDialog.show("Envío exitoso!", WaitDialog.TYPE.SUCCESS)

            } else {
                TipDialog.show("Error de envío", WaitDialog.TYPE.ERROR)
                triggerLogSave(context)
            }

            LogUtil.d("Closing connection.")

            client.logout()
            client.close()

            LogUtil.d("Connection closed successfully.")
        } catch (e: Exception) {
            LogUtil.d("Login failed: $e")
            TipDialog.show("Error de envío", WaitDialog.TYPE.ERROR)
            triggerLogSave(context)
        }
    }.start()
}


// Function to trigger log saving
private fun triggerLogSave(context: Context) {
    val customCrashHandler = CrashHandler(context)
    val log = customCrashHandler.getLogcatOutput()
    customCrashHandler.saveLogToFile(log)
}