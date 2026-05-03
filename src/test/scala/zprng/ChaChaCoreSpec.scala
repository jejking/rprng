package zprng

import zio.*
import zio.test.*
import zio.test.Assertion.*

object ChaChaCoreSpec extends ZIOSpecDefault:
  def spec = suite("ChaChaCoreSpec")(
    test("generateBytes should produce deterministic output for a given state") {
      val key   = Chunk.fill(32)(0.toByte)
      val nonce = Chunk.fill(12)(0.toByte)
      val state = RNGState(key, nonce, 0, 0, 0)

      val (bytes1, nextState1) = ChaChaCore.generateBytes(state, 64)
      val (bytes2, _)          = ChaChaCore.generateBytes(state, 64)

      assertTrue(bytes1 == bytes2) &&
      assertTrue(bytes1.size == 64) &&
      assertTrue(nextState1.counter == state.counter + 1)
    },
    test("generateBytes should fail for invalid key length") {
      val state  = RNGState(Chunk.fill(16)(0.toByte), Chunk.fill(12)(0.toByte), 0, 0, 0)
      val result = scala.util.Try(ChaChaCore.generateBytes(state, 64))
      assertTrue(result.isFailure)
    },
    test("generateBytes should fail for invalid nonce length") {
      val state  = RNGState(Chunk.fill(32)(0.toByte), Chunk.fill(8)(0.toByte), 0, 0, 0)
      val result = scala.util.Try(ChaChaCore.generateBytes(state, 64))
      assertTrue(result.isFailure)
    },
    test("generateBytes should fail if counter would overflow") {
      val state =
        RNGState(Chunk.fill(32)(0.toByte), Chunk.fill(12)(0.toByte), Int.MaxValue, 0, 0)
      val result = scala.util.Try(ChaChaCore.generateBytes(state, 64))
      assertTrue(result.isFailure)
    },
    test("deriveKey and mixEntropy should use different domains") {
      val key   = Chunk.fill(32)(0.toByte)
      val input = Chunk.fill(32)(0.toByte)

      val splitKey  = ChaChaCore.deriveKey(key, input)
      val reseedKey = ChaChaCore.mixEntropy(key, input)

      assertTrue(splitKey != reseedKey)
    }
  )
