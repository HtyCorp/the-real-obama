package io.mamish.therealobama.audio;

import java.util.Queue;

public class Word {

    private final Queue<OpusFrame> opusFrames;

    public Word(Queue<OpusFrame> opusFrames) {
        this.opusFrames = opusFrames;
    }

    public Queue<OpusFrame> getOpusFrames() {
        return opusFrames;
    }
}
