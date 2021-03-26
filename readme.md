# Opus4J

A Java wrapper for the [Opus Codec](https://opus-codec.org/).

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.opus4j</groupId>
  <artifactId>opus4j</artifactId>
  <version>1.0.0</version>
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
  implementation 'de.maxhenkel.opus4j:opus4j:1.0.0'
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
IntBuffer error = IntBuffer.allocate(1);
PointerByReference opusEncoder = Opus.INSTANCE.opus_encoder_create(sampleRate, 1, application, error);
if (error.get() != Opus.OPUS_OK && opusEncoder == null) {
  // Handle the error
}

...

ShortBuffer nonEncodedBuffer = ...;
nonEncodedBuffer.flip();
ByteBuffer encoded = ByteBuffer.allocate(maxPayloadSize);
int result = Opus.INSTANCE.opus_encode(opusEncoder, nonEncodedBuffer, frameSize, encoded, encoded.capacity());

if (result < 0) {
  // Handle the error
}

byte[] audio = new byte[result];
encoded.get(audio);

...

Opus.INSTANCE.opus_encoder_destroy(opusDecoder); // Destroys the encoder instance
```

**Decoding**

``` java
IntBuffer error = IntBuffer.allocate(1);
PointerByReference opusDecoder = Opus.INSTANCE.opus_decoder_create(sampleRate, 1, error);
if (error.get() != Opus.OPUS_OK && opusDecoder == null) {
  // Handle the error
}

...

int result;
ShortBuffer decoded = ShortBuffer.allocate(maxPayloadSize);
if (data == null || data.length == 0) {
    result = Opus.INSTANCE.opus_decode(opusDecoder, null, 0, decoded, frameSize, 0);
} else {
    result = Opus.INSTANCE.opus_decode(opusDecoder, data, data.length, decoded, frameSize, 0);
}

if (result < 0) {
  // Handle the error
}

short[] audio = new short[result];
decoded.get(audio);

...

Opus.INSTANCE.opus_decoder_destroy(opusDecoder); // Destroys the decoder instance
```