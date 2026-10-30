package com.example.dispensador_iot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

object BluetoothManager {

    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Servidor (Modo Dispensador)
    @SuppressLint("MissingPermission")
    fun iniciarServidor(adapter: BluetoothAdapter, onMensajeRecibido: (String) -> Unit) {
        thread {
            try {
                val serverSocket: BluetoothServerSocket? =
                    adapter.listenUsingRfcommWithServiceRecord("DispensadorIoT", SPP_UUID)
                println("Servidor Bluetooth en escucha...")
                bluetoothSocket = serverSocket?.accept()
                serverSocket?.close() // Ya se aceptó la conexión
                escucharEntrada(onMensajeRecibido)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Cliente (Modo Dueño)
    @SuppressLint("MissingPermission")
    fun conectarComoCliente(adapter: BluetoothAdapter, direccionMAC: String, onMensajeRecibido: (String) -> Unit) {
        thread {
            try {
                val device = adapter.getRemoteDevice(direccionMAC)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter.cancelDiscovery()
                bluetoothSocket?.connect()
                escucharEntrada(onMensajeRecibido)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun escucharEntrada(onMensajeRecibido: (String) -> Unit) {
        try {
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val mensaje = String(buffer, 0, bytes)
                    onMensajeRecibido(mensaje)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun enviarMensaje(mensaje: String) {
        thread {
            try {
                outputStream?.write(mensaje.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}