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
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 3, OpusEncoder.Application.VOIP);
            encoder.close();
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            OpusEncoder encoder = new OpusEncoder(48000, 0, OpusEncoder.Application.VOIP);
            encoder.close();
        });
    }

    @Test
    @DisplayName("Double close")
    void doubleClose() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        encoder.encode(new short[960]);
        encoder.close();
        encoder.close();
    }

    @Test
    @DisplayName("Encode after close")
    void encodeAfterClose() throws IOException, UnknownPlatformException {
        OpusEncoder encoder = new OpusEncoder(48000, 1, OpusEncoder.Application.VOIP);
        encoder.encode(new short[960]);
        encoder.close();
        RuntimeException e = assertThrowsExactly(RuntimeException.class, () -> {
            encoder.encode(new short[960]);
        });
        assertEquals("Failed to get encoder pointer", e.getMessage());
    }

}
