# Stellar Quest Java Starter

A minimal Java starter to run Stellar Quest Learn tasks with less setup each time.

## Quick start (Spring Boot CLI)

Prereqs:
- Java 11+ (`java -version`)
- Maven on PATH (`mvn -v`)

1) Set your quest secret key:

```bash
export QUEST_SECRET="S..."
```

PowerShell:

```powershell
$env:QUEST_SECRET="S..."
```

To persist across new shells (PowerShell):

```powershell
[System.Environment]::SetEnvironmentVariable("QUEST_SECRET","S...","User")
```

2) Build (Spring Boot fat jar):

```bash
mvn -q -DskipTests package
```

3) Create and fund a new account from your quest account:

```bash
java -jar target/stellar-quest-java-1.0.0.jar create-account
```

4) Send a payment from your quest account:

```bash
java -jar target/stellar-quest-java-1.0.0.jar payment
```

5) Create a trustline from your quest account:

```bash
java -jar target/stellar-quest-java-1.0.0.jar trustline
```

6) Create an offer (buy/sell/passive):

```bash
java -jar target/stellar-quest-java-1.0.0.jar offer
```

7) Execute a path payment (strict send):

```bash
java -jar target/stellar-quest-java-1.0.0.jar path-payment
```

Add `--verbose` (or `-v`) to print balances before and after:

```bash
java -jar target/stellar-quest-java-1.0.0.jar create-account --verbose
java -jar target/stellar-quest-java-1.0.0.jar payment -v
```

4) (Optional) Fund any account via friendbot:

```bash
java -jar target/stellar-quest-java-1.0.0.jar fund G...
```

## Environment variables

- `QUEST_SECRET` (required for `create-account`)
- `HORIZON_URL` (default: `https://horizon-testnet.stellar.org`)
- `FRIENDBOT_URL` (default: `https://friendbot.stellar.org`)
- `NETWORK` (`testnet` default, or `public`)
- `STARTING_BALANCE` (default: `1000`)
- `PAYMENT_AMOUNT` (default: `100`)
- `ASSET_CODE` (default: `SANTA`)
- `TRUST_LIMIT` (default: `100`)
- `BASE_FEE` (default: `100`)
- `TIMEOUT_SECONDS` (default: `30`)
- `DESTINATION_PUBLIC_KEY` (optional override destination account)
- `PAYMENT_DESTINATION_PUBLIC_KEY` (optional override payment destination)
- `ISSUER_PUBLIC_KEY` (optional override issuer account)
- `OFFER_TYPE` (default: `sell`, options: `buy|sell|passive`)
- `OFFER_ASSET_CODE` (default: `USDC`)
- `OFFER_ASSET_ISSUER` (default: `GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5`)
- `OFFER_PRICE` (default: `0.1`)
- `OFFER_AMOUNT` (default: `1000`, sell/passive amount)
- `OFFER_BUY_AMOUNT` (default: `100`, buy amount)
- `OFFER_ID` (default: `0`, manage buy/sell only)
- `OFFER_TRUST_LIMIT` (optional trust limit for the offer asset)
- `PATH_ASSET_CODE` (default: `PATH`)
- `PATH_SEND_AMOUNT` (default: `1000`)
- `PATH_DEST_MIN` (default: `1000`)
- `PATH_DEST_AMOUNT` (default: `450`)
- `PATH_SEND_MAX` (default: `450`)

## Future quests

- Add a new quest class under `src/main/java/stellarquest/quests`.
- Wire it into `src/main/java/stellarquest/App.java` with a new command name.
- Reuse `StellarQuestClient` for Horizon access, network config, friendbot, and transaction submission.

## Dependencies

This starter uses the Java Stellar SDK from Lightsail (`network.lightsail:stellar-sdk`). The `pom.xml` is pinned to version `2.2.3`; update `stellar.sdk.version` if you want a newer release.
