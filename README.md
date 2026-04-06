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
- `BASE_FEE` (default: `100`)
- `TIMEOUT_SECONDS` (default: `30`)
- `DESTINATION_PUBLIC_KEY` (optional override destination account)

## Future quests

- Add a new quest class under `src/main/java/stellarquest/quests`.
- Wire it into `src/main/java/stellarquest/App.java` with a new command name.
- Reuse `StellarQuestClient` for Horizon access, network config, friendbot, and transaction submission.

## Dependencies

This starter uses the Java Stellar SDK from Lightsail (`network.lightsail:stellar-sdk`). The `pom.xml` is pinned to version `2.2.3`; update `stellar.sdk.version` if you want a newer release.
