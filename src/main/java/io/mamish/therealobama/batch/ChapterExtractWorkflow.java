package io.mamish.therealobama.batch;

import com.google.gson.Gson;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import io.mamish.therealobama.dao.WordMetadataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChapterExtractWorkflow implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ChapterExtractWorkflow.class);
    private static final Gson gson = new Gson();

    private static final String WAV = ".wav";
    private static final String OPUS = ".opus";

    private final String wordPrefix;
    private final String sourceJob;
    private final String bookName;
    private final int chapterIndex;
    private final ChapterJson chapter;
    private final Path sourceAudioFile;
    private final WordMetadataDao metadataDao;
    private final WordAudioDao audioDao;
    private final Model voskModel;

    private final String runChapterTranscribeFile;
    private final String runS3Prefix;

    public ChapterExtractWorkflow(String wordPrefix, String sourceJob, String bookName, int chapterIndex, ChapterJson chapter, Path sourceAudioFile,
                                  WordMetadataDao metadataDao, WordAudioDao audioDao, Model voskModel) {
        this.wordPrefix = wordPrefix;
        this.sourceJob = sourceJob;
        this.bookName = bookName;
        this.chapterIndex = chapterIndex;
        this.chapter = chapter;
        this.sourceAudioFile = sourceAudioFile;
        this.metadataDao = metadataDao;
        this.audioDao = audioDao;
        this.voskModel = voskModel;

        String runName = String.format("%s.c%s", bookName, chapterIndex);
        runChapterTranscribeFile = runName + WAV;
        runS3Prefix = String.format("books/%s/chapter/%s/", bookName, chapterIndex);
    }

    @Override
    public void run() {
        writeTranscribeAudioFile();
        var transcribeResults = runLocalTranscription();
        transcribeResults.getResult().forEach(this::uploadWord);
    }

    private void writeTranscribeAudioFile() {
        runFfmpeg(
                "-ss", chapter.getStartSeconds(),
                "-t", chapter.getLengthSeconds(),
                "-i", sourceAudioFile.toString(),
                "-ac", "1",
                "-ar", "16k",
                "-map_metadata", "-1",
                "-y",
                runChapterTranscribeFile);
        validateFile(runChapterTranscribeFile);
    }

    private void runFfmpeg(String... args) {
        List<String> command = Stream.concat(
                Stream.of("ffmpeg"),
                Arrays.stream(args)
        ).collect(Collectors.toList());

        log.info("Running ffpmeg: " + command);

        int exitCode;
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .inheritIO()
                    .start();
            exitCode = process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Error calling command " + command, e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("Non-zero exit code from command: " + command);
        }
    }

    private void validateFile(String fileName) {
        if (!Files.isRegularFile(Paths.get(fileName))) {
            throw new RuntimeException("Not a regular file at " + fileName);
        }
    }

    private VoskRecogniserJson runLocalTranscription() {
        VoskRecogniserJson voskJson;

        try (
                AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(runChapterTranscribeFile)));
                Recognizer recognizer = new Recognizer(voskModel, 16000)
        ) {
            int nbytes;
            byte[] b = new byte[4096];
            recognizer.setWords(true);

            while ((nbytes = audio.read(b)) >= 0) {
                recognizer.acceptWaveForm(b, nbytes);
            }

            voskJson = gson.fromJson(recognizer.getFinalResult(), VoskRecogniserJson.class);

        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }

        return voskJson;
    }

    private void uploadWord(VoskRecogniserJson.Item item) {

        // Metadata

        var itemStartMsChapter = secondsToMillis(item.getStart());
        var itemStartMsBook = itemStartMsChapter + secondsToMillis(chapter.getStartSeconds());
        var itemEndMsChapter = secondsToMillis(item.getEnd());
        var itemLengthMs = itemEndMsChapter - itemStartMsChapter;

        var variantName = bookName + "_" + itemStartMsBook; // Timestamp is unique and easy to track to a book location
        var audioFileName = item.getWord() + "_" + itemStartMsBook + OPUS;
        var qualifiedAudioFileName = "words/" + audioFileName;
        var audioS3Key = runS3Prefix + audioFileName;

        var bookLocation = new WordMetadataItem.BookLocation(
                bookName, chapterIndex, itemStartMsBook, itemStartMsChapter, itemLengthMs
        );
        var wordWithPrefix = this.wordPrefix + item.getWord();
        var newWordMetadata = new WordMetadataItem(
                wordWithPrefix, variantName, audioS3Key, bookLocation, sourceJob
        );
        log.info("Uploading word metadata: " + newWordMetadata);
        metadataDao.putWordMetadata(newWordMetadata);

        // Audio

        runFfmpeg(
                "-ss", String.format("%.4f", itemStartMsBook / 1000f),
                "-t", String.format("%.4f", itemLengthMs / 1000f),
                "-i", sourceAudioFile.toString(),
                "-c:a", "libopus",
                "-b:a", "64k",
                "-ar", "48k",
                "-map_metadata", "-1",
                "-y",
                qualifiedAudioFileName
        );
        log.info("Uploading word file {} to S3 key {}", qualifiedAudioFileName, audioS3Key);
        audioDao.putAudioWordFile(audioS3Key, Paths.get(qualifiedAudioFileName));
    }

    private long secondsToMillis(String seconds) {
        return (long) (Float.parseFloat(seconds) * 1000);
    }
}
