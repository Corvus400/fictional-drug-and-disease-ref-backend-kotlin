<!-- markdownlint-disable MD013 -->

# fictional-drug-and-disease-ref-backend-kotlin

Kotlin/Ktor backend for the fictional drug and disease reference API.

## Quick Start

```bash
./scripts/setup.sh
./scripts/start.sh
curl -s http://127.0.0.1:8080/health/ready
./scripts/stop.sh
```

`./scripts/start.sh` starts a fresh PostgreSQL container, waits for database readiness, builds the application image, runs Flyway migrations, and exposes the API only on `127.0.0.1:8080`.

## Runtime Requirements

- macOS 26 or later for Apple Container
- JDK 21
- Apple Container 0.8.x
- Rosetta 2 on Apple Silicon when running x86_64 images

## Probe Mapping

| Platform probe | Endpoint | Expected success | Dependency check | Operational meaning |
| --- | --- | --- | --- | --- |
| Liveness | `/health` | `200 {"status":"ok"}` | None | Process is running. Do not point this at the database-dependent readiness probe. |
| Readiness | `/health/ready` | `200 {"status":"ready"}` | PostgreSQL `Connection.isValid(1)` | Instance can receive traffic. Returns `503 {"status":"not_ready"}` when the database is unavailable. |

Use `/health` for restart decisions and `/health/ready` for traffic routing. A transient database outage should remove the instance from traffic, not restart the process.

## Metrics

`/metrics` exposes Prometheus metrics and is guarded by a CIDR allowlist based on the real socket peer address. The default allowlist is intended for local and private-network operational access. Cloudflare Tunnel deployment will add an edge-level `/metrics` block so public traffic cannot scrape internal metrics.

## Local Verification

```bash
./scripts/start.sh
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/health/ready
container stop fictional-drugref-backend-postgres
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/health/ready
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/health
./scripts/stop.sh
```

