package com.example.dispensador_iot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBtManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dispensador_iot.databinding.ActivityOwnerBinding

class DuenioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBinding
    private var cantidadRacion = 25
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        if (permisos.values.all { it }) {
            seleccionarDispositivoYConectar()
        } else {
            Toast.makeText(this, "Permisos Bluetooth denegados", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BluetoothManager.inicializar(this)
        val systemBtManager = getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBtManager
        bluetoothAdapter = systemBtManager.adapter

        // 1. Conectar callbacks de estado y mensajes recibidos (ya corren en UI Thread)
        BluetoothManager.onEstadoCambiado = { estado ->
            when (estado) {
                BluetoothManager.EstadoConexion.CONECTADO -> {
                    binding.tvEstadoConexion.text = "Estado: CONECTADO"
                    binding.tvEstadoConexion.setTextColor(getColor(android.R.color.holo_green_dark))
                }
                BluetoothManager.EstadoConexion.RECONECTANDO -> {
                    binding.tvEstadoConexion.text = "Estado: RECONECTANDO..."
                    binding.tvEstadoConexion.setTextColor(getColor(android.R.color.holo_orange_dark))
                }
                BluetoothManager.EstadoConexion.DESCONECTADO -> {
                    binding.tvEstadoConexion.text = "Estado: DESCONECTADO"
                    binding.tvEstadoConexion.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }

        BluetoothManager.onMensajeRecibido = { linea ->
            procesarMensajeEntrante(linea)
        }

        // Inicializar indicador de nivel predeterminado para que no inicie en 0%
        binding.tvComidaPanel.text = "Nivel: 50%"
        binding.sbNivelComida.progress = 50

        // 2. Controles UI
        binding.sbCantidadDispensar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cantidadRacion = if (progress < 10) 10 else progress
                binding.tvCantidadDispensar.text = "Cantidad a dispensar: $cantidadRacion%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnDispensar.setOnClickListener {
            val cmd = ProtocoloIoT.crearComandoDispensar(cantidadRacion)
            BluetoothManager.enviarMensaje(cmd)
            Toast.makeText(this, "Orden enviada ($cantidadRacion%)", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelar.setOnClickListener {
            cantidadRacion = 25
            binding.sbCantidadDispensar.progress = 25
            binding.tvCantidadDispensar.text = "Cantidad a dispensar: 25%"
        }

        binding.btnRegresar.setOnClickListener {
            BluetoothManager.detener()
            finish()
        }

        // 3. Solicitar permisos e iniciar conexión real
        verificarPermisosEIniciar()
    }

    private fun verificarPermisosEIniciar() {
        val permisosNecesarios = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val faltantes = permisosNecesarios.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltantes.isEmpty()) {
            seleccionarDispositivoYConectar()
        } else {
            requestPermissionsLauncher.launch(faltantes.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun seleccionarDispositivoYConectar() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            Toast.makeText(this, "Activa el Bluetooth primero", Toast.LENGTH_LONG).show()
            return
        }

        val emparejados = adapter.bondedDevices?.toList() ?: emptyList()
        if (emparejados.isEmpty()) {
            Toast.makeText(this, "Empareja el ESP32 o teléfono dispensador en Ajustes", Toast.LENGTH_LONG).show()
            return
        }

        val nombres = emparejados.map { "${it.name ?: "Dispositivo"} (${it.address})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Dispensador IoT")
            .setItems(nombres) { _, which ->
                BluetoothManager.conectarComoCliente(adapter, emparejados[which].address)
            }
            .setCancelable(false)
            .show()
    }

    fun procesarMensajeEntrante(mensaje: String) {
        // Descarta mensajes corruptos o sin checksum válido
        val datos = ProtocoloIoT.validarYParsear(mensaje) ?: return
        val (tipo, clave, valor) = datos

        when (tipo) {
            "LECTURA" -> {
                if (clave == "nivel") {
                    val nivel = valor.toIntOrNull() ?: 0
                    binding.tvComidaPanel.text = "Nivel: $nivel%"
                    binding.sbNivelComida.progress = nivel
                    if (nivel < 20) {
                        Toast.makeText(this, "¡ALERTA! Comida baja (<20%)", Toast.LENGTH_LONG).show()
                    }
                } else if (clave == "presencia") {
                    binding.tvPresenciaPanel.text = if (valor == "1") "Mascota: Presente" else "Mascota: Ausente"
                }
            }
            "ACK" -> {
                if (clave == "dispensar" && valor == "OK") {
                    Toast.makeText(this, "Confirmación: Ración Servida", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothManager.detener()
    }
}