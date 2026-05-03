# ZIO-Based Cryptographically Sound RNG

A functional, cryptographically sound random number generation library in Scala 3, using ZIO and Bouncy Castle.

## Features

- **CSPRNG-based**: Uses ChaCha20 (IETF / 7539) via Bouncy Castle.
- **Deterministic**: Fully deterministic when seeded, ideal for testing and simulations.
- **Composable & Safe**: Managed via ZIO `Ref` for atomic updates and safe concurrency.
- **Stream Splitting**: Supports hierarchical splitting via key derivation (SHA-256).
- **ZStream Integration**: Easy to use with ZIO Streams.
- **High Integrity**: Strict separation of pure logic and effectful state management.

## Design

The system follows a state machine model:
- `RNGState(key, nonce, counter)`: Encapsulates the ChaCha20 state.
- `RandomService`: The ZIO service interface.
- `EntropySource`: Abstracted source for external entropy (using `java.security.SecureRandom`).

### Guarantees
- **No Modulo Bias**: Uses rejection sampling for bounded integers.
- **Independence**: Split streams are computationally independent via one-way hashing.
- **Thread Safety**: Safe for concurrent access without locks or actors.

## How to Run Demos

The project includes three lotto demo applications:

1. **German Lotto (6aus49 + Superzahl)**:
   ```bash
   sbt "runMain zprng.demos.GermanLotto"
   ```

2. **EuroMillions**:
   ```bash
   sbt "runMain zprng.demos.EuroMillions"
   ```

3. **Eurojackpot**:
   ```bash
   sbt "runMain zprng.demos.Eurojackpot"
   ```

## Development

### Requirements
- Scala 3.8.3
- sbt 1.12.11

### Commands
- Compile: `sbt compile`
- Test: `sbt test`
- Format: `sbt scalafmtAll`
- Check Formatting: `sbt scalafmtCheckAll`

## Implementation Details

- **Core Algorithm**: Bouncy Castle `ChaCha7539Engine`.
- **Key Derivation**: SHA-256 of parent key and a generated stream ID.
- **Reseeding**: `newKey = SHA256(oldKey ++ entropy)`.
- **Pure Functions**: Mapping and cryptographic logic are implemented as pure functions for auditability and ease of testing.
