# Recover v2.0.6
La versión estable de Recover, que usan todos los usuarios actualmente.

- ⚠️ Credenciales no incluídas.
- ⚠️ Las credenciales deben ser las de [Engine](https://engine.unklatam.com/).
- ⚠️ La URL se compone de "http://IP:PORT", en formato String.
- ⚠️ El usuario y contraseña deben ser en formato String, y es el usuario Tenant de Recover.

## ¿Qué es Recover?
- Recover es una versión extendida de [Scan](https://github.com/UNKDevTeam/Scan-stable), la cuál tiene más funciones basado en la búsqueda de datos para loggers MST03 y sensores MST01.
- Cuenta con la posibilidad de conectarse a un logger/sensor, ir por datos según rango de fecha/hora, y generar un reporte.

## ¿Cómo funciona Recover?
- Inicializa con una lista de los sensores posterior a un escaneo continuo, a lo cuál el usuario debe seleccionar dicho sensor para conectarse.
  - Conexión con QR en progreso, no está disponible para Stable.
- Una vez conectado al sensor, se hace una selección de los días y horas de inicio y fin, a lo cuál la aplicación irá a consultar al logger/sensor por datos entre las fechas indicadas.
  - Estas fechas parten en formato Calendar, para después pasarse a timestamp en segundos.
- Finaliza con un reporte de los datos obtenidos, que se compone de un gráfico, una tabla con las T° Mínima, Máxima, Promedio, y la fecha y hora de inicio y fin.
  - Adicionalmente están las funciones de:
    - Descargar Reporte (en formato de imagen) ❌
    - Descargar Excel ✅
      - El Excel se compone de la MAC, el timestamp, la temperatura y la humedad.
    - Subir Datos (a Thingsboard) ✅
      - La función se demora porque no existe un login para diferenciar entre Customers usando la aplicación.
      - Un login está en progreso, no está disponible para Stable.
