# Stellar Quest Mission Control (Web App)

A Spring Boot web app for running Stellar Quest transactions from a browser UI.

## Quick start

Prereqs:
- Java 11+
- Maven on PATH

1) Set your quest secret key:

```bash
export QUEST_SECRET="S..."
```

PowerShell:

```powershell
$env:QUEST_SECRET="S..."
```

2) Start the app:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

3) Open the cockpit:

- http://localhost:8081

## What you can do in the UI

- Create account
- Send payment
- Create trustline
- Create offer (buy/sell/passive)
- Run strict-send path payment
- Fund account with friendbot

## REST API

- `GET /api/config`
- `GET /api/accounts/{accountId}`
- `POST /api/transactions/fund`
- `POST /api/transactions/create-account`
- `POST /api/transactions/payment`
- `POST /api/transactions/trustline`
- `POST /api/transactions/offer`
- `POST /api/transactions/path-payment`

All endpoints return a JSON envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

## Environment variables

- `QUEST_SECRET`
- `HORIZON_URL` (default: `https://horizon-testnet.stellar.org`)
- `FRIENDBOT_URL` (default: `https://friendbot.stellar.org`)
- `NETWORK` (`testnet` default, or `public`)
- `STARTING_BALANCE`
- `PAYMENT_AMOUNT`
- `ASSET_CODE`
- `TRUST_LIMIT`
- `BASE_FEE`
- `TIMEOUT_SECONDS`
- `DESTINATION_PUBLIC_KEY` (optional default destination)
- `PAYMENT_DESTINATION_PUBLIC_KEY` (optional default payment destination)
- `ISSUER_PUBLIC_KEY` (optional default issuer)
- `OFFER_TYPE` (default: `buy`, options: `buy|sell|passive`)
- `OFFER_ASSET_CODE` (default: `USDC`)
- `OFFER_ASSET_ISSUER`
- `OFFER_PRICE` (default: `0.1`)
- `OFFER_AMOUNT` (default: `1000`)
- `OFFER_BUY_AMOUNT` (default: `100`)
- `OFFER_ID` (default: `0`)
- `OFFER_TRUST_LIMIT` (optional)
- `PATH_ASSET_CODE` (default: `PATH`)
- `PATH_SEND_AMOUNT` (default: `1000`)
- `PATH_DEST_MIN` (default: `1000`)

## Notes

- Friendbot actions only make sense on testnet.
- The old CLI quest classes still exist in the codebase, but startup now runs as web-only.
