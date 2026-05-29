<!-- markdownlint-disable MD013 -->

# CMS 連携ガイド

このバックエンドは、[fictional-drug-and-disease-ref-cms](https://github.com/Corvus400/fictional-drug-and-disease-ref-cms) (以下 CMS) が医薬品・疾病データを編集するための管理 API (admin API) を提供します。CMS リポジトリ側の README は軽量で、連携契約はこのバックエンド側を正 (SSOT) とします。

API の詳細仕様は、稼働中の **admin Swagger / ReDoc** を参照してください (本ドキュメントには手書きのエンドポイント表は置きません)。

- Swagger UI: <http://127.0.0.1:19090/v1/admin/swagger>
- ReDoc: <http://127.0.0.1:19090/v1/admin/redoc>
- OpenAPI JSON: <http://127.0.0.1:19090/v1/admin/openapi.json>

admin OpenAPI は admin コネクタ (ポート `19090`) でのみ提供され、公開ポート (`18080`) の `/openapi.json` には含まれません。

## 前提

- CMS のチェックアウトが、このバックエンドのチェックアウトと**隣接**して存在すること。既定の探索先は `$(dirname <backend>)/fictional-drug-and-disease-ref-cms` です。
- 場所が異なる場合は `CMS_DIR=/path/to/fictional-drug-and-disease-ref-cms` で上書きします。
- CMS 開発サーバーの起動には `pnpm` と `launchctl` (macOS) が必要です。

## CMS の起動

`./scripts/start.sh` は、バックエンド (read API `127.0.0.1:18080` / admin API `127.0.0.1:19090`) に加えて、隣接する CMS チェックアウトがあれば CMS 開発サーバーを `127.0.0.1:5173` で起動します。

CMS の起動可否は環境変数 `CMS_ENABLED` で制御します。

| 値 | 挙動 |
| --- | --- |
| `true` | CMS を起動する。前提 (チェックアウト・`pnpm`・`launchctl` 等) が不足する場合は **ERROR で停止**。 |
| `auto` | CMS を起動するが、前提が不足する場合は **WARNING で続行** (CMS のみスキップ)。 |
| `false` | CMS を起動しない。 |

未設定時の既定値は、通常起動 (`./scripts/start.sh`) で `true`、公開起動 (`./scripts/start.sh --public`) で `auto` です。

```bash
# 通常起動 (CMS も起動)
./scripts/start.sh

# CMS を起動しない
CMS_ENABLED=false ./scripts/start.sh

# CMS チェックアウトの場所を指定
CMS_DIR=/path/to/fictional-drug-and-disease-ref-cms ./scripts/start.sh
```

## CMS への注入値 (admin コネクタ)

`start.sh` は CMS 開発サーバー起動時に、admin コネクタ (`127.0.0.1:19090`) を指す以下の環境変数を注入します。

| 変数 | 既定値 | 意味 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://127.0.0.1:19090` | CMS が叩く admin API のベース URL (admin ポート)。 |
| `VITE_API_TIMEOUT_MS` | `10000` | API 呼び出しのタイムアウト (ミリ秒)。 |
| `VITE_ADMIN_TOKEN_PATH` | `/v1/admin/token` | 管理トークン発行エンドポイントのパス。 |

CMS の起動後、`127.0.0.1:5173` が HTTP 200 を返すまで待機します。`./scripts/stop.sh` は記録された CMS 開発サーバープロセスとローカルコンテナを停止します。

## トークンブートストラップ

管理 API の更新系は admin scope を持つ JWT を必要とします。トークンは認証不要のローカルブートストラップで発行します。

- `POST /v1/admin/token` — admin コネクタ到達=信頼とみなし、認証なしで admin JWT を発行します。CMS はこのエンドポイント (`VITE_ADMIN_TOKEN_PATH`) からトークンを取得します。
- レスポンスは正準の `access_token` フィールドと、CMS 互換の `token` エイリアス (同値) の両方を含みます。
- CLI から取得する場合は `./gradlew printAdminToken` が `JWT_SECRET` で署名したトークンを出力します。

> [!WARNING]
> admin コネクタに到達できるプロセスは誰でも admin JWT を発行できます。信頼できる単一ユーザーの開発ホストでのみ使用し、トークンをログ・Issue・Pull Request・スクリーンショットに貼り付けないでください。Cloudflare Tunnel など公開エッジを admin コネクタへ向けないでください ([cloudflare-tunnel.md](./cloudflare-tunnel.md))。

## 楽観的並行制御 (ETag / If-Match)

データの取り違え更新を防ぐため、更新系の admin API は楽観ロックを用います。

- 読み取り API (`/v1/...`) はレスポンスに `ETag` ヘッダー (リソースの `updated_at` に対応) を付与します。
- 更新系 (`PUT` / `PATCH` / `DELETE`、画像アップロード) は `If-Match` ヘッダーに現在の ETag を必須で要求します。
- ETag が一致しない / 欠落している場合は `412 Precondition Failed` を返します。
- `PATCH` は JSON Merge Patch (RFC 7386) を用い、`Content-Type: application/merge-patch+json` を要求します。一致しない場合は `415 Unsupported Media Type` を返します。

各エンドポイントの詳細なリクエスト/レスポンススキーマは admin Swagger を参照してください。

## CORS

`start.sh` は CMS のオリジン `http://127.0.0.1:5173` および `http://localhost:5173` を、read API の `CORS_ALLOWED_ORIGINS` と admin API の `ADMIN_CORS_ALLOWED_ORIGINS` の既定値として注入します。別のオリジンから CMS を動かす場合はこれらを上書きしてください。
