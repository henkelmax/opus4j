# Opus4J

A Java wrapper for the [Opus Codec](https://opus-codec.org/) written in C using JNI.

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.opus4j</groupId>
  <artifactId>opus4j</artifactId>
  <version>2.1.0</version>
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
  implementation 'de.maxhenkel.opus4j:opus4j:2.1.0'
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

// Sets the max packet loss percentage to 1% for in-band FEC
encoder.setMaxPacketLossPercentage(0.01F);

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
// If this is not set properly, decoded PLC/FEC frames will have the wrong size
decoder.setFrameSize(960);

// Decodes the encoded audio
short[] decoded = decoder.decode(encodedAudio);

// Decode a missing packet with PLC (Packet Loss Concealment)
decoded = decoder.decode(null);

// Decode a missing packet and the current packet with FEC (Forward Error Correction)
short[][] decodedFec = decoder.decode(encodedAudio, 2);

// Resets the decoder state
decoder.resetState();

...

// Closes the decoder - Not calling this will cause a memory leak!
decoder.close();
```

## Building from Source

### Prerequisites

- [Java](https://www.java.com/en/) 21
- [Zig](https://ziglang.org/) 0.14.1
- [Ninja](https://ninja-build.org/)

### Building

``` bash
./gradlew build
```

## Credits

- [Opus](https://opus-codec.org/)
