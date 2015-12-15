# rprng

*A reactive pseudo-RNG web service built on commons math and akka*

## Run the server

You can start the web application from `sbt` or an IDE. For now we assume that the server is running on `localhost` on port 8080.

### Getting bytes

You can obtain bytes in two different styles:
* fixed size blocks
    * `http://localhost:8080/byte/block` will deliver 1024 bytes of pseudo-randomness
    * `http://localhost:8080/byte/block/${blockSize}` will deliver `${blockSize` bytes of pseudo-randomness. The path variable must be a positive integer greater than zero.
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
* `max`. The top bound (inclusive) of members of the collection(s) to return. Defaults to Java's `Integer.MAX_VALUE`. Must be an integer (in the Java sense). It must also (obviously) be greater than `min` and, if as a set is being requested, the span between `min` and `max` must be greater than `size`. (Set shuffling is not implemented yet).

A JSON object is returned and the collections are mapped as JSON arrays under the key `content`.    

## Design Overview

The program is built up in a relatively simple fashion, the main idea being to provide a modular design, to make it difficult to observer the service and draw conclusions about the PRNGs in use and to provide a service that draws on reactive principles to provide a robust, non-blocking service.

* `RandomSource`, a trait defining some simple methods to obtain a random array of bytes, integers, longs and to reseed
some underlying PRNG. It is assumed that implementations will use a deterministic PRNG.
* `SecureSeeder`, a trait defining a method to obtain a random long. The idea is that the seeding results from high-quality entropy to make it hard to predict how any subsequent output from a PRNG.
* `SecureRandomSeeder`, an implementation of `SecureSeeder` that uses Java's `SecureRandom.generateSeed()` method which will (normally) delegate to a good quality source of randomness such as `/dev/random`.
* `RandomGenerator` from Apache Commons Math. The web application uses the [IsaacRandom](http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/random/ISAACRandom.html) PRNG.
* `RandomGeneratorSource`, an implementation of `RandomSource` that wraps a `RandomGenerator` 
* `RandomSourceActor`, an actor that wraps a `RandomGeneratorSource` and ensures a number of things:
    * before starting, obtains and sets seed
    * schedules reseeding at a random interval between a configurable min and max duration which default to 1 hour and 8 hours respectively. As obtaining high quality entropy is blocking, obtaining seed is executed in another thread and the result sent, on completion, back to the actor
    * handles incoming messages, as an actor does:
        * request for given number of bytes
        * request for integer in a given range
        * new seed, in which case the underlying `RandomSource` is reseeded and a new reseeding is randomly scheduled
        * instruction to obtain new seed (sent by scheduler)
* random router over a configurable number of `RandomSourceActor` instances. This router is addressed when handling incoming requests.
* `RandomIntActorPublisher` - a reactive streams `Publisher` that is created to handle requests for collections of integers and exposes a stream of integers (of appropriately constrained range) obtained from the random router that delegates to the underlying `RandomSourceActor` instances
* `RandomByteStringActorPublisher` - which does the same for requests for bytes. 

Collections of integers are assembled using Akka Streams. A custom `PushPullStage` allows a stream of integers to be mapped to a stream of set of integers. Requests for lists are simply processed by obtaining a list and grouping it appropriately.

JSON mapping is carried out simply with Spray JSON.

## Configuration

Akka has not been specifically configured for the system except for some basic logging settings.

An Typesafe Config `application.conf` file has been supplied with some options for the port for the web listener, the number of `RandomSourceActor` instances to be created and the min and max times between actor reseeding.

The following optional environment variables can be set:
* `RPRNG_HTTP_PORT`, the http port
* `RPRNG_ACTOR_COUNT`, the number of `RandomSourceActor` instances to create
* `RPRNG_RESEED_MIN`, the minimum duration to a reseeding, must be a Typesafe config duration.
* `RPRNG_RESEED_MAX`, the maximum duration to a reseeding, must be a Typesafe config duration, and, longer than the minimum duration
    
## Load Testing

A Gatling scenario is provided in the `gatling` directory which can be tweaked as needed.