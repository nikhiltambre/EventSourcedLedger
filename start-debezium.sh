#!/usr/bin/env bash

set -e

# Dynamically resolve project root
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONNECTOR_FILE="$PROJECT_DIR/connectors/wallet-ledger-connector.json"

echo "=========================================="
echo " 1. Starting All Services "
echo "=========================================="
cd "$PROJECT_DIR"

# Docker Compose will start zookeeper, mysql, kafka, wait for their healthchecks,
# and then automatically spin up debezium.
docker compose up -d

echo ""
echo "=========================================="
echo " 2. Waiting for Debezium REST API (8083) "
echo "=========================================="
until curl -s -f http://localhost:8083/connectors > /dev/null 2>&1; do
  echo "Debezium REST API is initializing... waiting 5 seconds."
  sleep 5
done
echo "Debezium REST API is LIVE!"

echo ""
echo "=========================================="
echo " 3. Registering Wallet Ledger Connector "
echo "=========================================="
if [ ! -f "$CONNECTOR_FILE" ]; then
  echo "Error: Connector configuration file not found at $CONNECTOR_FILE"
  exit 1
fi

curl -s -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @"$CONNECTOR_FILE" | grep -v '^\s*$' || true

echo -e "\n"

echo "=========================================="
echo " 4. Verifying Connector Status "
echo "=========================================="
sleep 3
curl -s http://localhost:8083/connectors/wallet-ledger-connector/status | echo "$(cat)"

echo -e "\n"
echo "=========================================="
echo " System Ready! "
echo "=========================================="
echo "To view CDC events in real time, run:"
echo "docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic wallet.ledger_write_db.ledger_events --from-beginning"