<!-- markdownlint-disable MD013 -->

# Cloudflare Tunnel 公開ガイド

デフォルトの起動経路はローカルのみです。アプリケーションはホストポートを `127.0.0.1:18080` にバインドし、PostgreSQL はホストに公開しません。`18080` のルーターポートフォワードを追加しないでください。公開トラフィックは `cloudflared` 経由でのみ到達することを想定しています。

## 初回セットアップ

```bash
TUNNEL_HOSTNAME=fictional-drugref.win ./scripts/setup.sh
```

`setup.sh` は `cloudflared` のインストールまたは確認を行い、必要に応じて Cloudflare のブラウザログインを開き、名前付きトンネル `fictional-drugref-backend` を作成または再利用し、ホスト名の DNS をルーティングし、`cloudflared/config.yml` を書き出し、Cloudflare の証明書および認証情報ファイルの権限を制限します。

## 日常運用

```bash
./scripts/start.sh --public
./scripts/stop.sh
```

`start.sh --public` は PostgreSQL、Ktor アプリ、バックグラウンドの Cloudflare Tunnel プロセスを起動します。公開モード用のランタイム限定のデータベース秘密情報と JWT 秘密情報を生成し、一時的な env ファイル経由で注入します。

公開モードはデフォルトで `CMS_ENABLED=auto` を使います。隣接する CMS チェックアウト・依存関係・`pnpm`・`launchctl` が利用可能であれば CMS の開発サーバーも `127.0.0.1:5173` で起動し、そうでなければ CMS 起動のみスキップして公開バックエンド/トンネルの起動経路は使える状態に保ちます。CMS 起動を厳密に行う必要がある場合は `CMS_ENABLED=true ./scripts/start.sh --public`、意図的に CMS をスキップする場合は `CMS_ENABLED=false ./scripts/start.sh --public` を使います (CMS 連携の詳細は [cms-integration.md](./cms-integration.md))。

公開エッジの readiness はトンネル起動後に報告されます。エッジ readiness チェックの失敗でコマンドを失敗させたい場合は `PUBLIC_READINESS_REQUIRED=true` を設定してください。`stop.sh` はトンネル、記録された CMS 開発サーバープロセス、ローカルコンテナを停止します。

## 設定ファイルと秘密情報

コミット済みの `cloudflared/config.yml.example` は、他のリクエストを `http://localhost:18080` に転送する前に Cloudflare の ingress で `/metrics` を遮断します。生成された `cloudflared/config.yml`、トンネル認証情報、PID/ログファイルは Git に含めないでください。認証情報が漏洩した場合はトンネルを失効または再作成してください。

## 管理エンドポイントとアクセス制御

この架空の公開 API では Cloudflare Access を意図的に必須としていません。管理エンドポイントは公開トンネル経由でルーティングされず、保護された管理ハンドラには JWT 認証と admin スコープの認可も維持されています。実際の機微なデータ・コンプライアンス要件・複数の人間オペレーターを扱い始めた場合にのみ、後から Access を追加してください。
