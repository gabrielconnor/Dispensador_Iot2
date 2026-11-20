package com.example.dispensador_iot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.min

object BluetoothManager {

    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    enum class EstadoConexion { CONECTADO, DESCONECTADO, RECONECTANDO }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activo = false
    private var modoServidor = false
    private var macDestino: String? = null
    private var adapterRef: BluetoothAdapter? = null
    private var appContext: Context? = null
    private var backoffMs = 1000L

    var onEstadoCambiado: ((EstadoConexion) -> Unit)? = null
    var onMensajeRecibido: ((String) -> Unit)? = null

    fun inicializar(context: Context) {
        appContext = context.applicationContext
    }

    private fun notificarEstado(estado: EstadoConexion) {
        mainHandler.post { onEstadoCambiado?.invoke(estado) }
    }

    // MODO SERVIDOR (Dispensador / Teléfono mascota)
    @SuppressLint("MissingPermission")
    fun iniciarServidor(adapter: BluetoothAdapter) {
        adapterRef = adapter
        modoServidor = true
        activo = true
        backoffMs = 1000L
        ejecutarCicloServidor()
    }

    @SuppressLint("MissingPermission")
    private fun ejecutarCicloServidor() {
        if (!activo) return
        notificarEstado(EstadoConexion.RECONECTANDO)
        thread {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = adapterRef?.listenUsingRfcommWithServiceRecord("DispensadorIoT", SPP_UUID)
                val accepted = serverSocket?.accept() // Bloquea hasta que se conecte el cliente
                serverSocket?.close()
                if (accepted != null) {
                    socket = accepted
                    outputStream = accepted.outputStream
                    backoffMs = 1000L
                    notificarEstado(EstadoConexion.CONECTADO)
                    vaciarColaPendiente()
                    leerLineas(accepted)
                }
            } catch (e: IOException) {
                try { serverSocket?.close() } catch (_: IOException) {}
                programarReconexion()
            }
        }
    }

    // MODO CLIENTE (Dueño -> se conecta al ESP32 o al teléfono Dispensador)
    @SuppressLint("MissingPermission")
    fun conectarComoCliente(adapter: BluetoothAdapter, direccionMAC: String) {
        adapterRef = adapter
        macDestino = direccionMAC
        modoServidor = false
        activo = true
        backoffMs = 1000L
        ejecutarCicloCliente()
    }

    @SuppressLint("MissingPermission")
    private fun ejecutarCicloCliente() {
        if (!activo) return
        val mac = macDestino ?: return
        notificarEstado(EstadoConexion.RECONECTANDO)
        thread {
            try {
                val device = adapterRef?.getRemoteDevice(mac)
                val clientSocket = device?.createRfcommSocketToServiceRecord(SPP_UUID)
                adapterRef?.cancelDiscovery()
                clientSocket?.connect()
                if (clientSocket != null) {
                    socket = clientSocket
                    outputStream = clientSocket.outputStream
                    backoffMs = 1000L
                    notificarEstado(EstadoConexion.CONECTADO)
                    vaciarColaPendiente()
                    leerLineas(clientSocket)
                }
            } catch (e: IOException) {
                cerrarSocketSilencioso()
                programarReconexion()
            }
        }
    }

    // Lectura segura separada por '\n'
    private fun leerLineas(activeSocket: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(activeSocket.inputStream, Charsets.UTF_8))
            while (activo && activeSocket.isConnected) {
                val linea = reader.readLine() ?: break
                if (linea.isNotBlank()) {
                    mainHandler.post { onMensajeRecibido?.invoke(linea.trim()) }
                }
            }
        } catch (_: IOException) {
        } finally {
            cerrarSocketSilencioso()
            programarReconexion()
        }
    }

    // Backoff exponencial: 1s, 2s, 4s, 8s, 16s, máx 30s
    private fun programarReconexion() {
        if (!activo) {
            notificarEstado(EstadoConexion.DESCONECTADO)
            return
        }
        notificarEstado(EstadoConexion.RECONECTANDO)
        val esperaActual = backoffMs
        backoffMs = min(backoffMs * 2, 30_000L)

        mainHandler.postDelayed({
            if (!activo) return@postDelayed
            if (modoServidor) ejecutarCicloServidor() else ejecutarCicloCliente()
        }, esperaActual)
    }

    // Envía si hay conexión; si está caído, guarda en almacenamiento temporal
    fun enviarMensaje(mensaje: String) {
        thread {
            val out = outputStream
            if (socket?.isConnected == true && out != null) {
                try {
                    out.write(mensaje.toByteArray(Charsets.UTF_8))
                    out.flush()
                } catch (e: IOException) {
                    encolarOffline(mensaje)
                    cerrarSocketSilencioso()
                    programarReconexion()
                }
            } else {
                encolarOffline(mensaje)
            }
        }
    }

    // Cola persistente offline en SharedPreferences
    @Synchronized
    private fun encolarOffline(mensaje: String) {
        val prefs = appContext?.getSharedPreferences("ColaOfflineIoT", Context.MODE_PRIVATE) ?: return
        val arr = JSONArray(prefs.getString("pendientes", "[]"))
        if (arr.length() < 50) { // Límite de seguridad
            arr.put(mensaje)
            prefs.edit().putString("pendientes", arr.toString()).apply()
        }
    }

    @Synchronized
    private fun vaciarColaPendiente() {
        val prefs = appContext?.getSharedPreferences("ColaOfflineIoT", Context.MODE_PRIVATE) ?: return
        val arr = JSONArray(prefs.getString("pendientes", "[]"))
        if (arr.length() == 0) return

        val out = outputStream ?: return
        try {
            for (i in 0 until arr.length()) {
                out.write(arr.getString(i).toByteArray(Charsets.UTF_8))
            }
            out.flush()
            prefs.edit().putString("pendientes", "[]").apply()
        } catch (_: IOException) {
            // Si falla a mitad de camino, se reintentará en la siguiente reconexión
        }
    }

    private fun cerrarSocketSilencioso() {
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        outputStream = null
        socket = null
    }

    fun detener() {
        activo = false
        cerrarSocketSilencioso()
        notificarEstado(EstadoConexion.DESCONECTADO)
    }
}