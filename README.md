# rprng

[![Build Status](https://travis-ci.org/jejking/rprng.svg?branch=master)](https://travis-ci.org/jejking/rprng)

*A reactive pseudo-RNG web service built on commons math and akka*

## Run the server

You can start the web application interactively with `sbt` or an IDE. For now we assume that the server is running on `localhost` on port 8080.

`sbt assembly` will produce a fat JAR that can be run directly from the command line using `java`.

## API

### Getting bytes

You can obtain bytes in two different styles:
* fixed size blocks
    * `http://localhost:8080/byte/block` will deliver 1024 bytes of pseudo-randomness
    * `http://localhost:8080/byte/block/${blockSize}` will deliver `${blockSize}` bytes of pseudo-randomness. The path variable must be a positive integer greater than zero.
* stream
    * `http:localhost:8080/byte/stream` will deliver a continuous stream of randomness in chunks of 1024 bytes until the connection is closed

In both cases, the content type is `application/octet-stream`.    
    
### Getting integers 

You can also obtain collections of integers grouped in either lists or sets.
* `http://localhost:8080/int/list`
* `http://localhost:8080/int/set`

In both cases, the following query parameters may be specified:
* `size`. The size of the collection to return. Must be a positive integer greater than zero. Defaults to 100.
* `count`. The number of collections of the specified size to return. Must be a positive integer greater than zero. Defaults to 1.
* `min`. The lowest bound (inclusive) of members of the collection(s) to return. Defaults to Java's `Integer.MIN_VALUE`. Must be an integer (in the Java sense).
* `max`. The top bound (inclusive) of members of the collection(s) to return. Defaults to Java's `Integer.MAX_VALUE`. Must be an integer (in the Java sense). It must also (obviously) be greater than `min` and, if a set is being requested, the span between `min` and `max` must be greater than `size`. (Set shuffling is not implemented yet).

A JSON object is returned and the collections are mapped as JSON arrays under the key `content`.    

## Configuration

Akka has not been specifically configured for the system except for some basic logging settings.

An Typesafe Config `application.conf` file has been supplied with some options for the port for the web listener, the number of `RandomSourceActor` instances to be created and the min and max times between actor reseeding.

The following optional environment variables can be set:
* `RPRNG_HTTP_PORT`, the http port
* `RPRNG_ACTOR_COUNT`, the number of `RandomSourceActor` instances to create
* `RPRNG_RESEED_MIN`, the minimum duration to a reseeding, must be a Typesafe config duration.
* `RPRNG_RESEED_MAX`, the maximum duration to a reseeding, must be a Typesafe config duration, and, longer than the minimum duration

## Writing Randomness to stdout

If you want to assess the quality of randomness produced by the application, you can run `RandomBytesToStandardOut` which will start up an actor system and stream chunks of 512 bytes to standard out until the program is terminated.

This stream can be used, for example, as an input to programs such as [dieharder](http://www.phy.duke.edu/~rgb/General/dieharder.php).

First, create the fat jar using `sbt assembly`. Then, start the application and pipe its output to dieharder, for example:

```
java -cp rprng-assembly-1.0.13-SNAPSHOT.jar com.jejking.rprng.main.RandomBytesToStandardOut | dieharder -a -g 200

```

The usual caveats apply when using `dieharder` or similar tools to assess randomness. But when I've run it it *appears* to be quite reasonable.

## Design Overview

The application is relatively simple in its design which is intended to provide two essential qualities:
 * it should be difficult to observe the running service and draw conclusions about the state of the underlying PRNGs and hence to predict future output
 * it should be reasonably performant and provide a robust, non-blocking service
 
At the heart of the system is the `Rng` trait which defines methods to obtain a random array of bytes and to reseed some underlying PRNG. There is currently just one regular implementation (`CommonsMathRng`), which wraps an instance of `RandomGenerator` from Apache Commons math. The web application uses the [IsaacRandom](http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/random/ISAACRandom.html) as its underlying PRNG implementation.

As the Apache `RandomGenerator` instances are not thread-safe, the `Rng` instances are managed by the `RngActor`. The external API for the actor is simply `RandomByteRequest` messages which are responded to using Akka `ByteStrings`. 

The actor not only provides thread-safety guarantees, it also takes responsibility for ensuring that the underlying RNG is properly seeded - and then reseeded at unpredictable intervals. The provision of seed is defined in the `SecureSeeder` trait and the standard implementation is `SecureRandomSeeder` which essentially wraps  Java's `SecureRandom.generateSeed()` method which will (normally) delegate to a good quality source of randomness such as `/dev/random`. Secure seed is obtained during actor startup. The actor uses the Akka scheduling system to schedule reseeding within a configurable interval. As obtaining high quality entropy is blocking, obtaining seed is executed in another thread and the result sent, on completion, back to the actor.

The standard application setup creates a configurable number of `RngActor` instances on start up and puts them behind a `RandomRouter` instance which helps to further obfuscate the overall observable state of the PRNGs. 

Now, the Akka HTTP API which we are using to expose the service to the web, is streams based. It therefore makes sense to expose the RNGs as a stream. To that effect requests create `ByteStringSource` instances which interact with the actor router and expose a type-safe stream of `ByteString` instances of the requested size. In order to create `Int`s and other types there is a further wrapper class `EightByteString` (for eight bytes, aka 64 bits) and conversion functions exposed on `EightByteStringOps`.

The stream processing that serves the HTTP API is in the `AkkaRoutingHelper`. The class `ToSizedSet`, a custom `GraphStage`, allows sets of integers of a given size to be produced from a stream of integers.

JSON mapping of `RandomIntegerCollectionResponse` instances is carried out simply with Spray JSON.

    
## Load Testing

A Gatling scenario is provided in the `gatling` directory which can be tweaked as needed.
