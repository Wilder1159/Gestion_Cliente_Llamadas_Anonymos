# Llamadas simultáneas con Telnyx

## Preparación

1. Ejecuta las migraciones necesarias manualmente; la aplicación no las
   ejecuta automáticamente. Para cambiar a cobro sin reservas, detén el
   servidor y ejecuta `migracion_sin_reservas_llamadas.sql` en
   `ClienteTelefoniaDB` para liberar reservas antiguas.
2. Conserva tu `.env` local y agrega las claves que aparecen en
   `.env.example`; no reemplaces credenciales existentes por los ejemplos.
3. Configura la URL de webhook HTTPS de Telnyx como
   `https://<dominio-publico>/api/telnyx/webhook` y habilita los eventos
   `call.initiated`, `call.answered` y `call.hangup`.
   Un túnel HTTPS puede exponer el puerto local 8081 durante pruebas.
4. Inicia `main.com.ClienteTelefonia.server.ServidorApplication` desde el IDE
   o como proceso servidor. Luego inicia la aplicación Swing e inicia sesión.
   El cliente de llamadas usa `http://localhost:8081` por defecto. Para un
   servidor distinto, configura la propiedad JVM `-Dllamadas.api.url=<URL>`.

La clave API de Telnyx se carga únicamente en el servidor. La pantalla Swing
requiere una sesión activa y solo envía destino y clave de idempotencia; no
acepta un identificador de cliente o cuenta.

## Operación

- Se pueden registrar llamadas concurrentes. Para iniciar cada llamada, el
  saldo actual debe cubrir su costo máximo posible; no se crea una reserva ni
  se descuenta saldo al iniciar.
- La llamada se limita a 30 segundos de timbrado y al máximo de duración
  configurado en la base. El cobro usa minuto iniciado, mínimo facturable y
  `BigDecimal` con redondeo `HALF_UP`.
- El saldo solo cambia al procesar el webhook final: se descuenta el costo
  calculado por el consumo real y se registra el movimiento en una transacción
  SQL. Si varias llamadas concurrentes consumen más que el saldo disponible,
  se cobra el saldo restante, se deja en cero y el faltante queda indicado en
  la descripción del movimiento; la llamada conserva su costo calculado.
- `VER / FINALIZAR` muestra el historial y permite pedir a Telnyx que finalice
  una llamada activa. La pantalla confirma la petición; el webhook confirma
  el cierre y el costo.
- La interfaz advierte que Telnyx puede generar cargos reales.

Si la respuesta de creación se pierde o queda ambigua, el servidor marca la
solicitud `INCIERTA` y no la reenvía para evitar una llamada duplicada. La
llamada no mantiene saldo reservado mientras se reconcilia con Telnyx.

## Endpoints

- `POST /api/llamadas`: autenticado; requiere `Idempotency-Key` UUID y JSON
  `{"destino":"+519XXXXXXXX"}`.
- `GET /api/llamadas`: devuelve el historial de la cuenta asociada a la sesión.
- `POST /api/llamadas/finalizar`: autenticado; JSON
  `{"idLlamada":123}`. La propiedad se comprueba en SQL Server.
- `POST /api/telnyx/webhook`: público, pero solo acepta firmas Ed25519 válidas
  y timestamps recientes; persiste eventos antes de responder.

## Validación pendiente de infraestructura

`mvn clean test` cubre cálculos, firma Ed25519 y el contrato HTTP del cliente.
No sustituye pruebas con SQL Server, ngrok/Telnyx ni una llamada real. Ejecuta
esas pruebas en un entorno de prueba antes de habilitar tráfico real; esta
implementación no marca esos escenarios como verificados.
