package io.mamish.therealobama.audio;

import java.util.Queue;

public class Sentence {

    private final Queue<Word> words;

    public Sentence(Queue<Word> words) {
        this.words = words;
    }

    public Queue<Word> getWords() {
        return words;
    }
}
