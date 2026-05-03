package zprng

import zio.*
import zio.test.*
import zio.test.Assertion.*

object ChaChaCoreSpec extends ZIOSpecDefault:
  def spec = suite("ChaChaCoreSpec")(
    test("generateBytes should produce deterministic output for a given state") {
      val key   = Chunk.fill(32)(0.toByte)
      val nonce = Chunk.fill(12)(0.toByte)
      val state = RNGState(key, nonce, 0)

      val (bytes1, nextState1) = ChaChaCore.generateBytes(state, 64)
      val (bytes2, _)          = ChaChaCore.generateBytes(state, 64)

      assertTrue(bytes1 == bytes2) &&
      assertTrue(bytes1.size == 64) &&
      assertTrue(nextState1.counter == state.counter + 1)
    }
  )
