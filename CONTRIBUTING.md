# Contributing to precise-ledger-pro

Thanks for contributing.

This project is financially sensitive: changes can affect balances, historical replay, and reversals. Please keep changes small, tested, and easy to reason about.

## Prerequisites

- Java 21
- Node.js 20.x
- Maven (or use `./mvnw`)
- Optional: IntelliJ HTTP Client CLI (`ijhttp`) for demo flows

## Project Setup

### Backend

```bash
./mvnw test
./mvnw compile quarkus:dev
```

### Frontend

```bash
cd src/main/webui
npm install
npm run dev
```

## Development Workflow

1. Understand domain impact first:
   - Is this touching backdated activity handling?
   - Is this touching reversal or retroactive sync?
2. Add/adjust tests before broad refactoring.
3. Keep public API and DTO changes explicit and documented in PR notes.

## Testing Guidelines

Run the smallest useful scope first, then expand:

```bash
./mvnw -Dtest=LedgerServiceTest test
./mvnw -Dtest=LoanResourceTest test
./mvnw test
```

For ledger-behavior updates, prefer fixture-driven tests under:

- `src/test/resources/data/ledger/service/`

These CSV files are part of the executable spec, not just test data.

## Code Style and Design Rules

- Prefer focused, minimal diffs.
- Preserve ledger invariants and idempotency.
- Avoid introducing hidden time dependencies in core logic.
- Keep algorithmic behavior explicit when replaying/rolling back entries.

## Pull Request Checklist

- [ ] Added/updated tests for behavior changes
- [ ] Verified impacted backend tests pass locally
- [ ] Updated docs when behavior or workflow changed
- [ ] Included a brief domain-impact note (what changed in ledger terms)

## CI Notes

GitHub Actions builds on:

- JDK 21
- Node.js 20
- Maven package lifecycle (`mvn -B package --file pom.xml`)

Make sure local changes are compatible with that toolchain.
