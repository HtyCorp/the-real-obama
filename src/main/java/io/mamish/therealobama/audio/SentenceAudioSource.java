package io.mamish.therealobama.audio;

import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SentenceAudioSource extends AudioSourceBase {

    private static final int NUM_PAUSE_FRAMES_BETWEEN_WORDS = 4; // x20ms = 80ms

    private final Sentence sentence;
    private final CompletableFuture<Void> onLastFrameFuture;

    private Word currentWord;
    private OpusFrame nextFrame;
    private int numPauseFramesRemaining = 0;

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
            numPauseFramesRemaining = NUM_PAUSE_FRAMES_BETWEEN_WORDS;
        }

        // Get next frame in the current word; this can be null if we're at the least word and have already polled all its frames
        nextFrame = currentWord.getOpusFrames().poll();
    }

    @Override
    public byte[] getNextFrame() {
        byte[] frameData = Objects.requireNonNull(nextFrame, "Cannot get next frame when at end of stream").getData();
        advance();
        if (nextFrame == null && !onLastFrameFuture.isDone()) {
            onLastFrameFuture.complete(null);
        }
        return frameData;
    }

    @Override
    public boolean hasNextFrame() {
        if (numPauseFramesRemaining > 0) {
            numPauseFramesRemaining--;
            return false;
        }
        return nextFrame != null;
    }

    @Override
    public boolean hasFinished() {
        // This method has to be overridden since we want to inject pauses using hasNextFrame without marking the source as finished (default impl)
        return nextFrame == null;
    }

    @Override
    public AudioSource copy() {
        throw new RuntimeException("Copy not implemented");
    }
}
