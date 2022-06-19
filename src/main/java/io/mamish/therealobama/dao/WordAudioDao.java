package io.mamish.therealobama.dao;

import software.amazon.awssdk.services.s3.S3Client;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class WordAudioDao {

    private static final String AUDIO_WORD_FILE_BUCKET = "htycorp-therealobama-audio-words";

    private final S3Client s3Client = S3Client.create();

    public void putAudioWordFile(String key, Path path) {
        s3Client.putObject(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key), path);
    }

    public ByteBuffer getAudioWordFile(String key) {
        return s3Client.getObjectAsBytes(r -> r.bucket(AUDIO_WORD_FILE_BUCKET).key(key)).asByteBuffer();
    }

}
