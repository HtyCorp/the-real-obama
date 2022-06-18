package io.mamish.therealobama.dao;

import com.google.gson.Gson;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class WordAudioDao {

    public static final String TRANSCRIBE_INPUT_BUCKET = "htycorp-therealobama-audio-transcribe-input";
    public static final String TRANSCRIBE_OUTPUT_BUCKET = "htycorp-therealobama-audio-transcribe-output";
    private static final String AUDIO_WORD_FILE_BUCKET = "htycorp-therealobama-audio-words";

    private static final Gson GSON = new Gson();

    private final S3Client s3Client = S3Client.create();

    public void putTranscribeInputFile(String key, Path path) {
        s3Client.putObject(r -> r.bucket(TRANSCRIBE_INPUT_BUCKET).key(key), path);
    }

    public TranscribeResultsJson getTranscribeOutputResults(String key) {
        String rawJson = s3Client.getObjectAsBytes(r -> r.bucket(TRANSCRIBE_OUTPUT_BUCKET).key(key)).asUtf8String();
        return GSON.fromJson(rawJson, TranscribeResultsJson.class);
    }

    public void putAudioWordFile(String key, Path path) {
        s3Client.putObject(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key), path);
    }

    public ByteBuffer getAudioWordFile(String key) {
        return s3Client.getObjectAsBytes(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key)).asByteBuffer();
    }

}
