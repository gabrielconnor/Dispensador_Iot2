package com.example.dispensador_iot

import android.os.Handler
import android.os.Looper

object BluetoothSimulador {

    private var oyenteMensajes: ((String) -> Unit)? = null

    // Registrar la pantalla que escuchará los mensajes
    fun suscribirLlegadaMensajes(listener: (String) -> Unit) {
        this.oyenteMensajes = listener
    }

    // Simular el envío del mensaje a través de un canal virtual
    fun enviarMensajeSimulado(mensaje: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            println("Simulador Transmitiendo: $mensaje")
            oyenteMensajes?.invoke(mensaje)
        }, 300) // Retardo simula la latencia de red de 300ms
    }
}