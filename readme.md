# Sistema IoT de Dispensador de Alimentos para Mascotas

Aplicación móvil nativa en **Android (Kotlin)** conectada mediante **Bluetooth Clásico (SPP / RFCOMM)** a un microcontrolador **ESP32** (o a un segundo dispositivo Android en modo simulador) para el monitoreo de nivel de tolva, detección de presencia y dispensación remota de raciones.

---

## 1. Arquitectura y Protocolo de Comunicación

Las tramas viajan delimitadas por salto de línea (`\n`) con validación de integridad **CRC-8**:
`TIPO;CLAVE;VALOR;CRC`

* `LECTURA;nivel;60;A4` → Nivel actual de alimento en la tolva (0 a 100%).
* `LECTURA;presencia;1;3F` → Detección de mascota frente al plato (`1` = Presente, `0` = Ausente).
* `CMD;dispensar;25;8B` → Orden del dueño para dispensar un porcentaje específico de ración.
* `ACK;dispensar;OK;1C` → Confirmación de apertura y cierre seguro de compuerta.

---

## 2. Consumo de Energía Estimado (Criterio 7)

El firmware implementa `esp_light_sleep_start()` entre ciclos de telemetría para reducir el consumo del radio y CPU:

| Componente | Estado Activo | Estado Light Sleep | Observación |
| :--- | :---: | :---: | :--- |
| **ESP32-WROOM-32 (BT Clásico activo)** | ~130 mA | ~15 mA | Reducción del ~88% en reposo entre lecturas |
| **Sensor Ultrasónico HC-SR04** | ~15 mA | ~2 mA | Pulso de disparo de 10 µs cada 2 segundos |
| **Sensor Presencia IR / PIR** | ~5 mA | ~0.05 mA | Consumo continuo ultra bajo |
| **Servomotor SG90 (Compuerta)** | ~250 mA (pico) | ~5 mA (idle) | Solo consume durante los 1–3 s de dispensación |
| **Total Estimado del Sistema** | **~150 mA** (400 mA pico) | **~22 mA** | Autonomía superior a 90 horas con batería LiPo de 2000 mAh |

---

## 3. Plan de Contingencia ante Pérdida de Conexión (Criterio 8)

* **En el Microcontrolador (ESP32):**
    1. **Seguridad física:** La compuerta permanece cerrada por defecto (`0°`) ante cualquier corte de energía o enlace.
    2. **Buffer circular:** Almacena hasta las últimas **20 lecturas** en RAM y las sincroniza automáticamente al restablecerse el socket Bluetooth.
    3. **Modo autónomo:** Si transcurren más de **6 horas** sin cliente conectado y el nivel de tolva supera el 10%, el ESP32 ejecuta una dispensación preventiva automática del 25%.
* **En la App Móvil (Panel Dueño):**
    1. El indicador superior cambia inmediatamente a **Estado: RECONECTANDO...** (naranja) y activa un algoritmo de **backoff exponencial** (`1s, 2s, 4s, 8s, 16s`, máximo `30s`) sin cerrar ni congelar la aplicación.
    2. Las órdenes enviadas sin enlace se guardan en una cola persistente local (`SharedPreferences`) y se transmiten apenas vuelve la conexión.

---

## 4. Compatibilidad y Pruebas Multi-versión (Criterio 9)

La aplicación soporta desde **Android 7.0 (`minSdk = 24`)** hasta **Android 15+ (`targetSdk = 34/37`)**:
* **Android 12+ (API 31+):** Solicita dinámicamente en tiempo de ejecución `BLUETOOTH_CONNECT` y `BLUETOOTH_SCAN`.
* **Android 7.0 a 11 (API 24–30):** Solicita `ACCESS_FINE_LOCATION` y emplea `BLUETOOTH` / `BLUETOOTH_ADMIN`.
* **Dispositivos probados:** Emulador Pixel 7 (API 34/37) y dispositivo físico Android (API 29 / API 33).

---

## 5. Instrucciones de Compilación y Carga

### Compilar la App Android
1. Clonar el repositorio: `git clone https://github.com/gabrielconnor/Dispensador_Iot2.git`
2. Abrir el proyecto en **Android Studio Ladybug / Koala** o superior.
3. Colocar el archivo `google-services.json` en `app` para habilitar Firebase Authentication.
4. Ejecutar **Build > Rebuild Project** y desplegar con **Run 'app'**.

### Cargar el Firmware en el ESP32
1. Abrir `firmware/dispensador_esp32/dispensador_esp32.ino` en **Arduino IDE 2.x**.
2. Instalar la placa **ESP32 by Espressif Systems** y la librería **ESP32Servo**.
3. Conectar sensores según el mapa de pines (`TRIG=5`, `ECHO=18`, `PRESENCIA=19`, `SERVO=13`).
4. Seleccionar la placa **DOIT ESP32 DEVKIT V1** y presionar **Subir (Upload)**.
5. Emparejar el teléfono con el dispositivo Bluetooth **`Dispensador_IoT_ESP32`** antes de abrir el Panel del Dueño.

---

## 6. Capturas de Funcionamiento

*(Agrega aquí las capturas de pantalla de tu carpeta `docs/capturas/`: Login Firebase, Panel Dueño Conectado y Monitor Serial del ESP32)*