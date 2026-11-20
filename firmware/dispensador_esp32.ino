#include "BluetoothSerial.h"
#include <ESP32Servo.h>
#include "esp_sleep.h"

BluetoothSerial SerialBT;
Servo servoCompuerta;

// Pines Hardware ESP32
const int PIN_TRIG = 5;
const int PIN_ECHO = 18;
const int PIN_PRESENCIA = 19; // Sensor IR / PIR (HIGH = mascota presente)
const int PIN_SERVO = 13;

// Buffer Circular para lecturas offline (Criterio 6)
const int MAX_BUFFER = 20;
String bufferCircular[MAX_BUFFER];
int bufHead = 0;
int bufCount = 0;

// Variables de tiempo y contingencia (Criterio 8)
unsigned long ultimoEnvioMs = 0;
unsigned long ultimaRacionContingenciaMs = 0;
const unsigned long INTERVALO_LECTURA_MS = 2000;
const unsigned long INTERVALO_CONTINGENCIA_MS = 21600000; // 6 horas sin conexión

// Cálculo CRC-8 idéntico al de Kotlin (Criterio 4)
String calcularCRC(String datos) {
    uint8_t crc = 0;
    for (unsigned int i = 0; i < datos.length(); i++) {
        crc ^= (uint8_t)datos[i];
        for (int j = 0; j < 8; j++) {
            if (crc & 0x80) crc = (crc << 1) ^ 0x07;
            else crc <<= 1;
        }
    }
    char hexStr[3];
    sprintf(hexStr, "%02X", crc);
    return String(hexStr);
}

String empaquetarMensaje(String tipo, String clave, String valor) {
    String payload = tipo + ";" + clave + ";" + valor;
    return payload + ";" + calcularCRC(payload);
}

bool validarMensaje(String linea, String &tipo, String &clave, String &valor) {
    linea.trim();
    int p1 = linea.indexOf(';');
    int p2 = linea.indexOf(';', p1 + 1);
    int p3 = linea.indexOf(';', p2 + 1);
    if (p1 == -1 || p2 == -1 || p3 == -1) return false;

    tipo = linea.substring(0, p1);
    clave = linea.substring(p1 + 1, p2);
    valor = linea.substring(p2 + 1, p3);
    String crcRecibido = linea.substring(p3 + 1);
    crcRecibido.toUpperCase();

    String payload = tipo + ";" + clave + ";" + valor;
    return calcularCRC(payload) == crcRecibido;
}

void guardarEnBufferCircular(String trama) {
    bufferCircular[bufHead] = trama;
    bufHead = (bufHead + 1) % MAX_BUFFER;
    if (bufCount < MAX_BUFFER) bufCount++;
    Serial.println("[BUFFER OFFLINE] Guardado: " + trama);
}

void vaciarBufferCircular() {
    if (bufCount == 0 || !SerialBT.hasClient()) return;
    int startIdx = (bufHead - bufCount + MAX_BUFFER) % MAX_BUFFER;
    for (int i = 0; i < bufCount; i++) {
        int idx = (startIdx + i) % MAX_BUFFER;
        SerialBT.println(bufferCircular[idx]);
        Serial.println("[BUFFER SYNC] Enviado: " + bufferCircular[idx]);
    }
    bufCount = 0;
    bufHead = 0;
}

void enviarOEncolar(String trama) {
    if (SerialBT.hasClient()) {
        vaciarBufferCircular();
        SerialBT.println(trama);
        Serial.println("[BT TX] " + trama);
    } else {
        guardarEnBufferCircular(trama);
    }
}

int leerNivelPorcentaje() {
    digitalWrite(PIN_TRIG, LOW);
    delayMicroseconds(2);
    digitalWrite(PIN_TRIG, HIGH);
    delayMicroseconds(10);
    digitalWrite(PIN_TRIG, LOW);

    long duracion = pulseIn(PIN_ECHO, HIGH, 25000);
    if (duracion == 0) return 50; // Valor seguro si no hay eco
    float distanciaCm = duracion * 0.034 / 2.0;
    int pct = map((int)constrain(distanciaCm, 2, 20), 20, 2, 0, 100);
    return pct;
}

void accionarCompuerta(int porcentajeRacion) {
    Serial.println("[SERVO] Abriendo compuerta por racion: " + String(porcentajeRacion) + "%");
    servoCompuerta.write(90); // Abrir
    int tiempoAperturaMs = constrain(porcentajeRacion * 30, 500, 3000);
    delay(tiempoAperturaMs);
    servoCompuerta.write(0);  // Cerrar seguro
    Serial.println("[SERVO] Compuerta CERRADA");
}

void setup() {
    Serial.begin(115200);
    pinMode(PIN_TRIG, OUTPUT);
    pinMode(PIN_ECHO, INPUT);
    pinMode(PIN_PRESENCIA, INPUT);

    servoCompuerta.attach(PIN_SERVO);
    servoCompuerta.write(0); // Asegurar cerrada al iniciar

    SerialBT.begin("Dispensador_IoT_ESP32");
    Serial.println("=== Dispensador IoT ESP32 Iniciado ===");
}

void loop() {
    if (SerialBT.available()) {
        String linea = SerialBT.readStringUntil('\n');
        Serial.println("[BT RX] " + linea);
        String tipo, clave, valor;
        if (validarMensaje(linea, tipo, clave, valor)) {
            if (tipo == "CMD" && clave == "dispensar") {
                int racion = valor.toInt();
                if (racion <= 0) racion = 25;
                accionarCompuerta(racion);
                enviarOEncolar(empaquetarMensaje("ACK", "dispensar", "OK"));
            }
        } else {
            Serial.println("[ERROR CRC] Mensaje corrupto descartado.");
        }
    }

    unsigned long ahora = millis();

    if (!SerialBT.hasClient() && (ahora - ultimaRacionContingenciaMs >= INTERVALO_CONTINGENCIA_MS)) {
        int nivelActual = leerNivelPorcentaje();
        if (nivelActual > 10) {
            Serial.println("[CONTINGENCIA] Dispensando racion programada sin conexion.");
            accionarCompuerta(25);
            ultimaRacionContingenciaMs = ahora;
        }
    }

    if (ahora - ultimoEnvioMs >= INTERVALO_LECTURA_MS) {
        ultimoEnvioMs = ahora;
        int nivel = leerNivelPorcentaje();
        int presencia = digitalRead(PIN_PRESENCIA) == HIGH ? 1 : 0;

        enviarOEncolar(empaquetarMensaje("LECTURA", "nivel", String(nivel)));
        enviarOEncolar(empaquetarMensaje("LECTURA", "presencia", String(presencia)));

        esp_sleep_enable_timer_wakeup(200000);
        esp_light_sleep_start();
    }
}
