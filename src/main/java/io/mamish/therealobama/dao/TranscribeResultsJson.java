package io.mamish.therealobama.dao;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranscribeResultsJson {

    private List<Transcript> transcripts;
    private List<Item> items;

    public List<Transcript> getTranscripts() {
        return transcripts;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Transcript {
        private String transcript;

        public String getTranscript() {
            return transcript;
        }
    }

    public static class Item {

        @SerializedName("start_time")
        private String startTimeSeconds;
        @SerializedName("end_time")
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

        public boolean isPronunciationType() {
            return type.equals("pronunciation");
        }
    }
}
