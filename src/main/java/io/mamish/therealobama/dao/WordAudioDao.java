package io.mamish.therealobama.dao;

import software.amazon.awssdk.services.s3.S3Client;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class WordAudioDao {

    public static final String TRANSCRIBE_INPUT_BUCKET = "htycorp-therealobama-audio-transcribe-input";
    public static final String TRANSCRIBE_OUTPUT_BUCKET = "htycorp-therealobama-audio-transcribe-output";
    private static final String AUDIO_WORD_FILE_BUCKET = "htycorp-therealobama-audio-words";

    private final S3Client s3Client = S3Client.create();

    public static String getTranscribeInputFileUri(String key) {
        return String.format("s3://%s/%s", TRANSCRIBE_INPUT_BUCKET, key);
    }

    public void putTranscribeInputFile(String key, Path path) {
        s3Client.putObject(r -> r.bucket(TRANSCRIBE_INPUT_BUCKET).key(key), path);
    }

    // TODO: Putting this JSON getter in an 'audio DAO' feels a bit silly, maybe move it
    public String getTranscribeOutputJson(String key) {
        return s3Client.getObjectAsBytes(r -> r.bucket(TRANSCRIBE_OUTPUT_BUCKET).key(key)).asUtf8String();
    }

    public void putAudioWordFile(String key, Path path) {
        s3Client.putObject(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key), path);
    }

    public ByteBuffer getAudioWordFile(String key) {
        return s3Client.getObjectAsBytes(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key)).asByteBuffer();
    }

}
