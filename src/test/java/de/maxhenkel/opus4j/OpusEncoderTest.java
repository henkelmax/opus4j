package de.maxhenkel.opus4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class OpusEncoderTest {

    @Test
    @DisplayName("Encode")
    void encode() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        byte[] encoded = encoder.encode(new short[960]);
        encoder.close();
        assertTrue(encoded.length > 0);
    }

    @Test
    @DisplayName("Invalid channel count")
    void invalidChannels() {
        assertThrows(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 3, OpusEncoder.Application.VOIP);
            encoder.close();
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 0, OpusEncoder.Application.VOIP);
            encoder.close();
        });
    }

}
