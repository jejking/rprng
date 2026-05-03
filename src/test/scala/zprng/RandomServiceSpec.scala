package zprng

import zio.*
import zio.test.*
import zio.test.Assertion.*

object RandomServiceSpec extends ZIOSpecDefault:
  def spec = suite("RandomServiceSpec")(
    test("nextBytes should produce consistent output from a fixed seed") {
      val key   = Chunk.fill(32)(0.toByte)
      val nonce = Chunk.fill(12)(0.toByte)
      for {
        service1 <- ZIO
          .service[RandomService]
          .provide(RandomService.fromSeed(key, nonce), EntropySource.live)
        service2 <- ZIO
          .service[RandomService]
          .provide(RandomService.fromSeed(key, nonce), EntropySource.live)
        bytes1 <- service1.nextBytes(10)
        bytes2 <- service2.nextBytes(10)
      } yield assertTrue(bytes1 == bytes2)
    },
    test("split should produce independent but deterministic service") {
      val key   = Chunk.fill(32)(0.toByte)
      val nonce = Chunk.fill(12)(0.toByte)
      for {
        service <- ZIO
          .service[RandomService]
          .provide(RandomService.fromSeed(key, nonce), EntropySource.live)
        child1 <- service.split
        child2 <- service.split
        bytes1 <- child1.nextBytes(10)
        bytes2 <- child2.nextBytes(10)
      } yield assertTrue(bytes1 != bytes2) // Different because streamIds differ
    }
  )
