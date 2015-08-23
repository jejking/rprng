Rough Design Ideas
------------------

PRNG Core
---------

Basically we wrap a well-known PRNG from apache commons math in an actor.

That's OK, but if we can observe the output often enough we may be able to deduce the seed and thus predict the output.

So, we need to take some measures against this.

1. create a reasonable number of actors each wrapping a PRNG that is properly seeded (java.secure.Random)
2. route randomly across these. This may require extending the random router that comes with Akka.
3. reseed or recreate the actors at random intervals

-> allow reseed of underlying source???
-> we just need to be able to obtain some bytes...

Stream
------

We can expose this construction as a reactive stream of bytes, encapsulating the non-type safe akka stuff.

On top of the bytes, we can then assemble various other things:
* Java / Scala primitives

It should be possible to use the underlying library code to munge these to fall within a given range.

On top of the stream we can then construct collections of a given type and size:
* lists
* sets


Web API
-------

On top of that we can expose a web api that exposes the pseudo-randomness as a service.

* /stream - a stream of raw bytes from the underlying stream, chunked
* /chunk - a fixed size raw byte, standard size 1kB, other sizes as query param ?size=2048. Configurable limits
* /?collectionType/?primitiveType - list or set of byte, char, int, float, long, double.
  * parameters ?min ?max ?size ?count (number of such collections to produce in the output)






