# Stock asíncrono — contrato pedido.creado v2

## Qué incorpora esta versión

- `Producto.stock` comienza en **20 unidades** en los 23 productos de demostración
  y en productos nuevos si no se indica otra cantidad.
- GET de productos devuelve `stock`. POST/PUT rechazan stock negativo con HTTP 400.
- PUT sin el campo `stock` conserva el inventario existente; no lo repone a 20.
- `ms-productos` consume su propia cola durable `pedido.creado.productos.queue`.
- Exchange `pedidos.exchange` (direct), routing key `pedido.creado`.
- DLQ durable `pedido.creado.productos.queue.dlq`, a través de `pedidos.dlx`, routing
  key `pedido.creado.productos.queue`. No compartir la cola con otros servicios.

## Contrato publicado por ms-orden

Se conservan los campos anteriores y se añaden `schemaVersion`, `eventoId` e `items`:

```json
{
  "schemaVersion": 2,
  "eventoId": "9abe44c5-206a-4231-9ba0-14cd3101f42f",
  "pedidoId": 2,
  "usuarioId": "usuario-de-prueba",
  "emailUsuario": "cliente@example.com",
  "total": 12.99,
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-10-14T12:00:00",
  "items": [{ "productoId": 1, "cantidad": 1 }]
}
```

`eventoId` es un UUID generado una vez al construir ese evento. RabbitMQ conserva
el mismo JSON en las redeliveries. Un pedido nuevo puede reutilizar un pedidoId
tras reiniciar H2 de pedidos, pero recibe un UUID distinto y no se confunde con un
evento histórico de inventario. No usar el nombre de clase Java como contrato;
el listener infiere su DTO local. No ampliar paquetes confiables indiscriminadamente.

Notificaciones y reportes se actualizan para ignorar campos nuevos; sus pruebas
verifican los eventos anteriores y v2. Productos rechaza eventos antiguos sin UUID,
versión 2 o items, preservándolos en DLQ; no inventa cantidades ni descuenta pedidos
históricos automáticamente. El pedido #1 de la demo anterior no se reprocesa.

## Atomicidad e idempotencia

`StockService` ejecuta una transacción independiente por evento:

1. Valida UUID, versión, pedidoId y cantidades/productoId positivos.
2. Agrupa líneas del mismo producto y las ordena por ID.
3. Inserta un registro con PK `eventoId` y una huella de pedido/items.
4. Descuenta cada producto con un UPDATE condicionado a `stock >= cantidad`.
5. Confirma el registro y todos los descuentos juntos, antes del ACK del listener.

Si no existe un producto o falta stock, se revierte **todo**, incluido el registro
del evento. Se puede reintentar ese evento después de resolver la incidencia.
Una entrega duplicada ya confirmada no descuenta otra vez. El PK único evita que
dos consumidores concurrentes confirmen la misma entrega; el conflicto se resuelve
consultando el registro después del rollback. Reutilizar un eventoId con contenido
distinto es un error y no modifica stock. Dos compras que compiten por una última
unidad no pueden dejar el inventario negativo por este consumidor.

Eventos inválidos o inventario insuficiente se rechazan sin reencolado infinito.
Los fallos de consumo tienen reintentos limitados (`max-retries=3`); al agotarse,
se rechazan y pueden dirigirse a la DLQ. Vigilar la DLQ; no purgarla como solución.

## Configuración de despliegue

Productos ahora requiere configurar explícitamente `RABBITMQ_HOST`, `RABBITMQ_USER`,
`RABBITMQ_PASS` y `RABBITMQ_VHOST`. No se proporcionan contraseñas/host de ejemplo.
`RABBITMQ_PORT` tiene default 5672 y `RABBITMQ_SSL_ENABLED` default false para
compatibilidad, pero deben corresponder a la URL real del proveedor. Para amqps,
usar 5671 (o el puerto indicado) y SSL habilitado con validación de certificados.
Los nombres estándar `SPRING_RABBITMQ_*` también son aplicables por Spring Boot.
Introducir secretos privadamente en SSM, no en Git, chat, logs ni parámetros Run Command.

Orden ya tiene configuración real corregida en su contenedor actual: conservarla
al reemplazar la imagen. **No reutilizar el respaldo inicial con `TU_HOST.cloudamqp.com`.**

### Orden de actualización (sin compras durante la transición)

1. Construir las cuatro imágenes y respaldar configuración actual por instancia.
2. Actualizar notificaciones y reportes compatibles con campos nuevos.
3. Configurar/desplegar productos con el broker/vhost real y verificar su consumidor.
4. Actualizar orden al publicador v2 **al final**.
5. Verificar tres colas principales, tres consumidores y sus DLQ antes de comprar.

No basta con `git pull` y no se debe desplegar primero el publicador: los consumidores
actuales anteriores a este cambio rechazan propiedades desconocidas. Si se revierte
alguna parte, pausar compras y evaluar la compatibilidad antes de seguir; no borrar colas.

## Prueba de aceptación en AWS (pendiente)

1. Obtener GET autenticado `/productos/1` por el Gateway; anotar stock=20.
2. Comprar una unidad una sola vez. Anotar pedidoId/eventoId de los logs.
3. Repetir GET: stock=19; verificar log `Stock descontado` y correo/reporte del pedido.
4. Verificar colas principales y DLQ sin errores pendientes.
5. Una repetición controlada del **mismo evento** debe dejar stock=19 y registrar
   `Stock ya procesado`; no crear otra compra para simular una redelivery.

La repetición de eventos requiere autorización explícita: puede duplicar correos o
ventas en otros consumidores que todavía no tienen deduplicación. Las pruebas
locales de idempotencia no publican eventos al broker real ni afectan la tienda.

## Límites que esta implementación no oculta

- H2 en memoria y `create-drop` reinician stock a 20 y eliminan el ledger al reiniciar
  productos. La idempotencia dura mientras se conserve esa BD; usar BD persistente
  y migraciones para recuperación real. Reiniciar reportes borra sus estadísticas.
- Este es un descuento **asíncrono**, no una reserva de stock previa al pago. Si falta
  inventario, el incidente queda en DLQ, pero el pedido/correo/reporte ya pueden existir.
  No se cancela ni compensa automáticamente el pedido y no se garantiza ausencia de
  sobreventa a nivel de negocio. Añadir reserva/confirmación y saga sería otro cambio.
- El publicador aún no tiene outbox ni idempotencia HTTP: si crea el pedido y falla
  RabbitMQ, no se debe volver a pagar a ciegas. Los retries HTTP no son redeliveries.
- Los controles de roles/propiedad y precios oficiales continúan como trabajo separado.
- No se ha añadido visualización de stock en Angular: la evidencia inicial es el GET
  autenticado y los logs. No es necesario reconstruir el frontend para este contrato.
- Las pruebas locales usan H2, HTTP local y mocks/conversores. No equivalen a la prueba
  en CloudAMQP y EC2, que debe hacerse después del despliegue autorizado.

## Pruebas locales

```powershell
# productos: suite completa, sin depender de Azure/RabbitMQ reales
mvn.cmd -B test
# orden: pruebas focalizadas (los tests de contexto antiguos siguen separados)
mvn.cmd -B "-Dtest=RabbitMQConfigTest,PedidoEventTest,OrdenServiceTest" test
# notificaciones
mvn.cmd -B "-Dtest=RabbitMQConfigTest,EmailServiceTest" test
# reportes
mvn.cmd -B "-Dtest=RabbitMQConfigTest,ReportesServiceTest" test
```
