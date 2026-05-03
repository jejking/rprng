# ZIO-Based Cryptographically Sound RNG – Implementation Specification

## 1. Overview

Implement a **functional, cryptographically sound random number generation library** in **Scala 3**, using **ZIO**.

The system must:

* Provide a **CSPRNG-based random service**
* Support **deterministic stream splitting via key derivation**
* Expose both:

  * **direct APIs** (e.g. `nextInt`)
  * **ZStream-based APIs**
* Ensure **referential transparency where possible**
* Encapsulate all impurity (entropy acquisition) explicitly via ZIO effects

---

## 2. Core Requirements

### 2.1 RNG Model

The RNG must be implemented as:

* A **state machine**
* Backed by:

  * `Ref[RNGState]` (ZIO)

#### RNGState

```scala
final case class RNGState(
  key: Chunk[Byte],   // 32 bytes
  nonce: Chunk[Byte], // 12 bytes
  counter: Int
)
```

## 2.2 Pure vs Effectful Separation (CRITICAL)

The system must clearly separate:

### Pure functions (preferred)
All deterministic transformations MUST be implemented as pure functions:

- ChaCha-based byte generation (wrapped, referentially transparent)
- Key derivation (SHA-256)
- Reseed mixing
- Mapping functions (bytes → int, int → bounded int, etc.)
- Rejection sampling logic

These functions:
- must NOT return ZIO
- must be directly testable without effects

### Effectful components (restricted)

ZIO must ONLY be used for:

- RNG state management (`Ref[RNGState]`)
- Entropy acquisition (`EntropySource`)
- Service orchestration

Under no circumstances should pure logic be wrapped in effects unnecessarily.

---

## 3. Cryptographic Core

### 3.1 Primary Algorithm

Use:

* Bouncy Castle `ChaCha7539Engine`

Do NOT implement ChaCha manually.

### 3.2 Entropy Source

Use:

* `java.security.SecureRandom`

Encapsulate behind:

```scala
trait EntropySource {
  def nextBytes(n: Int): Task[Chunk[Byte]]
}
```

---

## 4. RNG Service API

```scala
trait RandomService {
  def nextBytes(n: Int): UIO[Chunk[Byte]]
  def nextInt: UIO[Int]
  def nextInt(bound: Int): UIO[Int]
  def nextDouble: UIO[Double] // [-1.0, +1.0]
  def reseed: UIO[Unit]
  def split: UIO[RandomService]
}
```

---

## 5. Stream Splitting (CRITICAL)

### 5.1 Definition

Splitting must be implemented via **key derivation**, not shared state.

### 5.2 Key Derivation

Use:

* SHA-256

```scala
childKey = SHA256(parentKey ++ streamId)
```

### 5.3 Properties

* Deterministic
* No shared mutable state
* Parent continues independently
* Child has independent state

---

## 6. Tree-Structured RNG

Support hierarchical splitting:

```scala
val child1 = rng.split
val child2 = rng.split
val grandchild = child1.split
```

Each node represents an independent RNG.

---

## 7. Reseeding

### 7.1 Mechanism

```scala
newKey = SHA256(oldKey ++ entropy)
```

### 7.2 Policy

Implement:

* Manual reseed (`reseed`)
* Optional automatic reseed:

  * after configurable byte threshold

---

## 8. Derived Primitives

Implement:

* `nextInt(bound)` using rejection sampling
* `nextDouble` mapped to [-1, +1]

Ensure:

* no modulo bias
* uniform distribution

---

## 9. ZStream Integration

Provide:

```scala
def randomBytesStream(chunkSize: Int): ZStream[RandomService, Nothing, Chunk[Byte]]
def randomIntStream: ZStream[RandomService, Nothing, Int]
```

---

## 10. Concurrency Model

* RNG state must be safe under concurrent access
* Use `Ref.modify`
* Do NOT use actors or locks

Parallelism achieved via:

* stream splitting

---

## 11. Testing Strategy

### 11.1 Tools

### 11.1 Tools

- ZIO Test (primary and only test framework)
- ZIO Test `Gen` for property-based testing

ScalaTest MUST NOT be used as a primary framework.

All development must follow a strict test-driven development (TDD) approach: write failing tests first, then implement the functionality to satisfy those tests, and finally refactor while keeping all tests passing.

### 11.2 Requirements

All tests must be:

* deterministic (fixed seed)
* reproducible

### 11.2a Pure Function Testing

Pure functions MUST:

- be tested without ZIO effects
- not be wrapped in `ZIO.succeed` or similar
- be tested using direct assertions

Example:

- key derivation
- bounded integer mapping
- rejection sampling

These tests must remain fully deterministic and fast.

### 11.3 Properties

Test:

* same seed ⇒ same output
* split streams are independent
* bounds correctness (`nextInt(bound)`)
* no bias (statistical sanity checks)

All property-based tests must:

- use deterministic generators (fixed seeds where applicable)
- avoid reliance on external entropy

---

## 12. Code Quality

* Scala 3
* idiomatic functional style
* no mutable shared state
* use `Chunk`, not arrays
* use `ZLayer` for wiring
* format with `scalafmt`

---

## 13. Demo Applications

Implement CLI demos:

### 13.1 German Lotto (6aus49)

* 6 unique numbers from 1–49 plus a Superzahl (single digit from 0–9)

### 13.2 EuroMillions

* 5 numbers (1–50)
* 2 stars (1–12)

### 13.3 Eurojackpot

* 5 numbers (1–50)
* 2 numbers (1–12)

### Requirements

* Use a sorted set to ensure uniqueness
* Correct ranges
* Print results to console

---

## 14. Non-Goals

Do NOT:

* implement custom cryptographic primitives
* introduce actor systems
* mix multiple RNGs
* rely on timing/scheduling for randomness

---

## 15. Deliverables

* Core RNG wrapper implementation
* Test suite
* Demo apps
* README explaining:

  * design
  * guarantees
  * limitations

---

## 16. Success Criteria

The system must be:

* small
* auditable
* deterministic under fixed seed
* free of emergent behaviour
* cryptographically defensible in structure

---

## 17. Guiding Principle

Prefer:

> simple, explicit, well-understood constructions

Over:

> clever composition or emergent behaviour
