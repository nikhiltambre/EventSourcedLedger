# Project Status Report — Event-Sourced Ledger
> Updated: 2026-08-04

## Branch: `stage/5-microservices-split`

---

## Stage Progress

| Stage | What it does                                                              | Status |
|-------|---------------------------------------------------------------------------|--|
| **1** | Event Store — append-only ledger, full replay                             | ✅ Done |
| **2** | Concurrency Versioning — `UNIQUE(aggregate_id, version)`, conflict check  | ✅ Done |
| **3** | Snapshotting — `account_snapshots` every 50 events, delta replay          | ✅ Done |
| **4** | Cache + Pub/Sub — Caffeine L1, Redis L2, cross-replica invalidation       | ✅ Done |
| **5** | Gateway + CQRS + CDC — split into command/query, Debezium, Kafka consumer | ✅ Done |
| **6** | Partitioning + Idempotency + Manual Commits                               | ✅ Done |
| **7** | Saga Orchestration                                                        | ⬜ Not started |
| **8** | Resilient Saga State                                                      | ⬜ Not started |
| **9** | Audit & Reconciliation                                                    | ⬜ Not started |
| **10** | JVM + Container Tuning                                                    | ⬜ Not started |

---

## Repo Structure

```
EventSourcedLedger/
├── .env                              ← secrets (gitignored)
├── docker-compose.yaml               ← all 8 services
├── init/init-db.sh                   ← write DB + read DB + debezium user
├── connectors/wallet-ledger-connector.json
├── EurekaServer/                     ← port 8761
├── api-gateway/                      ← port 8080
├── WalletCommandService/             ← port 8081
├── WalletQueryService/               ← port 8082
└── docs/
```

---

## Port Map

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Command Service | 8081 |
| Query Service | 8082 |
| Debezium | 8083 |
| Eureka | 8761 |
| MySQL | 3307 → 3306 |
| Kafka | 9092 → 29092 |
| Redis | 6379 |
| Zookeeper | 2181 |

---

## Git Tags

| Tag | What it covers |
|-----|---------------|
| `v0.3.0` | Stages 1–3 |
| `v0.4.0` | Stage 4 |
