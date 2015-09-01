package com.jejking.rprng.rng

import java.nio.ByteBuffer
import java.security.SecureRandom

import org.apache.commons.math3.random.RandomGenerator

import scala.reflect.{ClassTag, classTag}

/**
 * Provides functionality to instantiate various PRNG types and to supply them
 * with seed from a random source.
 *
 * @param secureRandom the source of underlying randomness
 */
class RandomByteSourceFactory(private val secureRandom: SecureRandom) {

  /**
   * Extracts the specified amount of seed from the underlying secure random. The method is
   * blocking.
   *
   * @param byteCount the number of bytes to generate
   * @return requested amount of entropy
   */
  def generateSeed(byteCount: Int): Array[Byte] = {
    secureRandom.generateSeed(byteCount)
  }

  /**
   * Seeds the supplied random generator with 8 bytes of randomness in the form of a long.
   * The method is side-effecting on the supplied parameter.
   *
   * @param randomGenerator the generator to seed
   * @return the random generator
   */
  def seedGenerator(randomGenerator: RandomGenerator): RandomGenerator = {
    val bytes = generateSeed(8)
    val wrapper = ByteBuffer.wrap(bytes)
    randomGenerator.setSeed(wrapper.getLong)
    randomGenerator
  }

  /**
   * Creates a new [[RandomGenerator]] instance of the specified concrete type.
   * @param tag implicit class tag, supplied by compiler
   * @tparam G the type. Note that this *must* have a zero arg constructor.
   * @return new instance, as yet unseeded.
   */
  def createNewGeneratorInstance[G <: RandomGenerator]()(implicit tag: ClassTag[G]): G = {
    tag.runtimeClass.newInstance().asInstanceOf[G]
  }

  /**
   * Creates a new [[CommonsMathRandomByteSource]] based on a new Random Generator instance of the type
   * specified (which must have a zero arg constructor) which is seeded with
   * @param tag implicit class tag, supplied by the compiler
   * @tparam G the type of generator to use. Note that this *must* have a zero arg constructor.
   * @return new instance wrapping a seeded generator instance of the specified type
   */
  def createRandomByteSource[G <: RandomGenerator]()(implicit tag: ClassTag[G]): RandomByteSource = {
    val generator: G = createNewGeneratorInstance[G]()
    this.seedGenerator(generator)
    CommonsMathRandomByteSource(generator)
  }

}
