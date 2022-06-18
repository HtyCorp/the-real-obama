package io.mamish.therealobama.dao;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

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

    @DynamoDbSortKey
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

    @DynamoDbBean
    public static class BookLocation {

        private String book;
        private int chapterIndex;
        private long bookStartMs;
        private long chapterStartMs;
        private long lengthMs;

        public BookLocation() {
            // Default constructor for reflection
        }

        public BookLocation(String book, int chapterIndex, long bookStartMs, long chapterStartMs, long lengthMs) {
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

        public int getChapterIndex() {
            return chapterIndex;
        }

        public void setChapterIndex(int chapterIndex) {
            this.chapterIndex = chapterIndex;
        }

        public long getBookStartMs() {
            return bookStartMs;
        }

        public void setBookStartMs(long bookStartMs) {
            this.bookStartMs = bookStartMs;
        }

        public long getChapterStartMs() {
            return chapterStartMs;
        }

        public void setChapterStartMs(long chapterStartMs) {
            this.chapterStartMs = chapterStartMs;
        }

        public long getLengthMs() {
            return lengthMs;
        }

        public void setLengthMs(long lengthMs) {
            this.lengthMs = lengthMs;
        }
    }
}
