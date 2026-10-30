package com.example.dispensador_iot

object ProtocoloIoT {
    fun crearLecturaNivel(nivel: Int): String = "LECTURA;nivel;$nivel\n"
    fun crearLecturaPresencia(presente: Boolean): String = "LECTURA;presencia;${if (presente) 1 else 0}\n"
    fun crearComandoDispensar(): String = "CMD;dispensar;START\n"
    fun crearRespuestaAck(): String = "ACK;dispensar;OK\n"
}