package io.mamish.therealobama.codec;

import io.mamish.therealobama.audio.OpusFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static java.lang.Byte.toUnsignedInt;

public class OpusStreamDecoder {

    // References:
    // OGG for Opus: https://datatracker.ietf.org/doc/html/rfc7845
    // Opus: https://datatracker.ietf.org/doc/html/rfc6716

    private static final byte[] OPUS_ID_MAGIC_SIGNATURE = new byte[] { 'O', 'p', 'u', 's', 'H', 'e', 'a', 'd' };
    private static final int OPUS_PROTOCOL_VERSION = 1;
    private static final int SAMPLE_RATE_48KHZ = 48000;
    private static final int CHANNEL_MAPPING_FAMILY_MONO_STEREO = 0;

    private final List<OpusFrame> opusAudioFrames;

    public OpusStreamDecoder(ByteBuffer streamData) {
        OggStreamDecoder oggDecoder = new OggStreamDecoder(streamData);
        List<OpusFrame> allPackets = oggDecoder.getAllPackets();
        byte[] idPacketData = allPackets.get(0).getData();
        validateIdPacket(idPacketData);
        // TODO: Maybe we want to use the comment/metadata header to validate clip title or something? Skip it totally for now
        this.opusAudioFrames = allPackets.subList(2, allPackets.size());
    }

    public List<OpusFrame> getAudioFrames() {
        return opusAudioFrames;
    }

    private void validateIdPacket(byte[] idPacketData) {
        ByteBuffer data = ByteBuffer.wrap(idPacketData);
        data.order(ByteOrder.LITTLE_ENDIAN);

        byte[] magic = new byte[8];
        data.get(magic);
        if (!Arrays.equals(OPUS_ID_MAGIC_SIGNATURE, magic)) {
            throw new OpusDecodeException("Incorrect ID packet magic signature");
        }

        int version = toUnsignedInt(data.get());
        if (version != OPUS_PROTOCOL_VERSION) {
            throw new OpusDecodeException("Unexpected Opus protocol version field");
        }

        int channelCount = toUnsignedInt(data.get());
        if (channelCount != 2) {
            throw new OpusDecodeException("Expecting stereo frames, got a different channel count");
        }

        data.getShort(); // Pre-skip value, unused

        int sampleRate = data.getInt();
        if (sampleRate != SAMPLE_RATE_48KHZ) {
            throw new OpusDecodeException("Expecting 48KHz sample rate audio");
        }

        data.getShort(); // Output gain, unused

        byte channelMappingFamily = data.get();
        if (channelMappingFamily != CHANNEL_MAPPING_FAMILY_MONO_STEREO) {
            throw new OpusDecodeException("Expected standard mono/stereo channel mapping family");
        }

        if (data.hasRemaining()) {
            throw new OpusDecodeException("Unexpected extra data in ID packet");
        }
    }

}
