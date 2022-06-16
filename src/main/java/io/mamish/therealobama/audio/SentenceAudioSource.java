package io.mamish.therealobama.audio;

import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

import java.util.Objects;

public class SentenceAudioSource extends AudioSourceBase {

    private final Sentence sentence;

    private Word currentWord;
    private OpusFrame nextFrame;

    public SentenceAudioSource(DiscordApi api, Sentence sentence) {
        super(api);
        this.sentence = sentence;
        currentWord = Objects.requireNonNull(sentence.getWords().poll(), "Provided sentence has no words");
        advance();
    }

    private void advance() {
        // As long as the current word is empty and there are more words to poll, advance through them
        while (currentWord.getOpusFrames().isEmpty() && !sentence.getWords().isEmpty()) {
            currentWord = sentence.getWords().remove();
        }

        // Get next frame in the current word; this can be null if we're at the least word and have already polled all its frames
        nextFrame = currentWord.getOpusFrames().poll();
    }

    @Override
    public byte[] getNextFrame() {
        byte[] frameData = Objects.requireNonNull(nextFrame, "Cannot get next frame when at end of stream").getData();
        advance();
        return frameData;
    }

    @Override
    public boolean hasNextFrame() {
        return nextFrame != null;
    }

    @Override
    public AudioSource copy() {
        throw new RuntimeException("Copy not implemented");
    }
}
