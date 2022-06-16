package io.mamish.therealobama;

import io.mamish.therealobama.audio.OpusFrame;
import io.mamish.therealobama.audio.Word;
import io.mamish.therealobama.codec.OpusStreamDecoder;
import software.amazon.awssdk.utils.Either;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class WordLoader {

    private static final String TEST_WORD = "speakownself";

    public Either<String,Word> loadWord(String wordText) {
        if (!wordText.equals(TEST_WORD)) {
            // Only supports test audio word for now
            return Either.left(wordText);
        }

        byte[] opusFileRaw;
        try (var testFile = Main.class.getClassLoader().getResourceAsStream("testaudio/speakforyourownself.opus")) {
            opusFileRaw = testFile.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Test audio file load error", e);
        }

        OpusStreamDecoder opusStreamDecoder = new OpusStreamDecoder(ByteBuffer.wrap(opusFileRaw));

        Queue<OpusFrame> audioFrameQueue = new LinkedList<>(opusStreamDecoder.getAudioFrames());
        return Either.right(new Word(audioFrameQueue));
    }

}
