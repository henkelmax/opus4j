# Opus4J

A Java wrapper for the [Opus Codec](https://opus-codec.org/) written in Rust using JNI.

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.opus4j</groupId>
  <artifactId>opus4j</artifactId>
  <version>2.0.3</version>
</dependency>

<repositories>
  <repository>
    <id>henkelmax.public</id>
    <url>https://maven.maxhenkel.de/repository/public</url>
  </repository>
</repositories>
```

**Gradle**

``` groovy
dependencies {
  implementation 'de.maxhenkel.opus4j:opus4j:2.0.3'
}

repositories {
  maven {
    name = "henkelmax.public"
    url = 'https://maven.maxhenkel.de/repository/public'
  }
}
```

### Example Code

**Encoding**

``` java
short[] rawAudio = ...;

// Creates a new encoder instance with 48kHz mono VOIP
OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);

// Sets the max payload size to 1500 bytes
encoder.setMaxPayloadSize(1500);

// Encodes the raw audio
byte[] encoded = encoder.encode(rawAudio);

// Resets the encoder state
encoder.resetState();

...

// Closes the encoder - Not calling this will cause a memory leak!
encoder.close(); 
```

**Decoding**

``` java
byte[] encodedAudio = ...;

// Creates a new decoder instance with 48kHz mono
OpusDecoder decoder = new OpusDecoder(48000, 1);

// Sets the frame size to 960 samples
// If this is not set properly, decoded FEC frames will have the wrong size
decoder.setFrameSize(960);

// Decodes the encoded audio
short[] decoded = decoder.decode(encodedAudio);

// Decode a missing packet with FEC (Forward Error Correction)
decoded = decoder.decodeFec();

// Resets the decoder state
decoder.resetState();

...

// Closes the decoder - Not calling this will cause a memory leak!
decoder.close();
```

## Credits

- [Opus](https://opus-codec.org/)
- [opus-rs](https://github.com/SpaceManiac/opus-rs)
- [jni-rs](https://github.com/jni-rs/jni-rs)