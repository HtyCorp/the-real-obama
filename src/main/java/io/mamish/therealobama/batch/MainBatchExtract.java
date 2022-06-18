package io.mamish.therealobama.batch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class MainBatchExtract {

    public static void main(String[] args) throws IOException {

        WordMetadataDao wordMetadataDao = new WordMetadataDao();
        WordAudioDao wordAudioDao = new WordAudioDao();

        // Env vars are not the best solution here, but getting parameters in from a Gradle task isn't that easy
        String bookName = envNonNull("EXTRACT_BOOK_NAME");
        Path audioFilePath = Paths.get(envNonNull("EXTRACT_AUDIO_FILE"));
        if (!Files.isRegularFile(audioFilePath)) {
            throw new IllegalArgumentException("Audio file is a regular file");
        }
        Path chapterFilePath = Paths.get(envNonNull("EXTRACT_CHAPTERS_FILE"));
        if (!Files.isRegularFile(chapterFilePath)) {
            throw new IllegalArgumentException("Chapters file is not a regular file");
        }

        Type chapterJsonListType = new TypeToken<List<ChapterJson>>(){}.getType();
        List<ChapterJson> chapters = new Gson().fromJson(new FileReader(chapterFilePath.toFile()), chapterJsonListType);

        for (int i = 0; i < chapters.size(); i++) {
            new ChapterExtractWorkflow(
                    bookName, i, chapters.get(i), audioFilePath, wordMetadataDao, wordAudioDao
            ).run();
        }
    }

    private static String envNonNull(String key) {
        return Optional.ofNullable(System.getenv(key)).orElseThrow();
    }
}
