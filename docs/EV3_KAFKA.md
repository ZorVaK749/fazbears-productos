# EV3 product producers

AWS13/11: `Dockerfile.aws`, perfil `ev3-aws` con PostgreSQL/DDL validate, Kafka
habilitado y Entra real sin override LAB. Esquema/catálogo inicial deben revisarse
antes del rollout; no migra automáticamente H2. Workflow GHCR manual opt-in.
Procedimiento en `fazbear-pago/docs/AWS_DEPLOY_ROLLBACK.md`. Test cruzado exporta JSON
real de outbox y Analytics lo consume por Kafka sin adaptación. Puertos de test
pueden cambiarse con PRODUCTOS_TEST_PORT/CARRITO_TEST_PORT ante reservas Windows.

Kafka is **off by default** (`EV3_KAFKA_ENABLED=false`). Boot 4.1.1 manages
`spring-boot-starter-kafka`/Spring Kafka versions. Existing Dockerfile builds/runs Java 21.
No AWS, registry publishing, legacy H2/R2 files, or existing databases are required.
CI only builds/tests; it does not deploy.

## Explicit isolated local startup

Use a **new disposable PostgreSQL database**, never a legacy deployment database.
Set `SPRING_PROFILES_ACTIVE=ev3-local`, `PRODUCTOS_DB_URL` (JDBC PostgreSQL URL),
`PRODUCTOS_DB_USER`, `PRODUCTOS_DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS` and
`EV3_KAFKA_ENABLED=true`. Run `./mvnw spring-boot:run` or the Dockerfile image.
For a container, bootstrap/database addresses must be reachable from that container
(not its own localhost).

The profile overrides H2 and `create-drop` with PostgreSQL and additive Hibernate
`ddl-auto=update`; SQL initialization and H2 console are disabled. It adds an
`ev3_outbox` identity-key table; **no destructive migration** is supplied. Production
schema changes must be reviewed and applied separately, with `ddl-auto=validate`.
Retain the database and outbox identity sequence across restarts. Never truncate or
reset the outbox sequence while consumers retain projection versions.

`LOCAL_JWT_JWK_SET_URI` is mapped only in `ev3-local`. The override changes only the
signature key source, **not issuer, audience, tenant, expiration or delegated identity**.
Tokens must have the configured Entra issuer/client-id/tenant-id; interactions require
UUID `oid` and exact `access_as_user` scope. Shared local test keys must sign those
claims. No email/admin bypass. HTTP catalog POST/PUT/DELETE are deliberately denied:
there is no established administrator authorization policy. Existing authenticated
GET `/api/productos/{id}` still returns `id,nombre,precio,stock,categoria` and other
existing fields for backend quote lookups with the forwarded JWT.

Local RabbitMQ defaults to localhost/guest in this profile; existing stock queue,
exchange, retry and idempotency contracts are unchanged. Supply `RABBITMQ_*` for a
local broker; to test only Kafka set
`SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=false` explicitly.

No catalog is silently fabricated or replaced. The profile starts with whatever
catalog is actually in its database. Two additional explicit local-only flags:

* `--ev3.local.seed=true`: only if the catalog is empty, load the repository's existing
  `data.sql` application-menu seed (23 catalog entries, no user records).
* `--ev3.local.catalog-snapshot=true`: enqueue a snapshot of **the actual catalog**
  at startup. Use to bootstrap an empty analytics projection; disable afterward.
  Existing catalog rows are locked while snapshot events are appended.

## Events / consumer agreement

Exact envelope: `{eventId,eventType,schemaVersion:1,occurredAt,producer,
aggregateId,aggregateVersion,correlationId,data}`. UUID event/correlation IDs and ISO
UTC occurredAt are server-generated; producer is `ms-productos`. `aggregateVersion`
is the durable database outbox BIGINT identity (globally increasing per service,
not necessarily contiguous per aggregate); clients cannot set it. Aggregate ID is
the Kafka key.

* `producto.actualizado.v1`, key product ID string:
  `{productoId:long,nombre,categoria,precio:decimal,stock:int,activo:boolean}`.
  Actual service catalog saves/deletes and successful RabbitMQ stock mutations
  enqueue snapshots in the **same database transaction**. Delete emits `activo:false`.
  A duplicate stock delivery does not append another catalog event.
* `producto.interaccion.v1`, key `tenantId:ownerId`:
  `{tenantId:UUID,ownerId:UUID,productoId:long,tipo:'VIEW'|'SELECT'}`.
  `POST /api/productos/{id}/interacciones`, body `{"tipo":"VIEW"}` or `SELECT`,
  validates an existing product and derives identity exclusively from validated JWT.
  Public/authenticated catalog reads do **not** imply user interaction events.

Outbox relay runs off the HTTP path, batches at most 50 oldest pending rows, stops
the batch on failure, retries every 2 seconds, waits at most 5 seconds for send ACK
(producer metadata blocking also bounded at 3 seconds). Broker outage does not
block mutation requests. Pending events remain durable; generic bounded warnings
do not log payloads, tokens, exception messages or secrets. ACK then mark-delivered
can duplicate on crash: consumers must deduplicate **eventId**, not assume exactly
once. Projection consumers additionally reject older aggregateVersion for product
snapshots; interaction consumers count each distinct eventId (not version-watermark
filtering). Run one relay instance per database for ordered delivery; multiple
instances can replay/reorder, so consumers must remain idempotent/version-aware.
Delivered rows are retained; archive only after retention/replay policy is agreed.

## Verification

`./mvnw verify` runs isolated in-memory tests (no legacy H2 files), security/stock
regressions, exact envelopes and transaction rollback. Optional actual local
PostgreSQL/Kafka smoke tests:

```powershell
docker compose -f docs/compose-ev3-test.yml up -d
$env:EV3_DOCKER_TEST='true'
$env:PRODUCTOS_DB_URL='jdbc:postgresql://localhost:56432/ev3_productos_test'
$env:PRODUCTOS_DB_USER='ev3'
$env:PRODUCTOS_DB_PASSWORD='ev3-local-only'
$env:KAFKA_BOOTSTRAP_SERVERS='localhost:19092'
.\mvnw.cmd -B '-Dtest=PostgresKafkaIntegrationTest' test
```

The compose project is dedicated to these tests, binds only loopback, and does not
mount or touch any legacy volume. Stop with the same compose file and `down`
(do not use broad cleanup commands). Test synthetic products are explicit test-only
insertions into `ev3_productos_test`, never normal startup fixtures.
