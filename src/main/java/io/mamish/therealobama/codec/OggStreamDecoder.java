package io.mamish.therealobama.codec;

import io.mamish.therealobama.audio.OpusFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Queue;

import static java.lang.Byte.toUnsignedInt;

public class OggStreamDecoder {

    // References:
    // OGG encapsulation: https://datatracker.ietf.org/doc/html/rfc3533
    // OGG for Opus: https://datatracker.ietf.org/doc/html/rfc7845
    // Opus: https://datatracker.ietf.org/doc/html/rfc6716

    private static final int PAGE_HEADER_AS_LE_INT = 'O' | ('g' << 8) | ('g' << 16) | ('S' << 24);
    private static final byte OGG_PROTOCOL_VERSION = 0;

    private static final byte HEADER_FLAG_CONTINUATION = 1;
    private static final byte HEADER_FLAG_BOS = (1 << 1);
    private static final byte HEADER_FLAG_EOS = (1 << 2);

    private static final int PACKET_BUFFER_CAPACITY = 65536; // This is a bit bigger than even the silly max case noted in RFC7845 (~61k)
    private static final int MAX_SEGMENT_SIZE = 255;

    private final Queue<OpusFrame> allPackets = new ArrayDeque<>();

    public OggStreamDecoder(ByteBuffer streamData) {
        readAllPackets(streamData);
    }

    public Queue<OpusFrame> getAllPackets() {
        return allPackets;
    }

    private void readAllPackets(ByteBuffer sourceData) {
        sourceData.order(ByteOrder.LITTLE_ENDIAN);

        int nextSequenceNumber = 0; // This might start at 1 instead of 0, confirm later
        boolean isFirstPage = true;
        ByteBuffer currentPacketBuffer = ByteBuffer.allocateDirect(PACKET_BUFFER_CAPACITY);
        boolean wasLastPageEosFlagSet = false;

        while (sourceData.hasRemaining()) {

            int capture = sourceData.getInt();
            if (capture != PAGE_HEADER_AS_LE_INT) {
                throw new OggDecodeException("OGG capture pattern missing at page boundary");
            }

            byte version = sourceData.get();
            if (version != OGG_PROTOCOL_VERSION) {
                throw new OggDecodeException("Bad OGG version field");
            }

            HeaderType headerType = new HeaderType(sourceData.get());
            boolean isExpectingContinuation = currentPacketBuffer.position() > 0;
            if (isExpectingContinuation ^ headerType.isContinuation) {
                throw new OggDecodeException(String.format("Continuation mismatch: expecting=%b, actual=%b",
                        isExpectingContinuation, headerType.isContinuation));
            }
            if (isFirstPage && !headerType.isBeginningOfStream) {
                throw new OggDecodeException("First packet in stream is missing BOS flag");
            }

            sourceData.getLong(); // Granule position, unused
            sourceData.getInt(); // Bitstream serial number, unused

            int pageSequenceNumber = sourceData.getInt(); // Page sequence number, unused
            if (pageSequenceNumber != nextSequenceNumber) {
                throw new OggDecodeException("Sequence number mismatch");
            }

            sourceData.getInt(); // Checksum, unused

            int numSegments = toUnsignedInt(sourceData.get());
            byte[] segmentTable = new byte[numSegments];
            sourceData.get(segmentTable);

            for (byte nextSegmentSizeByte: segmentTable) {
                int nextSegmentSize = toUnsignedInt(nextSegmentSizeByte);

                // Copy nextSegmentSize bytes into the current packet buffer
                sourceData.limit(sourceData.position() + nextSegmentSize);
                currentPacketBuffer.put(sourceData);
                sourceData.limit(sourceData.capacity()); // Change the source limit to its original value (== capacity, since it's a wrap buffer)

                // If we got a size < 255, we now have all the data for this packet and can add it to the list
                if (nextSegmentSize < MAX_SEGMENT_SIZE) {
                    currentPacketBuffer.flip();
                    byte[] fullPacketData = new byte[currentPacketBuffer.limit()];
                    currentPacketBuffer.get(fullPacketData);
                    allPackets.add(new OpusFrame(fullPacketData));
                    currentPacketBuffer.clear();
                }
            }

            wasLastPageEosFlagSet = headerType.isEndOfStream;
            isFirstPage = false;
            nextSequenceNumber++;
        }

        if (!wasLastPageEosFlagSet) {
            throw new OggDecodeException("Last packet in stream is missing EOS flag");
        }
        if (currentPacketBuffer.position() > 0) {
            throw new OggDecodeException("Incomplete data in the current packet buffer");
        }
    }

    private static class HeaderType {
        final boolean isContinuation;
        final boolean isBeginningOfStream;
        final boolean isEndOfStream;

        HeaderType(byte headerTypefieldvalue) {
            isContinuation = (headerTypefieldvalue & HEADER_FLAG_CONTINUATION) != 0;
            isBeginningOfStream = (headerTypefieldvalue & HEADER_FLAG_BOS) != 0;
            isEndOfStream = (headerTypefieldvalue & HEADER_FLAG_EOS) != 0;
        }
    }

}
