# Informe de cambios: recargas simuladas

## Alcance

Se actualizaron las recargas Yape y tarjeta para que sean operaciones
académicas simuladas, manteniendo Maven, Java 21, Swing y JDBC/SQL Server.
No se integraron pasarelas ni se añadió un endpoint de recargas.

## Archivos modificados

- `src/main/java/main/com/ClienteTelefonia/dao/ClienteDAO.java`
  - Se conservaron las firmas públicas existentes que reciben `double`.
  - Se añadieron variantes con `BigDecimal` y clave UUID de idempotencia.
  - Se centralizó el registro en una transacción SQL que bloquea la clave y la
    cuenta, registra recarga y movimiento, actualiza el saldo y confirma todo
    junto; cualquier fallo intenta revertir la transacción.
- `src/main/java/main/com/ClienteTelefonia/controller/ClienteTelefoniaController.java`
  - Mantiene las firmas previas y añade variantes `BigDecimal`/UUID.
  - Actualiza el saldo en memoria solo después de recibir confirmación del DAO.
  - Añade control del cliente autenticado y evita que un rol CLIENTE seleccione
    o recargue la cuenta de otra persona.
- `src/main/java/main/com/ClienteTelefonia/view/JLogin.java`
  - Crea el controlador de recargas con el cliente y rol obtenidos tras el
    inicio de sesión.
- `src/main/java/main/com/ClienteTelefonia/view/JRecargaEfectivo.java` y
  `JRecargaEfectivo.form`
  - Yape ya no carga ni muestra un QR.
  - La pantalla informa que no se debe realizar ni verificar una transferencia
    real y marca el resultado como simulado.
- `src/main/java/main/com/ClienteTelefonia/view/JRecargaTarjeta.java` y
  `JRecargaTarjeta.form`
  - Se eliminaron los campos de número de tarjeta, vencimiento y CVV.
  - La pantalla identifica expresamente la simulación y muestra bruto,
    comisión e importe acreditado.

## Archivos nuevos

- `src/main/java/main/com/ClienteTelefonia/service/CalculoRecargaSimulada.java`
  - Cálculo monetario común con `BigDecimal` y `RoundingMode.HALF_UP`.
- `src/test/java/main/com/ClienteTelefonia/service/CalculoRecargaSimuladaTest.java`
  - Pruebas de importes, comisión, redondeo y límites de entrada.
- `src/test/java/main/com/ClienteTelefonia/controller/ClienteTelefoniaControllerTest.java`
  - Prueba que un cliente no puede seleccionar la cuenta de otro.
- `migracion_recargas_simuladas.sql`
  - Migración aditiva e idempotente para trazabilidad financiera y referencia
    única.
- `INFORME_CAMBIOS.md`

## Decisiones financieras y SQL

- PEN; importe estrictamente positivo, redondeado a dos decimales con HALF_UP.
- Yape acredita el importe completo. Tarjeta registra importe bruto, comisión
  del 2 % e importe neto acreditado.
- La tabla `Recarga` conserva `monto` como bruto y añade `monto_comision`,
  `monto_acreditado` y `es_simulada`. El movimiento registra el importe
  acreditado y los saldos anterior/posterior.
- La referencia persistida `SIMULADA:<UUID>` identifica la operación.
  `UPDLOCK`/`HOLDLOCK` sobre el índice único de referencias serializa
  solicitudes con la misma clave, incluso mientras aún no existe una fila.
  Una repetición idéntica devuelve el saldo actual sin volver a acreditar.
  Reutilizar la referencia con otros datos se rechaza. La restricción única
  de referencia queda como defensa adicional.
- La cuenta se lee con `UPDLOCK`; cliente y cuenta deben estar activos y el
  nuevo saldo debe caber en `DECIMAL(12,2)`.
- La migración no elimina datos ni objetos. Las recargas históricas quedan como
  no simuladas; cuando existe un movimiento asociado, se usa para completar
  importe acreditado y comisión. La migración comprueba referencias duplicadas
  y revierte sus cambios ante errores.
- No se modificó `ClienteTelefonia` ni `pom.xml`.

## Pruebas ejecutadas

- `mvn clean compile` — **BUILD SUCCESS** con Java 21.
- `mvn test` — **BUILD SUCCESS**, 28 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Casos automatizados: Yape S/ 10.00 acredita S/ 10.00; tarjeta S/ 10.00
  calcula S/ 0.20 de comisión y acredita S/ 9.80; redondeo HALF_UP; importes
  cero, negativos y que redondean a cero rechazados; selección de cuenta ajena
  por un CLIENTE rechazada.
- Los formularios `.form` se comprobaron como XML válido y se verificó la
  ausencia de componentes de entrada de datos de tarjeta.

## Pruebas pendientes y problemas

El usuario compartió el resultado de una ejecución exitosa de
`migracion_recargas_simuladas.sql`, incluida la verificación de las columnas e
índice de referencia. No se conectó SQL Server desde esta sesión ni se
verificaron independientemente esos cambios. Las siguientes pruebas de
persistencia tampoco se ejecutaron aquí:

1. Rollback al fallar la inserción del movimiento.
2. Dos solicitudes simultáneas con la misma clave y conciliación saldo/libro.
3. Rechazo de cuenta bloqueada o cliente inactivo en la base.
4. Persistencia efectiva, repetición de una recarga ya confirmada y saldo
   posterior leído de SQL Server.

La prueba del controlador cubre la selección de cuenta ajena; no sustituye una
prueba HTTP. Actualmente no hay endpoint HTTP de recargas en el servidor.
Cualquier endpoint futuro debe autorizar con la sesión validada del servidor
(rol y propiedad), sin aceptar el `id_cliente` del cuerpo como identidad.

Para recargas, el usuario informó que la migración ya se ejecutó. La migración
de llamadas sigue pendiente de revisión y ejecución manual en la base correcta
de SQL Server antes de habilitar llamadas simultáneas.


# Ampliación: llamadas simultáneas con Telnyx

## Alcance implementado

Se trasladó la creación de llamadas de Swing al servidor HTTP existente.
Swing ya no crea solicitudes Telnyx directamente ni recibe la API key. La
cuenta se obtiene de la sesión validada y las operaciones financieras se
confirman exclusivamente en SQL Server.

## Archivos de llamadas modificados y nuevos

- `src/main/java/main/com/ClienteTelefonia/dao/LlamadaDAO.java`
  - Crea solicitudes idempotentes bajo transacción y bloqueo de la cuenta,
    verificando que el saldo actual cubra el costo máximo de la llamada.
  - Al procesar el webhook finaliza la llamada, actualiza saldo, libera
    reservas heredadas si las hubiera e inserta el movimiento en una
    transacción.
  - Deduplica y procesa eventos Telnyx persistidos; reintenta errores
    transitorios de procesamiento con límite.
- `src/main/java/main/com/ClienteTelefonia/service/LlamadaService.java`
  - Calcula el costo `BigDecimal` por minuto iniciado, con mínimo, máximo y
    redondeo `HALF_UP`; limita el cobro al saldo disponible.
- `src/main/java/main/com/ClienteTelefonia/service/LlamadaManager.java`
  - Limita las operaciones externas a un pool acotado y procesa los webhooks
    fuera de los hilos HTTP. Las solicitudes con resultado ambiguo no se
    reenvían.
- `src/main/java/main/com/ClienteTelefonia/service/TelnyxLlamadaService.java`
  - Crea y finaliza llamadas Telnyx desde el servidor; envía un `client_state`
    correlacionable y limita el timbrado a 30 segundos.
- `src/main/java/main/com/ClienteTelefonia/service/LlamadaApiClient.java`
  - Cliente HTTP sin credenciales Telnyx; mantiene la misma clave UUID si
    el usuario reintenta una solicitud cuya respuesta fue incierta y rechaza
    campos financieros o estados incompletos recibidos del servidor.
- `src/main/java/main/com/ClienteTelefonia/model/LlamadaRegistro.java`
  - Modelo de respuesta del historial.
- `src/main/java/main/com/ClienteTelefonia/server/controller/LlamadaController.java`
  - Endpoints autenticados de inicio, historial propio y finalización.
- `src/main/java/main/com/ClienteTelefonia/server/controller/TelnyxWebhookController.java`
  - Verifica firma antes de persistir eventos y limita el tamaño del cuerpo.
- `src/main/java/main/com/ClienteTelefonia/server/security/TelnyxWebhookVerifier.java`
  - Verifica Ed25519 sobre timestamp y cuerpo original y rechaza timestamps
    fuera de la ventana de cinco minutos.
- `src/main/java/main/com/ClienteTelefonia/server/ServidorApplication.java`
  - Registra rutas protegidas de llamadas y el webhook público firmado.
- `src/main/java/main/com/ClienteTelefonia/controller/ClienteTelefoniaController.java`,
  `src/main/java/main/com/ClienteTelefonia/view/JLogin.java` y
  `src/main/java/main/com/ClienteTelefonia/view/MenuOpciones.java`
  - Crea un token persistente al iniciar sesión y lo revoca al cerrar sesión.
- `src/main/java/main/com/ClienteTelefonia/view/JConsumo.java` y
  `JConsumo.form`
  - Consume la API; permite registrar varias llamadas desde la pantalla y
    consultar/finalizar llamadas activas.
- `migracion_llamadas_concurrentes.sql`
  - Añade clave de idempotencia, snapshots, estados de envío y tabla de eventos
    sin borrar datos ni modificar saldos.
- `GUIA_LLAMADAS.md` y `.env.example`
  - Documentación de instalación, webhook y configuración con marcadores sin
    secretos.

## Decisiones y límites

- Tarifa, moneda PEN, duración máxima, cuenta y saldo proceden de SQL Server.
  No se crean reservas; antes de iniciar una llamada, el saldo actual debe
  cubrir el costo máximo posible de esa llamada.
- Las llamadas se liquidan al recibir el evento final. Se cobra el costo real
  hasta el saldo disponible y, si varias llamadas concurrentes lo exceden, el
  faltante no se cobra y queda descrito en el movimiento. Saldo, movimiento y
  estado final se actualizan en una transacción; la idempotencia del evento y
  la transición terminal evitan doble consumo.
- Las claves de idempotencia quedan asociadas a cliente y destino. Reutilizar
  una clave para otra solicitud se rechaza.
- Eventos duplicados se deduplican por el ID de evento Telnyx. El payload
  original no se almacena; solo se conservan campos necesarios para procesar y
  auditar.
- Si la creación Telnyx tiene resultado ambiguo, la operación pasa a
  `INCIERTA` y no se reenvía. No queda saldo reservado durante la conciliación
  manual con Telnyx.
- El proyecto no inicia Telnyx ni SQL Server durante la compilación. No se
  ejecutó la migración ni se hicieron llamadas reales.

## Pruebas actuales

- `mvn clean compile` — resultado de la última ejecución: **BUILD SUCCESS**.
- `mvn test` — **BUILD SUCCESS** con Java 21; 42 pruebas, 0 fallos,
  0 errores y 0 omitidas.
- Pruebas: redondeo/minuto iniciado/límite y cobro limitado al saldo;
  firma Ed25519, cuerpo
  alterado y timestamp vencido; ciclo HTTP del cliente con varias llamadas,
  reintento idempotente, rechazo de autenticación y datos financieros
  malformados.
- `JConsumo.form` — XML válido.
- `git diff --check` — sin errores de whitespace.
- No verificados: startup con la base del usuario, transacciones y bloqueos en
  SQL Server real, recepción desde Telnyx/ngrok, llamadas/cargos reales,
  reintentos de callbacks en infraestructura, ni rollback de movimiento contra
  SQL Server.
- El usuario reportó que el primer intento de ejecutar la migración de
  llamadas falló con el error SQL Server 207 en las referencias a
  `solicitud_telnyx_estado` e `idempotency_key`. Se ajustó el script para
  ejecutar dinámicamente la restricción, la comprobación de duplicados y el
  índice que dependen de columnas recién agregadas. Está pendiente volver a
  ejecutar y confirmar el script en `ClienteTelefoniaDB`.
- La siguiente etapa de validación se realizará con el servidor encendido en
  el entorno del usuario. El usuario comunicará si está autorizado realizar
  una llamada Telnyx en tiempo real; hasta recibir esa confirmación no se
  iniciará ninguna llamada real.

## Preparación manual

Ejecuta `migracion_llamadas_concurrentes.sql` manualmente en la base correcta
antes de iniciar el servidor. Para aplicar el cobro sin reservas, detén el
servidor y ejecuta `migracion_sin_reservas_llamadas.sql` en
`ClienteTelefoniaDB` para liberar las reservas antiguas. Las migraciones no se
ejecutan automáticamente. Configura los valores reales de
`TELNYX_API_KEY`, `TELNYX_CONNECTION_ID`, `TELNYX_FROM_NUMBER` y
`TELNYX_PUBLIC_KEY` en el `.env` local, conserva las credenciales de base
existentes y no incluyas `.env` en paquetes. Consulta `GUIA_LLAMADAS.md` para
iniciar servidor/UI y configurar la URL HTTPS del webhook.