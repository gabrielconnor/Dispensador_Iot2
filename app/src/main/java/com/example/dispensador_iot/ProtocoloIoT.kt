package com.example.dispensador_iot

object ProtocoloIoT {

    fun calcularCRC(datos: String): String {
        var crc = 0
        for (b in datos.toByteArray(Charsets.UTF_8)) {
            crc = (crc xor (b.toInt() and 0xFF)) and 0xFF
            for (i in 0 until 8) {
                crc = if ((crc and 0x80) != 0) {
                    ((crc shl 1) xor 0x07) and 0xFF
                } else {
                    (crc shl 1) and 0xFF
                }
            }
        }
        return String.format("%02X", crc)
    }

    fun empaquetar(tipo: String, clave: String, valor: String): String {
        val payload = "$tipo;$clave;$valor"
        val crc = calcularCRC(payload)
        return "$payload;$crc\n"
    }

    fun crearLecturaNivel(nivel: Int): String = empaquetar("LECTURA", "nivel", nivel.toString())
    fun crearLecturaPresencia(presente: Boolean): String = empaquetar("LECTURA", "presencia", if (presente) "1" else "0")
    fun crearComandoDispensar(cantidad: Int = 25): String = empaquetar("CMD", "dispensar", cantidad.toString())
    fun crearRespuestaAck(): String = empaquetar("ACK", "dispensar", "OK")

    /**
     * Valida estructura TIPO;CLAVE;VALOR;CRC y comprueba integridad.
     * Retorna Triple(tipo, clave, valor) o null si está corrupto.
     */
    fun validarYParsear(linea: String): Triple<String, String, String>? {
        val partes = linea.trim().split(";")
        if (partes.size != 4) return null
        val payload = "${partes[0]};${partes[1]};${partes[2]}"
        val crcRecibido = partes[3].uppercase()
        if (calcularCRC(payload) != crcRecibido) return null
        return Triple(partes[0], partes[1], partes[2])
    }
}