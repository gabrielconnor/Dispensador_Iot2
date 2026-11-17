package com.example.dispensador_iot

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityOwnerBinding

class DuenioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBinding
    private var cantidadRacion = 25

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Control deslizante para aumentar o disminuir la dispensación
        binding.sbCantidadDispensar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Evitamos que sea 0% fijando un mínimo de 10%
                cantidadRacion = if (progress < 10) 10 else progress
                binding.tvCantidadDispensar.text = "Cantidad a dispensar: $cantidadRacion%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 1. Botón Dispensar Ración con la cantidad elegida
        binding.btnDispensar.setOnClickListener {
            val cmd = "CMD;dispensar;$cantidadRacion\n"
            BluetoothManager.enviarMensaje(cmd)
            Toast.makeText(this, "Enviando orden: dispensar $cantidadRacion% de ración", Toast.LENGTH_SHORT).show()
        }

        // 2. Botón Cancelar Proceso (reinicia la selección a 25%)
        binding.btnCancelar.setOnClickListener {
            cantidadRacion = 25
            binding.sbCantidadDispensar.progress = 25
            binding.tvCantidadDispensar.text = "Cantidad a dispensar: 25%"
            Toast.makeText(this, "Proceso cancelado", Toast.LENGTH_SHORT).show()
        }

        // 3. Botón Regresar
        binding.btnRegresar.setOnClickListener {
            finish()
        }
    }

    fun procesarMensajeEntrante(mensaje: String) {
        val partes = mensaje.trim().split(";")
        if (partes.size == 3) {
            val tipo = partes[0]
            val clave = partes[1]
            val valor = partes[2]

            when (tipo) {
                "LECTURA" -> {
                    if (clave == "nivel") {
                        val nivel = valor.toIntOrNull() ?: 0
                        binding.tvComidaPanel.text = "Nivel: $nivel%"
                        binding.pbNivelComida.progress = nivel

                        if (nivel < 20) {
                            Toast.makeText(this, "¡ALERTA! Nivel de comida menor al 20%", Toast.LENGTH_LONG).show()
                        }
                    } else if (clave == "presencia") {
                        val presente = valor == "1"
                        binding.tvPresenciaPanel.text = if (presente) "Mascota: Presente" else "Mascota: Ausente"
                    }
                }
                "ACK" -> {
                    if (clave == "dispensar" && valor == "OK") {
                        Toast.makeText(this, "Confirmación: Ración Servida Exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}