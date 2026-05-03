# AGENTS.md — Guidelines for Implementing the ZIO RNG System

## 1. Purpose

This document defines how coding agents must approach implementation of the RNG system described in:

* `docs/specs/spec-001-core-rng-handling.md`
* `docs/adrs/adr-001-core-rng-handling.md`

This is a **high-integrity system**. Correctness, simplicity, and auditability take precedence over cleverness or abstraction.

---

## 2. Core Principles (Non-Negotiable)

### 2.1 Do not invent randomness algorithms

* NEVER implement cryptographic primitives manually
* ALWAYS use:

  * Bouncy Castle `ChaCha7539Engine`
  * JDK `SecureRandom` for entropy

If unsure: **stop and ask**, do not improvise.

---

### 2.2 Prefer simplicity over abstraction

If you are choosing between:

* a simple, explicit implementation
* a generic, abstract, reusable one

→ choose the simple one

Avoid:

* unnecessary typeclasses
* over-generalised algebraic interfaces
* indirection without clear value

---

### 2.3 No emergent behaviour

The system must be:

* locally understandable
* deterministic given state

DO NOT introduce:

* multiple interacting RNGs
* timing-based behaviour
* actor-based concurrency

---

### 2.4 State is explicit and controlled

* All RNG state must live in `Ref[RNGState]`
* All updates must be via `Ref.modify`
* No hidden mutable state

---

### 2.5 Entropy is effectful

* All entropy acquisition must go through `EntropySource`
* Must be wrapped in ZIO effects
* Must be replaceable for testing


### 2.6 Prefer pure functions

All deterministic logic MUST be implemented as pure functions.

DO:

- implement transformations as plain functions
- test them without effects

DO NOT:

- wrap pure logic in `ZIO`
- introduce effects where none are required

---

## 3. Architecture Constraints

### 3.1 Single RNG per instance

Each `RandomService`:

* owns exactly one RNG state
* does not interact with other RNGs

---

### 3.2 Splitting via key derivation only

`split` MUST:

* derive a new key via SHA-256
* create a new independent RNG

It MUST NOT:

* share state
* copy mutable structures
* reseed using output

---

### 3.3 Reseeding rules

Reseeding MUST:

* use external entropy (`SecureRandom`)
* use one-way hash mixing:

  ```
  newKey = SHA256(oldKey ++ entropy)
  ```

Reseeding MUST NOT:

* use other RNG outputs
* depend on timing
* reduce entropy

---

## 4. ZIO Usage

### 4.1 Effects

* Use `UIO` for pure RNG operations
* Use `Task` only for entropy acquisition
* Pure functions MUST NOT return ZIO

If a function is deterministic, it must be pure.

---

### 4.2 Concurrency

* Use `Ref` for state
* No locks
* No actors

Parallelism is achieved via:

* stream splitting
* NOT shared mutable access

---

### 4.3 Streams

* Streams are adapters over the service
* Do not embed RNG logic inside streams

---

## 5. Testing Requirements

### 5.1 Tools

- ZIO Test (mandatory)
- ZIO Test `Gen` for property-based testing

Do NOT introduce ScalaTest as a primary framework.

### 5.2 TDD is mandatory

For every feature:

1. Write failing test
2. Implement minimal code to pass
3. Refactor safely

---

### 5.3 Determinism

All tests MUST:

* use fixed seeds
* produce identical output across runs

### 5.3a Pure vs Effectful Tests

* Pure functions must be tested without ZIO
* Effectful code must be tested with ZIO

DO NOT wrap pure logic in effects just to fit a test style.

Example (correct):

* `deriveKey(...)` → tested directly
* `nextBytes(...)` → tested via ZIO

---

### 5.4 Property-based testing

Use:

* ScalaTest + ScalaCheck

Test at least:

* determinism (same seed ⇒ same output)
* independence of split streams
* bounds correctness
* absence of bias (basic statistical sanity)

---

### 5.5 No flaky tests

If a test is non-deterministic:

* it is incorrect
* fix it, do not retry it

---

## 6. Code Style

* Scala 3 only
* Functional, idiomatic style
* Use `Chunk`, not raw arrays (except at boundaries)
* Use `ZLayer` for wiring
* Format with `scalafmt`

Avoid:

* nulls
* side effects outside ZIO
* mutable collections

---

## 7. Implementation Order (Suggested)

1. RNGState model
2. EntropySource
3. ChaCha-based generator
4. RandomService (core methods)
5. Split functionality
6. Reseeding
7. Derived primitives (`nextInt(bound)`, `nextDouble`)
8. ZStream adapters
9. Tests (expand coverage)
10. Demo applications

---

## 8. Demo Applications

Implement:

* German Lotto (6aus49 + Superzahl)
* EuroMillions
* Eurojackpot

Rules:

* Correct ranges
* No duplicates (use sorted sets)
* Purely demonstrative (not certified randomness)

---

## 9. What to Avoid (Common Failure Modes)

DO NOT:

* reintroduce multiple RNGs with interaction
* implement “random mixing”
* rely on scheduling or timing for entropy
* optimise prematurely
* over-engineer abstractions
* wrapping pure functions in ZIO unnecessarily

If something feels “clever”, it is likely wrong.

---

## 10. When in Doubt

Default to:

> smaller, simpler, more explicit code

And align with:

* docs/adrs/adr-001-core-rng-handling.md
* docs/specs/spec-001-core-rng-handling.md

If a decision contradicts those documents, it is incorrect.

---

## 11. Success Definition

The implementation is successful if:

* it is small and readable
* every state transition is obvious
* behaviour is deterministic under fixed seed
* no hidden coupling exists
* it is easy to explain to another engineer in a few minutes

---

## 12. Guiding Heuristic

> This system should feel almost boring.

If it does not, simplify it.
