package io.mamish.therealobama.dao;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

// TODO: This should use the DDB enhanced client immutable pattern

@DynamoDbBean
public class WordMetadataItem {

    private String word;
    private String variant;
    private String audioFileS3Key;
    private BookLocation bookLocation;

    public WordMetadataItem() {
        // Default constructor for reflection
    }

    public WordMetadataItem(String word, String variant, String audioFileS3Key, BookLocation bookLocation) {
        this.word = word;
        this.variant = variant;
        this.audioFileS3Key = audioFileS3Key;
        this.bookLocation = bookLocation;
    }

    @DynamoDbPartitionKey
    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    @DynamoDbPartitionKey
    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getAudioFileS3Key() {
        return audioFileS3Key;
    }

    public void setAudioFileS3Key(String audioFileS3Key) {
        this.audioFileS3Key = audioFileS3Key;
    }

    public BookLocation getBookLocation() {
        return bookLocation;
    }

    public void setBookLocation(BookLocation bookLocation) {
        this.bookLocation = bookLocation;
    }

    public static class BookLocation {
        private String book;
        private String chapterIndex;
        private String bookStartMs;
        private String chapterStartMs;
        private String lengthMs;

        public BookLocation() {
            // Default constructor for reflection
        }

        public BookLocation(String book, String chapterIndex, String bookStartMs, String chapterStartMs, String lengthMs) {
            this.book = book;
            this.chapterIndex = chapterIndex;
            this.bookStartMs = bookStartMs;
            this.chapterStartMs = chapterStartMs;
            this.lengthMs = lengthMs;
        }

        public String getBook() {
            return book;
        }

        public void setBook(String book) {
            this.book = book;
        }

        public String getChapterIndex() {
            return chapterIndex;
        }

        public void setChapterIndex(String chapterIndex) {
            this.chapterIndex = chapterIndex;
        }

        public String getBookStartMs() {
            return bookStartMs;
        }

        public void setBookStartMs(String bookStartMs) {
            this.bookStartMs = bookStartMs;
        }

        public String getChapterStartMs() {
            return chapterStartMs;
        }

        public void setChapterStartMs(String chapterStartMs) {
            this.chapterStartMs = chapterStartMs;
        }

        public String getLengthMs() {
            return lengthMs;
        }

        public void setLengthMs(String lengthMs) {
            this.lengthMs = lengthMs;
        }
    }

}
