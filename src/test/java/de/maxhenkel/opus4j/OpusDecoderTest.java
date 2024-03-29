package de.maxhenkel.opus4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OpusDecoderTest {

    @Test
    @DisplayName("Encode")
    void encode() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        byte[] encoded = encoder.encode(new short[960]);

        OpusDecoder decoder = new OpusDecoder(48000, 1);
        short[] decoded = decoder.decode(encoded);
        decoder.close();
        assertEquals(decoded.length, 960);
    }

    @Test
    @DisplayName("Invalid channel count")
    void invalidChannels() {
        assertThrows(IllegalArgumentException.class, () -> {
            OpusDecoder decoder = new OpusDecoder(48000, 3);
            decoder.close();
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OpusDecoder encoder = new OpusDecoder(48000, 0);
            encoder.close();
        });
    }

}
