package com.example.dispensador_iot

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dispensador_iot.databinding.ActivityOwnerBinding

class DuenioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Botón Dispensar Ración
        binding.btnDispensar.setOnClickListener {
            val cmd = ProtocoloIoT.crearComandoDispensar() // "CMD;dispensar;START\n"
            BluetoothManager.enviarMensaje(cmd)
            Toast.makeText(this, "Enviando orden de dispensado...", Toast.LENGTH_SHORT).show()
        }

        // 2. Botón Cancelar Proceso
        binding.btnCancelar.setOnClickListener {
            Toast.makeText(this, "Proceso cancelado por el usuario", Toast.LENGTH_SHORT).show()
        }

        // 3. Botón Regresar
        binding.btnRegresar.setOnClickListener {
            finish()
        }
    }

    // Método para procesar lecturas entrantes por Bluetooth
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

                        // Actualiza el texto y la barra horizontal de la ración
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