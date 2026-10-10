# Payment Platform

Spring Boot payment service that stores payments in PostgreSQL and calls the local mock payment gateway over HTTP.

## Current behavior

- Runs on `http://localhost:8080`.
- Creates payments with an `Idempotency-Key` header.
- Records `CREATED`, `PROCESSING`, and final status events.
- Calls the mock gateway on port `8081`.
- Maps gateway `APPROVED` to `SUCCESS` and `DECLINED` to `FAILED`.
- Leaves a payment in `PROCESSING` if the gateway call times out.
- Supports retrieving one payment and listing payments with pagination.

## Prerequisites

- Java 21
- Docker Desktop
- The mock payment gateway running on port `8081`

## Run locally

Start PostgreSQL from this directory:

```powershell
docker compose up -d
docker compose ps
```

This application currently requires the JVM timezone to be set to UTC. Set it in the same PowerShell terminal that will run Spring Boot:

```powershell
$env:JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
.\mvnw.cmd spring-boot:run
```

The environment variable applies to the current PowerShell session. If you open a new terminal, set it again before starting the application.

Start the mock gateway before creating a payment. See `../mock-payment-gateway/README.md`.

## API examples

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/hello
```

Create a payment:

```powershell
$headers = @{ "Idempotency-Key" = "payment-001" }
$body = @{
    orderId = "ORDER-1001"
    amount = 150.00
    currency = "INR"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
    -Uri http://localhost:8080/payments `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $body
```

Get payments:

```powershell
Invoke-RestMethod http://localhost:8080/payments/1
Invoke-RestMethod "http://localhost:8080/payments?page=0&size=10&sort=id,desc"
```

Use a new idempotency key for each new payment. Reusing a key returns the existing payment without calling the gateway again.

## Inspect PostgreSQL

Open an interactive PostgreSQL shell:

```powershell
docker exec -it payment-postgres psql -U payment_user -d payment_db
```

Useful commands inside `psql`:

```sql
\dt
\d payments
\d payment_events
SELECT * FROM payments ORDER BY id;
SELECT * FROM payment_events ORDER BY id;
SELECT * FROM payment_events WHERE payment_id = 1 ORDER BY created_at;
\q
```

## Stop locally

Stop the Spring application with `Ctrl+C`, then stop PostgreSQL:

```powershell
docker compose down
```

This preserves the database volume. `docker compose down -v` also deletes the stored database data.

