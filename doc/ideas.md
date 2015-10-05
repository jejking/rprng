Rough Design Ideas
------------------

PRNG Core
---------

Our fundamental assumption is a trait `RandomByteSource` that allows us to generate an array of random bytes. That functionality is abstract. Other methods, more or less derived from existing code, allow these bytes to be turned into other primitives.

The one implementation that we have is based on Apache Commons Math and injects instances of `RandomGenerator`.

We also allow the instances to be reseeded with (hopefully) high quality random seed.

In order to allow these to be accessed and used in a threadsafe manner, we wrap them in an Actor. This actor is also responsible for taking some initial counter measures against deriving the seed from observing the output. It is supplied with an instance of a trait `SecureRandomSeeder` which should supplies a high quality long (8 bytes) of random seed from Java `SecureRandom`'s `generateSeed` method which should gather entropy. This seeder is used to initialise the wrapped `RandomByteSource`. At a random interval within a configurable time window, the seeder is again called upon to generate seed and the result is sent back. This seed generation in the running system is scheduled by the akka scheduler but then executed in a future so as not to block the actor.'

A number of instances of these self-reseeding actors are placed behind a standard Akka random router in order to add another layer of obfuscation to the underlying PRNGs and their state.

Stream
------

Now, let's see if we can encapsulate this as a stream of ByteStrings or a stream of Ints that are derived from sending
messages to the router and streaming back the responses from the underlying PRNG actors.

The idea would be that we have a `Source[ByteString, Unit]` that we can plug into.


Web API
-------

On top of that we can expose a web api that exposes the pseudo-randomness as a service.

* /byte/stream - a stream of raw bytes from the underlying stream, chunked
* /byte/block - a fixed size block of raw bytes, standard size 1kB, other sizes as query param ?size=2048. Configurable limits
* /?primitiveType/?collectionType - list or set of byte, char, int, float, long, double.
  * parameters ?min ?max ?size ?count (number of such collections to produce in the output)
  * JSON encoded






