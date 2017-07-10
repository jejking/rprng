Idea here is to try and build a very simple set of classes for creating a random PNG from a stream of random bytes.
 
 It would be a bit of an exercise to NOT create the full image in memory and then use the javax.image libraries to encode a PNG and then write that out.
 
 For this, we will *not* try and build a full set of capabilities for handling PNG - this isn't necessary for the purposes of the exercise.
 
 Definition - width * height.
 Output - RGBA PNG of corresponding pixels. 
 
 (Probably won't be possible to compute the content-length up front as the contents are random and we can't anticipate up front how much compression will be possible, if any).
 
 
For the streaming part, we want to write 4 channels: red, green, blue, alpha. (Color type 6).
We can write 8 or 16 bits per channel.
 
 
 Write:
 PNG signature
 IHDR chunk  image's width, height, bit depth, color type, compression method, filter method, and interlace method (13 data bytes total
 PLTE - not needed, we're going full random across RGBA
 IDAT - how many are we going to need?
 IEND
 
 (Any other ancilliary chunks can be left out)
 
 # Not Covered
 Any other filter than "none"
 Interlacing
 Progressive rendering
 Any color type other than 6
 Channels with varying sample depths