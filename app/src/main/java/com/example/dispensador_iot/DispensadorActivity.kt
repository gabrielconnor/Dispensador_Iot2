package com.example.dispensador_iot

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBtManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityDispenserBinding

class DispensadorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDispenserBinding
    private val handler = Handler(Looper.getMainLooper())
    private var nivelComida = 39
    private var mascotaPresente = true
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispenserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BluetoothManager.inicializar(this)
        val systemBtManager = getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBtManager
        bluetoothAdapter = systemBtManager.adapter

        BluetoothManager.onMensajeRecibido = { linea ->
            recibirComando(linea)
        }

        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            BluetoothManager.iniciarServidor(bluetoothAdapter!!)
        }

        binding.sbComida.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                nivelComida = progress
                binding.tvNivelComida.text = "Nivel de Comida: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.swPresencia.setOnCheckedChangeListener { _, isChecked ->
            mascotaPresente = isChecked
        }

        // Acciones del botón Dispensar Comida
        binding.btnDispensarManual.setOnClickListener {
            dispensarRacion()
        }

        // Acciones del botón Cancelar
        binding.btnCancelar.setOnClickListener {
            Toast.makeText(this, "Acción cancelada", Toast.LENGTH_SHORT).show()
        }

        // Acciones del botón Regresar
        binding.btnRegresar.setOnClickListener {
            BluetoothManager.detener()
            finish()
        }

        iniciarEnvioCada2Segundos()
    }

    private fun dispensarRacion() {
        // Cambiar estado visual de la compuerta
        binding.tvEstadoCompuerta.text = "Compuerta: ABIERTA"
        binding.tvEstadoCompuerta.setTextColor(getColor(android.R.color.holo_green_dark))

        Toast.makeText(this, "Dispensando ración de comida...", Toast.LENGTH_SHORT).show()

        // Responder confirmación ACK vía Bluetooth
        val ackMsg = ProtocoloIoT.crearRespuestaAck() // "ACK;dispensar;OK\n"
        BluetoothManager.enviarMensaje(ackMsg)

        // Temporizador para cerrar la compuerta automáticamente tras 3 segundos
        handler.postDelayed({
            binding.tvEstadoCompuerta.text = "Compuerta: CERRADA"
            binding.tvEstadoCompuerta.setTextColor(getColor(android.R.color.holo_red_dark))
        }, 3000)
    }

    private fun iniciarEnvioCada2Segundos() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val msgNivel = ProtocoloIoT.crearLecturaNivel(nivelComida)
                val msgPresencia = ProtocoloIoT.crearLecturaPresencia(mascotaPresente)

                BluetoothManager.enviarMensaje(msgNivel)
                BluetoothManager.enviarMensaje(msgPresencia)

                handler.postDelayed(this, 2000)
            }
        }, 2000)
    }

    fun recibirComando(mensaje: String) {
        val datos = ProtocoloIoT.validarYParsear(mensaje) ?: return
        val (tipo, clave, valor) = datos
        if (tipo == "CMD" && clave == "dispensar") {
            val cantidadADispensar = valor.toIntOrNull() ?: 15
            nivelComida = (nivelComida - cantidadADispensar).coerceAtLeast(0)

            binding.sbComida.progress = nivelComida
            binding.tvNivelComida.text = "Nivel de Comida: $nivelComida%"

            dispensarRacion()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothManager.detener()
    }
}