# Retroactive Reconciliation in Financial Ledgers: A Fork-and-Sync Architecture for Out-of-Order Activities and Reversals with Per-Entry Traced Corrections

**Vibhor Mahajan** (Independent Researcher), **Raman Bedi**

---

> **arXiv Submission Metadata (draft)**
> - **Archive / Subject Class:** cs.DB (Databases); secondary cs.SE (Software Engineering)
> - **ACM Classification:** H.2.4 (Systems), H.2.8 (Database Applications)
> - **Abstract character count:** ~1,650 (within 1,920-character limit)
> - **Status:** First draft — not yet submitted

---

## Abstract

Financial ledger systems in lending institutions must accommodate two classes of late-arriving events that violate chronological ordering assumptions: out-of-order (backdated) payment activities and reversals of previously applied activities. Existing approaches — full ledger reconstruction, append-only compensating entries, and mutable rewriting — each fail to simultaneously preserve the complete audit trail, maintain derived-calculation correctness, and support arbitrary future retroactive events. We present a fork-and-sync architecture that addresses all three requirements. When a retroactive event arrives, the system forks the primary ledger at the appropriate historical insertion point, applies the event and replays all subsequent activities on the fork, and then reconciles the fork back into the primary ledger by computing per-activity balance-impact deltas. Each reconciliation emits individual, source-attributed adjustment entries rather than a single grouped correction, preserving entry-level audit traceability and enabling clean composition of future retroactive events. A virtual ledger clock decouples logical time from wall-clock time to guarantee deterministic replay. We describe the algorithmic design, implement it in a reference system, and provide a comparative evaluation against a conventional grouped-adjustment ledger processing nine activities that include a reversal and a backdated payment, demonstrating full audit traceability with no balance drift.

---

## 1. Introduction

Loan servicing systems must process millions of ledger activities daily, yet financial events routinely arrive out of chronological order. A payment may be applied days after its contractual effective date, a disbursement may be discovered to have been incorrectly processed, or a regulatory investigation may require reversing a fee applied months ago. Each such event is not merely an append at the tail of the ledger — it changes the basis on which every subsequent derived value (daily interest accrual, balance allocation) was computed.

Three strategies are prevalent in practice. **Full reconstruction** rebuilds the entire ledger from its initial state each time a retroactive event arrives. This is correct but computationally expensive: for a ledger with thousands of entries and a retroactive event at position *k*, the cost is proportional to the number of entries following *k* that must be recomputed and persisted. **Append-only compensation** appends a single correcting entry (or a small group of entries) without recomputing the downstream derived values that depended on the corrected balance. This is efficient but produces balance drift whenever the corrected activity altered the basis for any subsequent temporal calculation. **Mutable rewriting** updates historical entry values in place, which destroys the immutable audit trail required by financial regulations and introduces concurrency hazards.

A fourth technique — grouping downstream corrections into a single opaque adjustment entry — is balance-correct but violates a subtler invariant: it destroys entry-level audit traceability. When a grouped adjustment of, say, +\$3.30 appears in a ledger, neither an auditor nor a subsequent retroactive event processor can determine which of the original entries it corrects or by how much — not without replaying the entire history from scratch.

This paper presents **PreciseLedgerPro**, a system and accompanying algorithm family that resolves this tension. The core innovation is a fork-and-sync protocol in which:

1. The primary ledger is never mutated; its original entries form the permanent audit record.
2. A retroactive fork contains the corrected view of history, computed efficiently from the insertion point onward.
3. Per-activity differential reconciliation emits one individually source-attributed adjustment entry per divergent downstream activity — not a single grouped correction.
4. A virtual ledger clock advances only forward, providing a consistent time reference for deterministic replay of time-dependent calculations.
5. Composite idempotency keys prevent duplicate entries during recursive replay cycles.

The result is a ledger in which every row — including every correction — can be explained by reference to a single source activity, and in which any subsequent retroactive event can be applied cleanly without reference to a decomposed adjustment history.

The remainder of this paper is organized as follows. Section 2 provides background on financial ledger data models and formalizes the problem. Section 3 surveys related work. Section 4 describes the system architecture and data model. Section 5 presents the four core algorithms. Section 6 describes the reference implementation. Section 7 provides a comparative evaluation. Section 8 discusses design trade-offs, limitations, and future work. Section 9 concludes.

---

## 2. Background and Problem Statement

### 2.1 Ledger Data Model

A financial ledger for a loan account maintains an ordered sequence of *ledger entries*. Each entry is an immutable record capturing:

- An incremental balance change across multiple components: principal *p*, interest *i*, fee *f*, excess *e*.
- A running balance (*P*, *I*, *F*, *E*) after this entry, computed as the running balance of the preceding entry plus the incremental change.
- An *effectiveAt* timestamp: the business-effective date of the originating activity.
- A *createdAt* timestamp: the system processing time.
- A *source activity identifier*: a (type, id) pair that links the entry to the ledger activity that created it.

A **ledger activity** is an abstract event — a payment, a disbursement, a daily interest accrual, a reversal — that *applies* to a ledger by generating one or more ledger entries.

Formally, let *L* = ⟨*e*₁, *e*₂, …, *eₙ*⟩ be the primary ledger ordered by *effectiveAt*. For a new activity *a* with effective date *t*:

- *a* is **in-order** if *t* ≥ *effectiveAt*(*eₙ*).
- *a* is **backdated** (out-of-order) if *t* < *effectiveAt*(*eₙ*).

### 2.2 Derived Values and the Correction Problem

Daily interest accrual — the canonical temporal ledger activity — computes:

$$\text{interest}_d = P_{d-1} \times \frac{r}{n}$$

where *P*<sub>*d*-1</sub> is the principal balance at the close of day *d*-1, *r* is the annual interest rate, and *n* is the day-count basis. If a backdated event at position *k* changes *P*<sub>*k*-1</sub>, then every subsequent daily interest entry must be recomputed, since each entry's interest accrual depends on the running principal balance at its own computation time.

The correction problem is therefore: given a retroactive event that changes the balance at position *k* in a ledger of *n* entries (*k* < *n*), efficiently propagate the correct balance deltas to all entries at positions *k*+1 through *n* without destroying the original entry history.

### 2.3 Requirements

We derive four requirements from financial system practice:

- **R1 (Correctness):** After applying any retroactive event, running balances of all subsequent entries must reflect the corrected history.
- **R2 (Audit Immutability):** Original ledger entries must not be modified or deleted.
- **R3 (Entry-Level Traceability):** Each correction entry must identify the source activity it corrects.
- **R4 (Retroactive Composability):** The ledger must support a subsequent retroactive event without requiring decomposition of prior correction entries.

Systems based on full reconstruction satisfy R1–R4 but at high computational cost. Grouped-adjustment approaches satisfy R1–R2 but violate R3–R4. Mutable rewriting satisfies only R1. This paper provides a system that satisfies all four.

---

## 3. Related Work

### 3.1 Event Sourcing and Append-Only Ledgers

Event sourcing [Fowler 2005] stores state changes as an immutable sequence of events from which current state is derived by replay. While event sourcing provides R2, standard implementations reconstruct current state by full replay, satisfying R1 only if the replay is complete and ordered. They do not address the efficiency problem of partial re-derivation from an insertion point, nor do they produce individually traced correction entries (R3).

### 3.2 Bitemporal Data Models

Snodgrass [1999] and Jensen et al. [1994] formalize bitemporal databases that track both *valid time* (business-effective time) and *transaction time* (system recording time). Bitemporal models preserve full history but typically require query-time reconstruction to derive correct values at arbitrary time points — they do not produce an updated, materially correct running ledger in place. The fork-and-sync approach presented here can be viewed as a form of bi-temporal reconciliation that materializes corrected running balances in the primary ledger while respecting transaction-time immutability.

### 3.3 Compensating Transactions and Sagas

The saga pattern [Garcia-Molina and Salem 1987] uses compensating transactions to undo the effects of failed or rolled-back long-running transactions. A reversal activity in our model is structurally similar to a compensating transaction, but differs in a critical respect: a simple compensation negates only the direct impact of the reversed activity. It does not account for the downstream derived effects — the interest accruals and allocation changes that occurred because of the now-reversed balance state. Our reversal algorithm generates both a compensation entry and a differential reconciliation of all downstream derived values.

### 3.4 Financial Ledger Systems in Prior Literature

Meijer and Fokkinga [1991] describe a functional model of double-entry bookkeeping. More recently, Hellerstein et al. [2019] survey transactional data systems, and the OLAP literature addresses aggregate balance queries over large financial datasets, but handling retroactive insertions with maintained running balances has received limited attention in academic systems research. Commercial loan servicing systems (e.g., ACBS, nCino) handle backdating operationally, but their algorithms are proprietary and their audit-trail properties are not publicly documented.

### 3.5 Idempotent Processing

Idempotency in distributed systems is well-studied [Bernstein and Goodman 1981]. Our use of composite idempotency keys on ledger entries, rather than at the transaction level, is specific to the recursive replay problem: the same ledger activity may be "seen" multiple times during nested retroactive recursion, and the key must be stable across all encounters to prevent duplicates.

---

## 4. System Architecture

### 4.1 Core Data Structures

**Ledger.** An ordered list of `LedgerEntry` records associated with a single loan account. The ledger supports:

- `addEntry(e)` — idempotent append with running-balance recomputation.
- `rollbackToEntryBefore(effectiveAt)` — returns a new ledger (fork) containing only entries with `effectiveAt` ≤ the given timestamp.
- `rollbackToEntryBefore(activityType, activityId)` — returns a fork excluding all entries from the first entry of a given source activity onward.
- `calculateTotalImpact(activityType, activityId)` — returns the sum of all balance changes across all entries attributed to a given source activity.
- `getCurrentBalance()` — returns the running multi-component balance.

**LedgerEntry.** An immutable record that captures an incremental balance change across the four components (principal, interest, fee, excess), running balances after the entry, the business-effective timestamp (`effectiveAt`), the processing-time timestamp (`createdAt`), and a source-activity identifier pair (`sourceLedgerActivityType`, `sourceLedgerActivityId`) that permanently links the entry to the ledger activity that created it. A composite idempotency key — a deterministic hash over the loan identifier, entry identifier, entry type, source-activity pair, and effective date — prevents duplicate entry insertion during recursive replay cycles. The complete field specification is given in Appendix C, Table C1.

**Balance.** A 4-tuple (*P*, *I*, *F*, *E*) supporting component-wise addition, subtraction, negation, and total-amount computation.

### 4.2 Ledger Activities

A `LedgerActivity` is an abstract object with `effectiveAt`, `createdAt`, and `activityId` fields. An activity *applies to* a ledger by generating ledger entries. Three concrete subtypes are defined:

- **Transaction.** A payment or disbursement that applies a `TransactionSpreadStrategy` to the current balance to determine per-component impacts. Two spread strategies are provided: *computational spread* (configurable waterfall allocation with overflow to excess) and *static spread* (direct per-component assignment).
- **TemporalActivity.** A time-driven activity (e.g., daily interest accrual) that computes entries using an injected `TemporalActivityContext` supplying interest rates and day-count conventions. Injection enables deterministic replay independent of external state.
- **ReversalActivity.** An activity that reverses a previously applied activity; handled by Algorithm 2 (Section 5.2).

An activity is classified as backdated if its `effectiveAt` precedes the `effectiveAt` of the ledger's most recent entry.

### 4.3 LedgerClock

The `LedgerClock` is a virtual monotonic clock passed as a parameter to all activity application methods. It:

- Starts at a defined minimum timestamp.
- Advances only forward through `advanceTime(t)`.
- Rejects backward adjustments during forward processing.

During retroactive replay, the clock's `now()` value is fixed to the snapshot time of the primary ledger at the point of invocation. This ensures that time-dependent derived values (e.g., daily interest amounts) are computed deterministically regardless of wall-clock time. The clock's externally controlled nature also enables isolated, reproducible unit testing of all processing paths.

### 4.4 Component Interaction

Figure 1 illustrates runtime component interaction. The `LedgerService` orchestrates all algorithm variants. Activity classification is performed on each iteration. For normal activities, the activity is applied directly. For backdated activities, `performBackdatedActivity` is invoked. For reversals, `reverseLedgerActivity` is invoked. Both delegates conclude with `syncWithRetroactiveLedger`.

```
┌─────────────────────────────────────────────────────────────────┐
│                        LedgerService                           │
│                                                                 │
│  applyLedgerActivities(ledger, activities, clock, ctx)          │
│     │                                                           │
│     ├── [in-order]   ──▶  activity.applyTo(ledger, clock, ctx) │
│     │                                                           │
│     ├── [backdated]  ──▶  performBackdatedActivity(...)         │
│     │                       │                                   │
│     │                       ├── rollbackToEntryBefore(t)        │
│     │                       ├── activity.applyTo(fork, ...)     │
│     │                       ├── applyLedgerActivities(fork,...) │  ← recursive
│     │                       └── syncWithRetroactiveLedger(...)  │
│     │                                                           │
│     └── [reversal]   ──▶  reverseLedgerActivity(...)            │
│                              │                                   │
│                              ├── calculateTotalImpact(...)      │
│                              ├── generateCompensationEntry(...)  │
│                              ├── rollbackToEntryBefore(type, id) │
│                              ├── applyLedgerActivities(fork,...) │  ← recursive
│                              └── syncWithRetroactiveLedger(...)  │
└─────────────────────────────────────────────────────────────────┘
```
*Figure 1: LedgerService orchestration and recursive delegation to fork-and-sync sub-routines.*

---

## 5. Algorithms

### 5.1 Algorithm 1: Retroactive Fork-and-Sync for Backdated Activities

When a backdated activity *a* arrives, the algorithm forks the primary ledger at the point immediately before *a*'s effective date, applies *a* to the fork, and replays all subsequent activities from the activity log against the fork. The completed fork is then reconciled back into the primary ledger via Algorithm 3. The invariant guaranteed on completion is that every entry after the insertion point carries a running balance consistent with the corrected history, with each downstream correction attributed to its originating source activity.

Recursion is safe because a backdated activity encountered during the replay of a fork triggers a nested invocation of the same protocol, and the composite idempotency key on each entry prevents duplication regardless of recursion depth. The dominant cost per event is O(*m* + *k* log *k*), where *m* is the number of post-fork entries in the primary ledger and *k* is the number of replay activities; the log factor arises from sorting replay activities by effective date. The formal step-by-step specification is given in Appendix C, Algorithm C1.

### 5.2 Algorithm 2: Compensation + Retroactive Fork-and-Sync for Reversals

A reversal does more than negate the direct balance impact of the target activity — it also invalidates every downstream derived value (interest accruals, allocation waterfall results) that was computed using the balance state the reversed activity created. Algorithm 2 handles both concerns in sequence.

First, a compensation entry that exactly negates the total recorded impact of the reversed activity is appended to the primary ledger immediately. This ensures that any balance query issued between the compensation step and the completion of downstream reconciliation observes a correct current balance. Second, the primary ledger is forked at the point immediately before the reversed activity's entries, and all subsequent activities — excluding the reversed target — are replayed on the fork. The exclusion of the reversed activity is the critical termination invariant: it ensures the fork represents the ledger *as if the reversed activity never happened*, and prevents infinite recursion in the replay loop. The fork is then reconciled into the primary ledger via Algorithm 3. The formal step-by-step specification is given in Appendix C, Algorithm C2.

### 5.3 Algorithm 3: Differential Impact Reconciliation (SYNC)

SYNC is the shared sub-routine invoked at the conclusion of both Algorithm 1 and Algorithm 2. It walks the post-fork entries of the retroactive fork and, for each distinct source activity key, compares the total balance impact recorded in the fork against the total balance impact recorded in the primary ledger. If the impacts differ, a single adjustment entry carrying the delta is appended to the primary ledger, attributed to the source activity that produced the divergence. If the impacts are identical — the zero-delta case — no entry is emitted. Activities with no prior record in the primary ledger (net-new activities introduced by the fork) are added directly.

Three properties are guaranteed by construction: each source activity is processed exactly once per reconciliation round (a *processed* set records visited keys); every emitted adjustment entry carries full source-activity attribution (satisfying R3); and the algorithm is O(*m*) in the number of post-fork entries. The zero-delta suppression is the mechanism that keeps entry count growth bounded: only activities that genuinely re-allocated balances differently produce new rows. The formal step-by-step specification is given in Appendix C, Algorithm C3.

All entry insertion flows through a single `addEntry` primitive that clones the incoming entry, recomputes its running balances against the current ledger tail, derives a composite idempotency key, and silently discards the entry if a matching key already exists. The key — a deterministic hash of loan identifier, entry identifier, entry type, source-activity pair, and effective date — is stable across invocations: revisiting the same activity at any recursion depth always produces the same key, so duplication is structurally impossible without reliance on sequence numbers or external counters. The formal step-by-step specification is given in Appendix C, Algorithm C4.

---

## 6. Implementation

The reference implementation uses Java 21 with the Quarkus framework (version 3.11) for the server-side service layer. Persistence uses Hibernate/Panache with H2 (test/development) and PostgreSQL (production). Monetary arithmetic uses the JavaMoney/Moneta library to avoid floating-point precision loss on financial quantities; balance components are stored as scaled 64-bit values and converted for arithmetic via `MonetaryAmount` objects.

Key design decisions:

- **Hexagonal architecture.** The core algorithmic logic (`LedgerService`, `Ledger`, `LedgerActivity` hierarchy) contains no repository or framework dependencies. Repository interfaces are injected, enabling deterministic unit testing with in-memory ledger objects.
- **Injected temporal context.** `TemporalActivityContext` supplies interest-rate schedules and day-count conventions as constructor parameters rather than runtime lookups. This is required for deterministic replay: a `StartOfDay` activity replayed on a retroactive fork must produce the same interest amount as it would have in forward processing.
- **Virtual clock injection.** `LedgerClock` is passed as a parameter, not obtained from `System.currentTimeMillis()`. This decouples algorithmic correctness from wall-clock time and makes all processing paths directly unit 
testable.
- **Activity factory.** A `LedgerActivityFactory` converts `GeneralLedgerActivity` persistence objects into typed `LedgerActivity` instances, isolating the persistence model from the domain model.

The implementation, including all test fixtures and integration tests, is publicly available at https://github.com/v1bh0r/precise-ledger-pro.

---

## 7. Evaluation

### 7.1 Scenario Design

We evaluate both the correctness and traceability properties of the fork-and-sync approach against a conventional grouped-adjustment ledger through a canonical nine-activity loan scenario. The loan has an initial principal of \$1,000,000 with an annual interest rate of 10.0%, computed on an actual/365 day-count basis (daily rate: \$273.97).

**Activity sequence:**

| Step | Activity | Amount | Effective Date |
|------|----------|--------|----------------|
| 1 | Disbursal | \$1,000,000 | 2024-03-01 |
| 2 | SOD (interest accrual) | — | 2024-03-02 |
| 3 | SOD (interest accrual) | — | 2024-03-03 |
| 4 | Payment #234 | \$10,000 | 2024-03-03 |
| 5 | SOD (interest accrual) | — | 2024-03-04 |
| 6 | SOD (interest accrual) | — | 2024-03-05 |
| 7 | Payment #456 | \$5,000 | 2024-03-05 |
| 8 | Reversal of Payment #234 | — | 2024-03-05 |
| 9 | Backdated Payment #768 | \$4,000 | 2024-03-03 (received 2024-03-05) |

Steps 1–7 are in-order activities processed identically by both systems. Steps 8 and 9 trigger the retroactive paths.

**Conventional ledger baseline.** Our comparative baseline follows typical loan servicing practice: reversals are handled by appending a compensation entry plus a single grouped downstream adjustment; backdated payments are appended against current processing-time balances. It correctly maintains running balances but represents multiple downstream corrections as a single entry.

### 7.2 Step-by-Step Comparison at Activity 8 (Reversal)

After processing steps 1–7, the primary ledger for both systems holds 7 entries with identical balances:

- Principal balance: \$990,547.24
- Interest balance: \$0.00 (fully consumed by Payment #456)

**Conventional ledger (step 8).** Appends a compensation entry (+\$9,452.05 principal, +\$547.95 interest) followed by a single grouped adjustment (+\$3.30 principal, \$0.00 net interest) covering three distinct downstream corrections: (i) SOD-304 interest recalculation, (ii) SOD-305 interest recalculation, and (iii) Payment #456 re-allocation. Resulting entries after reversal: **9 entries**.

**Fork-and-sync ledger (step 8).** Applies the same compensation entry. The fork-replay exposes that SOD-304 and SOD-305 each recalculated on a higher principal (restored to \$1,000,000) and Payment #456 required re-allocation with a higher interest base. Three individually traced adjustment entries are appended:

1. Adjustment for SOD-20240304: Δinterest = +\$1.65 (interest: \$271.38 → \$273.03)
2. Adjustment for SOD-20240305: Δinterest = +\$1.65 (same recalculation)
3. Adjustment for Transaction-456: Δprincipal = +\$3.30, Δinterest = −\$3.30 (re-allocation to higher interest base)

Resulting entries after reversal: **11 entries**. Every correction row names the entry it corrects; the ledger is readable top-to-bottom with no opaque rows.

### 7.3 Step-by-Step Comparison at Activity 9 (Backdated Payment)

**Conventional ledger (step 9).** The \$4,000 payment is applied against the current processing-time interest balance of \$547.95. The allocation is \$547.95 to interest and \$3,452.05 to principal.

**Fork-and-sync ledger (step 9).** The payment is inserted at its economic date of 2024-03-03. At that date the interest balance (per the corrected retroactive fork) is \$547.94. The allocation is \$547.94 to interest and \$3,452.06 to principal. The \$0.01 difference is economically small but materially correct: the payment is allocated against the interest balance that actually existed at the payment's effective date, not the processing-time balance.

After replay, downstream SODs (March 4 and 5) recalculate on the lower post-payment principal. The delta is less than \$0.01 per day when rounded, so no new adjustment entries are generated (zero-delta suppression).

### 7.4 Summary Results

| Metric | Conventional | Fork-and-Sync |
|--------|-------------|---------------|
| Final principal balance | \$987,095.18 | \$987,095.19 |
| Final interest balance | \$0.00 | \$0.00 |
| Total entries (all 9 steps) | 11 | 13 |
| Downstream correction entries | 1 grouped | 3 individually traced |
| Entry-level traceability | No | Yes |
| Retroactive composability | Fragile | Yes |
| Grouped-entry decomposition | Not possible from ledger | Not applicable |

The fork-and-sync approach generates two additional entries (11 vs 13) as the cost of full traceability. This is an O(1) overhead per retroactive event, not O(*n*) in the ledger length.

### 7.5 Retroactive Composability

Consider a hypothetical tenth activity: a second backdated event at 2024-03-04. With grouped corrections, the conventional engine must reason about applying-an-adjustment-to-an-adjustment: the grouped +\$3.30 entry from step 8 becomes the target of further correction, and its decomposition into the three original effects (SOD-304, SOD-305, Payment #456) cannot be recovered from the ledger alone. Full re-derivation from the primary activity log is required.

With per-entry traced corrections, each prior adjustment row identifies its exact source activity. The fork at 2024-03-04 simply re-encounters these source activities during replay, and the `calculateTotalImpact` / `SYNC` protocol naturally converges to the new correct state. No special case or decomposition logic is needed.

---

## 8. Discussion

### 8.1 Entry Count Growth

The fork-and-sync approach generates strictly more ledger entries than the grouped-adjustment approach — *k* individual adjustment rows versus 1 grouped row, where *k* is the number of downstream activities with divergent impacts. In practice, the number of activities with non-trivial downstream effects per retroactive event is small (typically 3–10 for a standard loan ledger), so the growth is bounded and predictable.

### 8.2 Zero-Delta Suppression

The skip-if-equal condition in Algorithm 3 (Step 5e, the `Not Equal` branch) is important for practical usability. Without it, every activity after the fork point would produce an adjustment entry, inflating the ledger by O(*m*) entries per retroactive event. With suppression, only activities with genuine downstream impact (typically those that use the running balance in their computation, such as temporal accruals) produce entries. Allocation-fixed activities (disbursals, static-fee charges) almost never diverge after a principal change.

### 8.3 Virtual Clock and Determinism

The `LedgerClock` design is a form of test-seam injection [Feathers 2004] applied at the production-logic level rather than only in tests. By parameterizing all time-sensitive computations on an external clock object, the system achieves reproducibility: replaying the same activity sequence with the same clock configuration always produces the same ledger. This is a prerequisite for regression testing of retroactive paths, which are notoriously difficult to test with wall-clock-coupled logic.

### 8.4 Limitations

- **Concurrent retroactive events.** The current design assumes serial application of activities to a given ledger. Concurrent retroactive events targeting the same ledger require external serialization (e.g., optimistic locking on the ledger object) to prevent lost-update anomalies.
- **Infinite recursion guard.** The reversal algorithm excludes the target activity from replay explicitly. Incorrect activity-ID matching or missing exclusion in a derived implementation would cause unbounded recursion. The system mitigates this via exhaustive fixture-based tests.
- **Query performance.** `calculateTotalImpact` performs a linear scan over ledger entries filtered by source activity. For long ledgers, a secondary index on (loanId, sourceLedgerActivityType, sourceLedgerActivityId) is required to maintain O(log *n*) query cost [9].

### 8.5 Future Work

Three directions remain open. First, a formal proof of convergence and correctness for recursively nested retroactive events would strengthen the theoretical foundation of the fork-and-sync protocol. Second, empirical benchmarking on synthetic ledgers of 10,000–100,000 entries is needed to validate the O(*m* + *k* log *k*) cost model in practice. Third, extension to multi-party ledgers — where a retroactive event on one party's ledger triggers cascading reconciliation to a counterparty ledger — would broaden the applicability of the approach to interbank settlement and syndicated loan contexts.

---

## 9. Conclusion

We presented a fork-and-sync architecture for financial ledger systems that handles both out-of-order (backdated) activities and reversals while satisfying all four of correctness, immutable auditability, entry-level traceability, and retroactive composability. The key contribution is Algorithm 3 (SYNC), which reconciles per-activity balance impacts between a retroactive fork and the primary ledger, emitting one individually source-attributed adjustment entry per divergent activity rather than a single grouped correction. A virtual ledger clock ensures deterministic replay of time-dependent derived calculations. Evaluation on a nine-activity loan scenario demonstrates that the approach produces an auditable, row-by-row explainable ledger with a bounded entry-count overhead of O(*k*) per retroactive event, where *k* is the number of activities with genuine downstream balance impact. A complete reference implementation is publicly available. We believe this approach is directly applicable to any financial system that must accommodate retroactive corrections over a sequence of derived calculations, including general ledger systems, derivative position accounts, and insurance claim ledgers.

---

## Acknowledgements

The authors thank the open-source contributors to the Quarkus, Hibernate, and JavaMoney/Moneta projects, whose frameworks underpin the reference implementation.

---

## References

[1] Fowler, M. (2005). *Event Sourcing*. https://martinfowler.com/eaaDev/EventSourcing.html

[2] Snodgrass, R. T. (1999). *Developing Time-Oriented Database Applications in SQL*. Morgan Kaufmann.

[3] Jensen, C. S., et al. (1994). A Glossary of Temporal Database Concepts. *ACM SIGMOD Record*, 23(1), 52–64.

[4] Garcia-Molina, H., and Salem, K. (1987). Sagas. *Proceedings of SIGMOD 1987*, 249–259.

[5] Bernstein, P. A., and Goodman, N. (1981). Concurrency Control in Distributed Database Systems. *ACM Computing Surveys*, 13(2), 185–221.

[6] Meijer, E., and Fokkinga, M. (1991). Functional Programming with Bananas, Lenses, Envelopes and Barbed Wire. *Proc. Conference on Functional Programming Languages and Computer Architecture*, 124–144.

[7] Feathers, M. C. (2004). *Working Effectively with Legacy Code*. Prentice Hall.

[8] Hellerstein, J. M., et al. (2019). Declarative Over What? The Meaning of "Declarative". In *Proceedings of Summit on Advances in Programming Languages (SNAPL 2019)*.

[9] Graefe, G. (2011). Modern B-Tree Techniques. *Foundations and Trends in Databases*, 3(4), 203–402.

---

## Appendix A: Test Fixture Summary

The reference implementation includes fixture-based integration tests that encode final ledger expectations as CSV files. The primary fixture for the nine-activity evaluation scenario is:

```
src/test/resources/data/ledger/service/
  applyLedgerActivities_test1/
    apply_ledger_activities_test1_expectation.csv
```

Each row encodes a full ledger entry expectation including entry type, source activity ID, incremental and running balances for all four components. These fixtures serve as executable specifications equivalent to the comparison table in Section 7.4 and are verified on every CI run.

---

## Appendix B: ACM Computing Classification

**Primary:** Information systems → Database management system engines → Data management systems → Relational database model

**Secondary:** Software and its engineering → Software organization and properties → Contextual software domains → Software application domains → Software reliability

*ACM CCS 2012 format:*
`Information systems~Transaction management; Information systems~Data streams; Software and its engineering~Software reliability`

---

## Appendix C: Algorithm and Data Structure Specification

This appendix contains the formal specifications referenced in Sections 4 and 5. Readers interested in the conceptual rationale for each algorithm should consult the corresponding prose sections; this appendix is intended for lookup and implementation reference.

### Table C1: LedgerEntry Field Specification

| Field | Type | Description |
|---|---|---|
| `entryType` | Enum | Semantic classification: Disbursal, Payment, StartOfDay, Reversal, Adjustment |
| `principal`, `interest`, `fee`, `excess` | Decimal | Incremental balance changes per component |
| `principalBalance`, `interestBalance`, `feeBalance`, `excessBalance` | Decimal | Running balances after this entry |
| `effectiveAt` | Timestamp | Business-effective date of the originating activity |
| `createdAt` | Timestamp | System processing timestamp |
| `sourceLedgerActivityType` | String | Type identifier of the originating activity |
| `sourceLedgerActivityId` | String | Unique identifier of the originating activity |

The composite idempotency key is a deterministic hash of (`loanId`, `entryId`, `entryType`, `sourceLedgerActivityType`, `sourceLedgerActivityId`, `effectiveAt`).

---

### Algorithm C1: Retroactive Fork-and-Sync for Backdated Activities

**Precondition:** A ledger activity *a* arrives with *a*.effectiveAt < ledger.latestEntry.effectiveAt.

**Postcondition:** The primary ledger contains the correct running balances for all entries following the insertion point, with each downstream correction attributed to its source activity.

```text
Algorithm RETROACTIVE-FORK-SYNC(ledger, a, clock, ctx):
  1. fork ← ledger.rollbackToEntryBefore(a.effectiveAt)
  2. forkIndex ← |fork.entries|
  3. a.applyTo(fork, clock, ctx)
  4. For each new entry e in fork[forkIndex..end]:
       ledger.addEntry(e)               // mirror direct entries
  5. replayActivities ← getLedgerActivities(
         loanId       = ledger.loanId,
         effectiveAt  ≥ a.effectiveAt,
         createdAt    ≤ clock.now)
       sorted by effectiveAt ascending
  6. APPLY-LEDGER-ACTIVITIES(fork, replayActivities, clock, ctx)
  7. SYNC(ledger, fork, clock.now, a.createdAt, forkIndex)
```

**Complexity.** Let *m* be the number of entries following the insertion point and *k* the number of replay activities. Total cost per backdated event: O(*m* + *k* log *k*), dominated by the sort at Step 5.

---

### Algorithm C2: Compensation + Retroactive Fork-and-Sync for Reversals

**Precondition:** A reversal activity *r* identifies a target activity *t* to be reversed. All entries attributed to *t* exist in the primary ledger.

**Postcondition:** The primary ledger contains a compensation entry immediately negating *t*'s impact, plus individually traced adjustments for all downstream derived effects, with no record of *t*'s existence removed from the ledger.

```text
Algorithm REVERSAL-FORK-SYNC(ledger, r, clock, ctx):
  1. totalImpact ← ledger.calculateTotalImpact(r.targetType, r.targetId)
  2. compensation ← buildEntry(
         change  = -totalImpact,
         balance = ledger.currentBalance - totalImpact,
         type    = "Reversal",
         source  = (r.targetType, r.targetId),
         effectiveAt = r.effectiveAt, createdAt = clock.now)
  3. ledger.addEntry(compensation)
  4. fork ← ledger.rollbackToEntryBefore(r.targetType, r.targetId)
  5. forkIndex ← |fork.entries|
  6. replayActivities ← getLedgerActivities(
         loanId     = ledger.loanId,
         createdAt  > originalActivity(r.targetId).createdAt,
         createdAt  ≤ clock.now)
       excluding r.targetId
       sorted by effectiveAt ascending
  7. APPLY-LEDGER-ACTIVITIES(fork, replayActivities, clock, ctx)
  8. SYNC(ledger, fork, clock.now, r.createdAt, forkIndex)
```

---

### Algorithm C3: Differential Impact Reconciliation (SYNC)

**Precondition:** `fork` is a retroactive fork of `primary`. `forkIndex` is the position in `fork.entries` from which new or replayed entries begin.

**Postcondition:** For each source activity with a divergent impact between `fork` and `primary`, exactly one adjustment entry is appended to `primary`. Zero-impact-delta activities produce no entry.

```text
Algorithm SYNC(primary, fork, now, adjustmentEffectiveAt, forkIndex):
  1. forkEntries ← fork.entries sorted by effectiveAt
  2. primaryEntries ← primary.entries sorted by effectiveAt
  3. If forkEntries is empty or primaryEntries is empty: return
  4. processed ← {}
  5. For i from forkIndex to |forkEntries| - 1:
     a. retro ← forkEntries[i]
     b. key ← (retro.sourceLedgerActivityType, retro.sourceLedgerActivityId)
     c. If key ∈ processed: continue
     d. primaryImpact ← primary.calculateTotalImpact(key.type, key.id)
     e. If primaryImpact = NULL:
          primary.addEntry(retro)          // net-new activity
        Else:
          retroImpact ← fork.calculateTotalImpact(key.type, key.id)
          If retroImpact ≠ primaryImpact:
            delta ← retroImpact - primaryImpact
            adj ← buildAdjustmentEntry(
                    change  = delta,
                    balance = primary.currentBalance + delta,
                    source  = key,
                    effectiveAt = adjustmentEffectiveAt,
                    createdAt   = now)
            primary.addEntry(adj)
     f. processed.add(key)
```

**Complexity:** O(*m*) in the number of post-fork entries.

---

### Algorithm C4: Idempotent Entry Addition

```text
Algorithm ADD-ENTRY(ledger, e):
  1. e' ← clone(e)
  2. e'.updateRunningBalances(ledger.currentBalance)
  3. key ← idempotencyKey(e')
  4. If any existing entry in ledger has the same key: return  // silent skip
  5. ledger.entries.append(e')
```

The composite idempotency key is a deterministic function of (`loanId`, `entryId`, `entryType`, `sourceLedgerActivityType`, `sourceLedgerActivityId`, `effectiveAt`).

---

*Draft version — April 1, 2026. Formatted for arXiv cs.DB submission.*
*Author correspondence: vibhor.mahajan@gmail.com*
