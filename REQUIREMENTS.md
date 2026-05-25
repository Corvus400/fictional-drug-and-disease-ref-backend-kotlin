# 要件（Phase 1 / backend-dev-kotlin-requirements Step 1）

- **DB**: PostgreSQL（JDBC + Exposed + HikariCP + Flyway。R2DBC は不採用）— Phase 3
- **認証**: JWT（将来の CMS write API / Admin 系ルートを保護）。当面の GET エンドポイントは public — Phase 5
- **API surface**: REST。当面 `GET` のみ（mock 契約互換）。将来 CMS 用 write API（POST/PUT/DELETE + JWT）の余地を残す。OpenAPI 公開あり — Phase 4
- **デプロイ先**: ローカル = Mac + Apple Container。実環境 = Cloudflare Tunnel（outbound-only）。OCI イメージは linux/amd64 でもビルドし Cloud Run lift-and-shift 可能性を維持 — Phase 7
- **非機能**: Ktor `RateLimit`（`CF-Connecting-IP` 単位、429 + Retry-After）+ Cloudflare edge の 2 層防御。OTel トレーシング + Micrometer/Prometheus `/metrics` + 構造化 JSON ログ。`/health`(liveness) / `/health/ready`(readiness, DB SELECT 1) — Phase 5/6
