package zprng

import zio.*
import zio.test.*
import zio.test.Assertion.*

object RandomMappingSpec extends ZIOSpecDefault:
  def spec = suite("RandomMappingSpec")(
    test("bytesToInt should correctly convert 4 bytes") {
      val bytes = Chunk(0x12.toByte, 0x34.toByte, 0x56.toByte, 0x78.toByte)
      assertTrue(RandomMapping.bytesToInt(bytes) == 0x12345678)
    },
    test("nextIntBounded should stay within bounds") {
      val bound                             = 10
      val mockNextBytes: Int => Chunk[Byte] = _ => Chunk.fill(4)(0.toByte)
      val result                            = RandomMapping.nextIntBounded(bound)(mockNextBytes)
      assertTrue(result >= 0 && result < bound)
    },
    test("bytesToDouble should map to [-1.0, 1.0]") {
      val zeroBytes = Chunk.fill(8)(0.toByte)
      val maxBytes  = Chunk.fill(8)(0xff.toByte)
      val d1        = RandomMapping.bytesToDouble(zeroBytes)
      val d2        = RandomMapping.bytesToDouble(maxBytes)
      assertTrue(d1 >= -1.0 && d1 <= 1.0) &&
      assertTrue(d2 >= -1.0 && d2 <= 1.0)
    }
  )
