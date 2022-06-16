package io.mamish.therealobama;

import io.mamish.therealobama.audio.Word;
import io.mamish.therealobama.codec.OpusStreamDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

public class WordLoader {

    private static final String TEST_WORD = "speakownself";

    public Word loadWord(String wordText) {
        if (!wordText.equals(TEST_WORD)) {
            throw new IllegalArgumentException("Only supports test audio word");
        }

        byte[] opusFileRaw;
        try (var testFile = Main.class.getClassLoader().getResourceAsStream("testaudio/speakforyourownself.opus")) {
            opusFileRaw = testFile.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Test audio file load error", e);
        }

        OpusStreamDecoder opusStreamDecoder = new OpusStreamDecoder(ByteBuffer.wrap(opusFileRaw));
        return new Word(new LinkedList<>(opusStreamDecoder.getAudioFrames()));
    }

}
