package io.mamish.therealobama.ogg;

import java.nio.ByteBuffer;
import java.util.Queue;

import static java.lang.Byte.toUnsignedInt;

public class OpusStreamDecoder {

    // References:
    // OGG for Opus: https://datatracker.ietf.org/doc/html/rfc7845
    // Opus: https://datatracker.ietf.org/doc/html/rfc6716

    private static final long OPUS_ID_MAGIC_SIGNATURE_LE_LONG = 'O' | 'p' << 1 | 'u' << 2 | 's' << 3 | 'H' << 4 | 'e' << 5 | 'a' << 6 | 'd' << 7;
    private static final int OPUS_PROTOCOL_VERSION = 1;
    private static final int SAMPLE_RATE_48KHZ = 48000;
    private static final int CHANNEL_MAPPING_FAMILY_MONO_STEREO = 0;

    private final Queue<byte[]> packetQueue;

    public OpusStreamDecoder(ByteBuffer streamData) {
        OggStreamDecoder oggDecoder = new OggStreamDecoder(streamData);
        this.packetQueue = oggDecoder.getAllPackets();
        byte[] idPacket = packetQueue.remove();
        validateIdPacket(idPacket);
        packetQueue.remove(); // Metadata packet, unused
    }

    public byte[] getNextPacket() {
        return packetQueue.poll();
    }

    private void validateIdPacket(byte[] idPacketData) {
        ByteBuffer buf = ByteBuffer.wrap(idPacketData);

        long magic = buf.getLong();
        if (magic != OPUS_ID_MAGIC_SIGNATURE_LE_LONG) {
            throw new OpusDecodeException("Incorrect ID packet magic signature");
        }

        int version = toUnsignedInt(buf.get());
        if (version != OPUS_PROTOCOL_VERSION) {
            throw new OpusDecodeException("Unexpected Opus protocol version field");
        }

        int channelCount = toUnsignedInt(buf.get());
        if (channelCount != 2) {
            throw new OpusDecodeException("Expecting stereo frames, got a different channel count");
        }

        buf.getShort(); // Pre-skip value, unused

        int sampleRate = buf.getInt();
        if (sampleRate != SAMPLE_RATE_48KHZ) {
            throw new OpusDecodeException("Expecting 48KHz sample rate audio");
        }

        buf.getShort(); // Output gain, unused

        byte channelMappingFamily = buf.get();
        if (channelMappingFamily != 0) {
            throw new OpusDecodeException("Expected standard mono/stereo channel mapping family");
        }

        if (buf.hasRemaining()) {
            throw new OpusDecodeException("Unexpected extra data in ID packet");
        }
    }

}
