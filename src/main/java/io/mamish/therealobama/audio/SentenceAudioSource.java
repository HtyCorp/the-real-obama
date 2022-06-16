package io.mamish.therealobama.audio;

import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SentenceAudioSource extends AudioSourceBase {

    private final Sentence sentence;
    private final CompletableFuture<Void> onLastFrameFuture;

    private Word currentWord;
    private OpusFrame nextFrame;

    public SentenceAudioSource(DiscordApi api, Sentence sentence, CompletableFuture<Void> onLastFrameFuture) {
        super(api);
        this.sentence = sentence;
        this.onLastFrameFuture = onLastFrameFuture;
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
        if (!hasNextFrame() && !onLastFrameFuture.isDone()) {
            onLastFrameFuture.complete(null);
        }
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
