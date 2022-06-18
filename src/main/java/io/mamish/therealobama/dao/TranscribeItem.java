package io.mamish.therealobama.dao;

public class TranscribeItem {

    private String startTimeSeconds;
    private String endTimeSeconds;
    private String alternatives;
    private String type;

    public String getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public String getEndTimeSeconds() {
        return endTimeSeconds;
    }

    public String getAlternatives() {
        return alternatives;
    }

    public String getType() {
        return type;
    }
}
