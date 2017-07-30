Idea here is to try and build a very simple set of classes for creating a random PNG from a stream of random bytes.
 
 It would be a bit of an exercise to NOT create the full image in memory and then use the javax.image libraries to encode a PNG and then write that out.
 
 For this, we will *not* try and build a full set of capabilities for handling PNG - this isn't necessary for the purposes of the exercise.
 
 Definition - width * height.
 Output - RGBA PNG of corresponding pixels. 
 
 (Probably won't be possible to compute the content-length up front as the contents are random and we can't anticipate up front how much compression will be possible, if any).
 
 
For the streaming part, we want to write 4 channels: red, green, blue, alpha. (Color type 6).
We can write 8 or 16 bits per channel - but will use 8, giving 32 bits per pixel, or 4 bytes.
 
 
 Write:
 PNG signature
 IHDR chunk  image's width, height, bit depth, color type, compression method, filter method, and interlace method (13 data bytes total
 IDAT - deflate compressed stream of scanlines
 IEND
 
 # Graph Processing
 
 `ByteStringStage` will provide the random input. Create at `width` * 4.
 
 `ScanlineStage` consumes raw byte strings that represent random RGBA pixels for a line, prepends the filter byte.
 - this can be done by doing a map with the Png.scanline() function.
 
 
 `IdatGraphStage` consumes `height` scanlines and emits IDAT chunks.
 
 `PNG Stage`
 
 send the png signature and the header
 consume idats for as long as there are any
 when there no more idats, send the footer
 - 
 
 
 # Not Covered
 Any other filter than "none"
 Interlacing
 Progressive rendering
 Any color type other than 6
 Channels with varying sample depths