package io.mamish.therealobama.audio;

public class OpusFrame {

    private final byte[] data;

    public OpusFrame(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
