// Recover - Hecho por UNK Latam 2024
package com.unk.recoverrework

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kongzue.dialogx.dialogs.WaitDialog
import com.minew.ble.mst03.bean.MST03Entity
import com.minew.ble.mst03.manager.MST03SensorBleManager
import com.minew.ble.v3.enums.BleConnectionState
import com.minew.ble.v3.interfaces.OnConnStateListener
import com.minew.ble.v3.interfaces.OnScanDevicesResultListener
import com.unk.recoverrework.databinding.ScanDevicesActivityBinding
import com.ylwl.industry.bean.IndustrialHtSensor
import com.ylwl.industry.enums.MSensorConnectionState
import com.ylwl.industry.interfaces.OnScanSensorResultListener
import com.ylwl.industry.manager.IndustrySensorBleManager
import java.util.Locale
import java.util.stream.Collectors

/**
 * Actividad para escanear y conectar dispositivos BLE (MST03 y MST01).
 */
class ScanActivity : BaseActivity() {
    // Etiqueta para logs
    private var TAG = "ScanActivity"

    // Estado de conexión
    private var CONNECT_STATUS: Boolean = false

    // Indicador de si se está instalando un perfil
    private var INSTALLING_PROFILE: Boolean = false

    // Código para solicitar permisos
    private var REQUEST_PERMISSION_CODE = 123

    // Binding para acceder a las vistas del layout
    private lateinit var binding: ScanDevicesActivityBinding

    // Animador para la animación de rotación
    private lateinit var objectAnimator: ObjectAnimator

    // Adaptadores para las listas de dispositivos MST03 y MST01
    private lateinit var MST03ListAdapter: MST03Adapter
    private lateinit var MST01ListAdapter: MST01Adapter

    // Gestores BLE para MST03 y MST01
    private lateinit var MST03BleManager: MST03SensorBleManager
    private lateinit var MST01BleManager: IndustrySensorBleManager

    // Entidades para los dispositivos MST03 y MST01 seleccionados
    private lateinit var entity03: MST03Entity
    private lateinit var entity01: IndustrialHtSensor

    // Variables para el filtro de busqueda
    private var searchView: SearchView? = null
    private var isFiltering = false
    private val scanning = false

    /**
     * Método llamado al crear la actividad.
     * Inicializa las vistas, animaciones y gestores BLE, y solicita permisos necesarios.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar el layout usando View Binding
        binding = ScanDevicesActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar componentes de la interfaz y BLE
        initToolbar()
        initRefresh()
        initRecyclerView()
        initAnimator()
        initBleManager()
        initBlePerms()

        initSearchview()
    }

    /**
     * Método llamado cuando la actividad se inicia.
     * Configura los listeners del gestor BLE.
     */
    override fun onStart() {
        super.onStart()
        setBleManagerListener()

        startScan()
    }

    /**
     * Método llamado cuando la actividad se detiene.
     * Remueve los listeners del gestor BLE.
     */
    override fun onStop() {
        super.onStop()
        removeBleManagerListener()

        stopScan()
    }

    /**
     * Inicializa la barra de herramientas (Toolbar).
     */
    private fun initToolbar() {
        setToolbar(binding.toolBar)
    }

    /**
     * Configura el comportamiento del SwipeRefreshLayout para refrescar la lista de dispositivos.
     */
    private fun initRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            stopScan() // Detiene el escaneo actual
            startScan() // Inicia un nuevo escaneo
            binding.swipeRefreshLayout.isRefreshing = false // Detiene la animación de refresco
        }
    }

    /**
     * Inicializa los RecyclerViews para listar dispositivos MST03 y MST01.
     */
    private fun initRecyclerView() {
        // Configurar el layout manager para listas verticales
        binding.recyclerViewMST03.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMST01.layoutManager = LinearLayoutManager(this)

        // Inicializar adaptadores con el layout de ítem y lista vacía
        MST03ListAdapter = MST03Adapter(R.layout.item_device, null)
        MST01ListAdapter = MST01Adapter(R.layout.item_device, null)

        // Configurar el listener para clic en ítems de MST03
        MST03ListAdapter.setOnItemClickListener { adapter, view, position ->
            entity03 = MST03ListAdapter.getItem(position)!! // Obtener el dispositivo seleccionado
            stopScan() // Detener el escaneo
            setKey(entity03, "minewtech1234567") // Intentar establecer la clave de conexión
        }
        binding.recyclerViewMST03.adapter = MST03ListAdapter // Asignar adaptador al RecyclerView

        // Configurar el listener para clic en ítems de MST01
        MST01ListAdapter.setOnItemClickListener{ adapter, view, position ->
            entity01 = MST01ListAdapter.getItem(position)!! // Obtener el dispositivo seleccionado
            stopScan() // Detener el escaneo
            setKey(entity01, "minewtech1234567") // Intentar establecer la clave de conexión
        }
        binding.recyclerViewMST01.adapter = MST01ListAdapter // Asignar adaptador al RecyclerView
    }

    /**
     * Inicializa el animador para rotar el botón de escaneo.
     */
    private fun initAnimator() {
        // Crear un ObjectAnimator que rota el botón de escaneo de 0 a 360 grados
        objectAnimator = ObjectAnimator.ofFloat(binding.ibHomeScan, "rotation", 0f, 360f)
        objectAnimator.duration = 1500 // Duración de la animación en milisegundos
        objectAnimator.repeatCount = ValueAnimator.INFINITE // Repetir infinitamente
    }

    /**
     * Inicializa los gestores BLE para MST03 y MST01.
     */
    private fun initBleManager() {
        // Obtener instancias únicas de los gestores BLE
        MST03BleManager = MST03SensorBleManager.getInstance()
        MST01BleManager = IndustrySensorBleManager.getInstance()
    }

    /**
     * Configura los listeners para los estados de conexión de los gestores BLE.
     */
    private fun setBleManagerListener() {
        if (MST03BleManager != null) {
            MST03BleManager.setOnConnStateListener(MST03ConnStateListener) // Asignar listener para MST03
        }

        if (MST01BleManager != null) {
            MST01BleManager.setOnConnStateListener(MST01ConnStateListener) // Asignar listener para MST01
        }
    }

    /**
     * Remueve los listeners de conexión de los gestores BLE.
     */
    private fun removeBleManagerListener() {
        if (MST03BleManager != null) {
            MST03BleManager.setOnConnStateListener(null) // Remover listener para MST03
        }

        if (MST01BleManager != null) {
            MST01BleManager.setOnConnStateListener(null) // Remover listener para MST01
        }
    }

    /**
     * Listener para los cambios de estado de conexión de dispositivos MST03.
     */
    private val MST03ConnStateListener =
        OnConnStateListener { _, connState ->
            when (connState) {
                BleConnectionState.Connecting -> {
                    Log.d(TAG, "Connecting MST03...") // Log del estado de conexión
                    CONNECT_STATUS = false // Actualizar estado de conexión
                }

                BleConnectionState.Connected -> {
                    Log.d(TAG, "MST03 Connected.") // Log cuando MST03 está conectado
                }

                BleConnectionState.ConnectComplete -> {
                    Log.i(TAG, "MST03 Connection was completed.") // Log de finalización de conexión
                    WaitDialog.dismiss() // Descartar el diálogo de espera

                    // Crear intent para navegar a la actividad de conexión MST03
                    val intent = Intent(this@ScanActivity, ConnectionMST03::class.java)
                    intent.putExtra("mac", entity03.macAddress) // Pasar la dirección MAC del dispositivo

                    CONNECT_STATUS = true // Actualizar estado de conexión
                    startActivity(intent) // Iniciar la actividad de conexión
                }

                BleConnectionState.Disconnect -> {
                    if (CONNECT_STATUS) {
                        Log.i(TAG, "Disconnection of MST03") // Log de desconexión
                        WaitDialog.dismiss() // Descartar el diálogo de espera
                        CONNECT_STATUS = false // Actualizar estado de conexión
                    } else {
                        if (INSTALLING_PROFILE) {
                            Log.e(TAG, "Retrying first key 'cause we're installing.") // Log de reintento de clave
                            setKey(entity03, "minewtech1234567") // Reintentar con la primera clave
                        } else {
                            Log.e(TAG, "First key didn't work, trying second one.") // Log de intento con segunda clave
                            setKey(entity03, "3141592653589793") // Intentar con una segunda clave
                            INSTALLING_PROFILE = true // Marcar que se está instalando un perfil
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "else case") // Log para otros estados no manejados
                }
            }
        }

    /**
     * Listener para los cambios de estado de conexión de dispositivos MST01.
     */
    private val MST01ConnStateListener =
        com.ylwl.industry.interfaces.OnConnStateListener { _, connState ->
            when (connState) {
                MSensorConnectionState.Connecting -> {
                    Log.d(TAG, "Connecting MST01...") // Log del estado de conexión
                    CONNECT_STATUS = false // Actualizar estado de conexión
                }

                MSensorConnectionState.Connected -> {
                    Log.d(TAG, "MST01 Connected.") // Log cuando MST01 está conectado
                }

                MSensorConnectionState.ConnectComplete -> {
                    Log.i(TAG, "MST01 Connection was completed.") // Log de finalización de conexión
                    WaitDialog.dismiss() // Descartar el diálogo de espera

                    // Crear intent para navegar a la actividad de conexión MST01
                    val intent = Intent(this@ScanActivity, ConnectionMST01::class.java)
                    intent.putExtra("mac", entity01.macAddress) // Pasar la dirección MAC del dispositivo

                    CONNECT_STATUS = true // Actualizar estado de conexión
                    startActivity(intent) // Iniciar la actividad de conexión
                }

                MSensorConnectionState.Disconnect -> {
                    if (CONNECT_STATUS) {
                        Log.i(TAG, "Disconnection of MST01") // Log de desconexión
                        WaitDialog.dismiss() // Descartar el diálogo de espera
                        CONNECT_STATUS = false // Actualizar estado de conexión
                    } else {
                        if (INSTALLING_PROFILE) {
                            Log.e(TAG, "Retrying first key 'cause we're installing.") // Log de reintento de clave
                            setKey(entity01, "minewtech1234567") // Reintentar con la primera clave
                        } else {
                            Log.e(TAG, "First key didn't work, trying second one.") // Log de intento con segunda clave
                            setKey(entity01, "3141592653589793") // Intentar con una segunda clave
                            INSTALLING_PROFILE = true // Marcar que se está instalando un perfil
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "else case") // Log para otros estados no manejados
                }
            }
        }

    /**
     * Inicializa y solicita los permisos necesarios para el funcionamiento BLE.
     */
    private fun initBlePerms() {
        // Definir la lista de permisos a solicitar según la versión de Android
        val requestPermissionList: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // Verificar si todos los permisos ya fueron otorgados
        val allPermissionsGranted = requestPermissionList.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            // Si los permisos ya están concedidos, iniciar el escaneo de dispositivos
            startScan()
        } else {
            // Si faltan permisos, solicitarlos al usuario
            ActivityCompat.requestPermissions(this, requestPermissionList, REQUEST_PERMISSION_CODE)
        }
    }

    /**
     * Inicia el escaneo de dispositivos BLE para MST03 y MST01.
     */
    private fun startScan() {
        if (MST03BleManager != null && MST01BleManager != null) {
            try {
                // Iniciar escaneo para dispositivos MST03 con un tiempo de escaneo de 5 minutos
                MST03BleManager.startScan(this, 5 * 60 * 1000, object : OnScanDevicesResultListener<MST03Entity> {
                    override fun onScanResult(list: MutableList<MST03Entity>?) {
                        Log.i(TAG, "MST03 List: $list") // Log de los dispositivos encontrados
                        MST03ListAdapter.submitList(list) // Actualizar el adaptador con la lista encontrada
                    }

                    override fun onStopScan(list: MutableList<MST03Entity>?) {
                        Log.d(TAG, "Stopped scan for MST03.") // Log al detener el escaneo
                    }

                })

                // Iniciar escaneo para dispositivos MST01 con un tiempo de escaneo de 5 minutos
                MST01BleManager.startScan(this, 5 * 60 * 1000, object : OnScanSensorResultListener {
                    override fun onScanResult(list: MutableList<IndustrialHtSensor>?) {
                        Log.i(TAG, "MST01 List: $list") // Log de los dispositivos encontrados
                        MST01ListAdapter.submitList(list) // Actualizar el adaptador con la lista encontrada
                    }

                    override fun onStopScan(list: MutableList<IndustrialHtSensor>?) {
                        Log.d(TAG, "Stopped scan for MST01.") // Log al detener el escaneo
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error at startScan: $e") // Log de errores al iniciar el escaneo
            }
        } else {
            Log.e(TAG, "BT Managers are not initialized.") // Log si los gestores BLE no están inicializados
        }
    }

    /**
     * Detiene el escaneo de dispositivos BLE.
     */
    private fun stopScan() {
        if (MST03BleManager != null) {
            MST03BleManager.stopScan(this) // Detener escaneo de MST03
        }
        if (MST01BleManager != null) {
            MST01BleManager.stopScan(this) // Detener escaneo de MST01
        }
    }

    /**
     * Establece la clave de conexión para un dispositivo BLE y lo conecta.
     *
     * @param entity Dispositivo BLE (MST03Entity o IndustrialHtSensor)
     * @param password Clave a establecer para la conexión
     */
    private fun setKey(entity: Any?, password: String) {
        if (entity is MST03Entity) {
            val mac = entity.macAddress
            try {
                Log.i(TAG, "Trying key ${password} for MST03...") // Log del intento de clave
                MST03BleManager.setSecretKey(mac, password) // Establecer clave para MST03
                connectSensor(entity) // Conectar el sensor MST03
            } catch (e: Exception) {
                Log.e(TAG, "Error at setting MST03 Key ${password}: $e") // Log de errores al establecer clave
            }
        } else if (entity is IndustrialHtSensor) {
            try {
                Log.i(TAG, "Trying key ${password} for MST01...") // Log del intento de clave
                MST01BleManager.setSecretKey(password) // Establecer clave para MST01
                connectSensor(entity) // Conectar el sensor MST01
            } catch (e: Exception) {
                Log.e(TAG, "Error at setting MST01 Key ${password}: $e") // Log de errores al establecer clave
            }
        }
    }

    /**
     * Conecta un sensor BLE después de establecer la clave.
     *
     * @param entity Dispositivo BLE (MST03Entity o IndustrialHtSensor)
     */
    private fun connectSensor(entity: Any?) {
        if (entity != null) {
            if (entity is MST03Entity) {
                WaitDialog.show("Conectando a MST03...") // Mostrar diálogo de espera
                MST03BleManager.connect(this, entity) // Iniciar conexión con MST03
            }

            if (entity is IndustrialHtSensor) {
                WaitDialog.show("Conectando a MST01...") // Mostrar diálogo de espera
                MST01BleManager.connect(this, entity) // Iniciar conexión con MST01
            }
        }
    }

    private fun initSearchview() {
        // Initialize the searchView
        searchView = findViewById(R.id.searchView)

        // Check if searchView is not null before proceeding
        searchView?.let { view ->
            // Access and modify searchView components
            val searchIcon: ImageView = view.findViewById(androidx.appcompat.R.id.search_mag_icon)
            searchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_search_icon))

            view.findViewById<View>(androidx.appcompat.R.id.search_plate)
                .setBackgroundResource(R.drawable.search_view_bg)

            val searchText: TextView = view.findViewById(androidx.appcompat.R.id.search_src_text)
            searchText.setHintTextColor(Color.GRAY)

            // Set the query text listener
            view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { filterList(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        isFiltering = false
                        if (!scanning) {
                            startScan()
                        }
                    } else {
                        filterList(newText)
                    }
                    return true
                }
            })
        }
    }

    private fun filterList(query: String) {
        isFiltering = true // Inicia el filtro
        stopScan() // Detiene el escaneo mientras filtra

        val lowcaseQuery = query.lowercase(Locale.getDefault())

        Log.i("Query", lowcaseQuery)

        // Filter MST03Adapter
        val MST03Items: List<MST03Entity> =
            if (MST03ListAdapter.items != null) MST03ListAdapter.items else ArrayList()
        val filteredMst03List: List<MST03Entity>? = MST03Items.stream()
            .filter { item: MST03Entity ->
                "Recover 03".lowercase(
                    Locale.getDefault()
                )
                    .contains(lowcaseQuery) || removeColons(item.macAddress).lowercase(Locale.getDefault())
                    .contains(lowcaseQuery)
            }
            .collect(Collectors.toList())
        Log.i("Filtered MST03", filteredMst03List.toString())
        MST03ListAdapter.submitList(filteredMst03List)

        // Filter MST01Adapter
        val MST01Items: List<IndustrialHtSensor> =
            if (MST01ListAdapter.items != null) MST01ListAdapter.items else ArrayList()
        val filteredMst01List: List<IndustrialHtSensor>? = MST01Items.stream()
            .filter { item: IndustrialHtSensor ->
                "Recover 01".lowercase(
                    Locale.getDefault()
                )
                    .contains(lowcaseQuery) || removeColons(item.macAddress).lowercase(Locale.getDefault())
                    .contains(lowcaseQuery)
            }
            .collect(Collectors.toList())
        Log.i("Filtered MST01", filteredMst01List.toString())
        MST01ListAdapter.submitList(filteredMst01List)
    }

    // Helper method to remove colons from MAC addresses
    private fun removeColons(macAddress: String): String {
        return macAddress.replace(":", "")
    }
}
