# ADR-001: Core RNG Handling

## Status

Accepted

## Context

We need a random number generation subsystem that is:

* Cryptographically sound in structure (CSPRNG-based)
* Deterministic when seeded (for testing and simulation)
* Composable and safe under concurrency
* Idiomatic in Scala 3 using ZIO (explicit effects, controlled state)
* Capable of parallel consumption without contention or correlation

Historical approaches (e.g., multiple PRNGs, actor-based mixing, ad hoc reseeding) introduce correlation, unpredictability in behaviour, and are difficult to reason about or audit.

We therefore require a design that is:

* Small and auditable
* Based on well-understood primitives
* Free of emergent behaviour

## Decision

### 1. Use ChaCha20 (IETF / 7539) via Bouncy Castle

We will use Bouncy Castle's `ChaCha7539Engine` as the core primitive.

Rationale:

* Widely analysed stream cipher
* Simple and fast on general-purpose hardware
* Suitable for building a DRBG

We explicitly avoid implementing ChaCha20 ourselves.

---

### 2. Single RNG state (no mixing of generators)

We model randomness as a single state machine:

```
RNGState(key, nonce, counter)
```

Rationale:

* Eliminates correlation between multiple generators
* Enables local reasoning about behaviour
* Avoids emergent complexity

---

### 3. Functional state management via ZIO Ref

State is held in:

```
Ref[RNGState]
```

Rationale:

* Atomic updates
* Safe concurrent access
* No locks or actors required

---

### 4. Stream splitting via key derivation

Splitting is implemented as:

```
childKey = SHA256(parentKey ++ streamId)
```

Each derived key defines a new independent RNG instance.

Rationale:

* Eliminates shared mutable state between streams
* Ensures computational independence
* Enables parallelism without contention

---

### 5. Tree-structured RNG

Splitting forms a hierarchy of RNGs.

Rationale:

* Mirrors functional composition
* Enables deterministic parallel computation
* Simplifies reasoning about independence

---

### 6. Reseeding via entropy injection + hashing

Reseeding is defined as:

```
newKey = SHA256(oldKey ++ entropy)
```

Entropy source:

* `SecureRandom`

Rationale:

* One-way mixing prevents entropy loss
* Avoids PRNG-to-PRNG coupling
* Maintains forward security properties (within practical limits)

---

### 7. Explicit entropy boundary

All entropy acquisition is effectful and encapsulated:

```
trait EntropySource
```

Rationale:

* Keeps impurity explicit
* Enables deterministic testing
* Supports substitution of entropy sources

---

### 8. Service + Stream dual API

We provide:

* Direct service API (e.g., `nextInt`)
* ZStream-based APIs

Rationale:

* Simplicity for common use cases
* Composability for streaming scenarios

---

### 9. Deterministic testing via fixed seeds

All tests must use fixed seeds.

Rationale:

* Reproducibility
* Reliable property-based testing

---

### 10. No actor-based or timing-based randomness

We explicitly reject:

* Actor-based mixing
* Timing-based entropy
* Cross-generator reseeding

Rationale:

* These introduce hidden structure and correlation
* They are not cryptographically sound constructions

---

## Consequences

### Positive

* Small, auditable implementation
* Strong conceptual clarity
* Safe concurrency model
* Deterministic behaviour when required
* No emergent or non-local behaviour

### Negative

* Less "clever" or exploratory
* Requires discipline around entropy handling
* Not a certified RNG for regulated use (e.g., gambling compliance)

---

## Alternatives Considered

### 1. Multiple PRNGs with mixing

Rejected due to:

* Correlation risks
* Lack of formal guarantees
* High complexity

### 2. Using SecureRandom directly

Rejected as primary design because:

* Limited control over state
* Not easily splittable
* Harder to model functionally

### 3. Implementing ChaCha20 manually

Rejected due to:

* Risk of subtle cryptographic errors
* Lack of auditability

---

## Related Decisions

* RNG implementation spec (rng-spec-001)

---

## Notes

This design deliberately favours:

> simplicity, explicitness, and well-understood primitives

over

> cleverness, emergent behaviour, or architectural novelty

This is a deliberate response to known failure modes in RNG system design.
