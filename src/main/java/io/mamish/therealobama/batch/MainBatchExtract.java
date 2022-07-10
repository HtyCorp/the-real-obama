package io.mamish.therealobama.batch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import org.vosk.Model;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MainBatchExtract {

    public static void main(String[] args) throws IOException {

        WordMetadataDao wordMetadataDao = new WordMetadataDao();
        WordAudioDao wordAudioDao = new WordAudioDao();

        JsonObject configJson = JsonParser.parseReader(new FileReader("config.json")).getAsJsonObject();
        String wordPrefix = configJson.get("wordPrefix").getAsString();
        String bookName = configJson.get("bookName").getAsString();
        Path audioFilePath = Paths.get(configJson.get("audioFile").getAsString());
        if (!Files.isRegularFile(audioFilePath)) {
            throw new IllegalArgumentException("Audio file is a regular file");
        }
        Path chapterFilePath = Paths.get(configJson.get("chaptersFile").getAsString());
        if (!Files.isRegularFile(chapterFilePath)) {
            throw new IllegalArgumentException("Chapters file is not a regular file");
        }

        Model voskModel = new Model(configJson.get("modelDir").getAsString());

        int chapterStart = configJson.get("chapterStart").getAsInt();
        int chapterLimit = configJson.get("maxChapters").getAsInt();

        Type chapterJsonListType = new TypeToken<List<ChapterJson>>(){}.getType();
        List<ChapterJson> chapters = new Gson().fromJson(new FileReader(chapterFilePath.toFile()), chapterJsonListType);

        String jobName = String.format("Vosk_V1_%s_%s", LocalDateTime.now(), UUID.randomUUID());

        for (int i = chapterStart; i < chapters.size() && i < (chapterStart + chapterLimit); i++) {
            new ChapterExtractWorkflow(
                    wordPrefix, jobName, bookName, i, chapters.get(i), audioFilePath, wordMetadataDao, wordAudioDao, voskModel
            ).run();
        }
    }
}
