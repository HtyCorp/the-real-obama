package io.mamish.therealobama.batch;

import java.util.List;

public class VoskRecogniserJson {

    private List<Item> result;

    public List<Item> getResult() {
        return result;
    }

    public static class Item {

        private String word;
        private String conf;
        private String start;
        private String end;

        public String getWord() {
            return word;
        }

        public String getConf() {
            return conf;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }

}
