package com.jejking.rprng.rng.actors

import com.jejking.rprng.rng.Seed

/**
  * Defines the types that can be sent to an [[EightByteStringRngActor]] in addition
  * to [[Seed]].
  */
object Protocol {

  /**
    * Case object encapsulating a request for an [[com.jejking.rprng.rng.EightByteString]].
    */
  case object EightByteStringRequest

  /**
    * Instruction to the actor to ask for more seed to apply to the wrapped PRNG.
    */
  case object Reseed

  // errors
  sealed trait Error
  case object UnknownInputType extends Error
}
