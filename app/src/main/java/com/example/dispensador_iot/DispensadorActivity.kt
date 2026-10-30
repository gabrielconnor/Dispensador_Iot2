package com.example.dispensador_iot
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityDispenserBinding

class DispensadorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDispenserBinding
    private val handler = Handler(Looper.getMainLooper())
    private var nivelComida = 50
    private var mascotaPresente = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDispenserBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        iniciarEnvioCada2Segundos()
    }

    private fun iniciarEnvioCada2Segundos() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val msgNivel = ProtocoloIoT.crearLecturaNivel(nivelComida)
                val msgPresencia = ProtocoloIoT.crearLecturaPresencia(mascotaPresente)

                println("Enviando vía Bluetooth: $msgNivel")
                println("Enviando vía Bluetooth: $msgPresencia")

                handler.postDelayed(this, 2000)
            }
        }, 2000)
    }

    fun recibirComando(mensaje: String) {
        if (mensaje.trim() == "CMD;dispensar;START") {
            binding.tvEstadoCompuerta.text = "Compuerta: ABIERTA"
            val ack = ProtocoloIoT.crearRespuestaAck()
            println("Respondiendo vía Bluetooth: $ack")
        }
    }
}