# AGENTS.md

This file is the operating guide for humans and coding agents contributing to `precise-ledger-pro`.

## 1) Mission and Domain Context

- This is a **loan ledger** with support for:
  - out-of-order (backdated) ledger activities
  - clean reversals of previously applied activities
  - retroactive reconciliation between a primary ledger and a forked retroactive ledger
- The highest-risk business logic is around:
  - `LedgerService.applyLedgerActivities(...)`
  - `LedgerService.reverseLedgerActivity(...)`
  - `LedgerService.syncWithRetroactiveLedger(...)`
  - rollback helpers in `Ledger`

When changing any of the above, prioritize correctness and determinism over micro-optimizations.

## 2) Tech Stack

- Backend: Java 21, Quarkus 3.11, Maven
- Persistence: Panache/Hibernate (H2 in test/dev, PostgreSQL profile in prod)
- Money model: JavaMoney/Moneta
- Frontend: Vue 3 + Vite (served through Quinoa)
- Tests: JUnit 5, Quarkus Test, RestAssured, Mockito

## 3) Repository Map

- `src/main/java/ledger/common/`
  - Core ledger abstractions (`Ledger`, `LedgerActivity`, activity strategies)
- `src/main/java/ledger/service/`
  - Orchestration and application services (`LedgerService`, `LoanService`, calculators)
- `src/main/java/ledger/api/`
  - REST resources (`loan`, `ledger`, `ledgerevent`, etc.)
- `src/main/java/ledger/model/`
  - Data models and value carriers (`LedgerEntry`, `Balance`, etc.)
- `src/test/java/ledger/`
  - Integration/unit style tests for services, repositories, and API resources
- `src/test/resources/data/ledger/`
  - CSV fixtures driving many ledger scenario tests

## 4) Local Development Commands

Backend:

```bash
./mvnw compile quarkus:dev
./mvnw test
./mvnw package
```

Frontend (`src/main/webui`):

```bash
npm install
npm run dev
npm run build
```

Demo script:

```bash
ijhttp demo.http
```

## 5) Coding Principles for This Repo

1. Preserve ledger invariants:
   - `LedgerEntry` balances must remain consistent after every insertion.
   - Reversal/retroactive paths must not double-apply activity impact.
2. Prefer root-cause fixes in ledger orchestration instead of patching outputs.
3. Keep service logic testable:
   - avoid introducing untestable time coupling
   - pass time/context via `LedgerClock` and `TemporalActivityContext` patterns where feasible
4. Maintain idempotency behavior in `Ledger.addEntry(...)`.
5. Avoid broad refactors unless explicitly requested.

## 6) Testing Expectations

Before raising a PR for behavior changes:

1. Run targeted tests around touched area, then broader suite.
2. For retroactive/reversal logic changes, add or update fixture-based cases in:
   - `src/test/resources/data/ledger/service/`
3. Validate API behavior when service contracts change:
   - `src/test/java/ledger/api/`

Suggested sequence:

```bash
./mvnw -Dtest=LedgerServiceTest test
./mvnw -Dtest=LoanResourceTest test
./mvnw test
```

## 7) Change Safety Checklist

Use this checklist for financial correctness-sensitive changes:

- [ ] Backdated activity still triggers rollback + replay + sync path correctly.
- [ ] Reversal creates compensation entries with expected signs and balances.
- [ ] No duplicate entry insertion due to idempotency-key collisions.
- [ ] Entry ordering assumptions (`effectiveAt` vs `createdAt`) remain valid.
- [ ] CSV-based expectations updated where behavior intentionally changed.

## 8) Common Pitfalls

- Mixing `effectiveAt` and `createdAt` semantics during replay/sync.
- Introducing current-time calls where deterministic clock/context is expected.
- Updating one branch of retroactive logic without the corresponding sync path.
- Treating test fixture CSV files as incidental—they are core executable specs here.

## 9) PR Guidance

- Keep diffs focused and explain domain impact (especially balance/ledger-entry impact).
- Include “before/after” examples for ledger scenarios when changing core algorithms.
- If a fix is for a bug scenario, reference/add a targeted regression test.

## Git Commit Instruction

When committing changes locally for this repository, use the personal git identity below so commits are attributed correctly:

- Name: Vibhor Mahajan
- Email: vibhor.mahajan@gmail.com

Example commands:

```bash
git config user.name "Vibhor Mahajan"
git config user.email "vibhor.mahajan@gmail.com"
git add -A
git commit -m "chore(docs): record commit instruction and update test runner"
```

This instruction is intentionally recorded here so agents and contributors follow the repository's commit identity convention.
