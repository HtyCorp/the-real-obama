package io.mamish.therealobama.batch;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import io.mamish.therealobama.dao.WordMetadataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.LanguageCode;
import software.amazon.awssdk.services.transcribe.model.TranscriptionJob;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.awssdk.services.transcribe.model.TranscriptionJobStatus.*;

public class ChapterExtractWorkflow implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ChapterExtractWorkflow.class);
    private static final Gson gson = new Gson();

    private static final String OPUS = ".opus";
    private static final String JSON = ".json";

    private final String bookName;
    private final int chapterIndex;
    private final ChapterJson chapter;
    private final Path sourceAudioFile;
    private final WordMetadataDao metadataDao;
    private final WordAudioDao audioDao;

    private final String runName;
    private final String runChapterAudioFileName;
    private final String runS3Prefix;

    public ChapterExtractWorkflow(String bookName, int chapterIndex, ChapterJson chapter, Path sourceAudioFile,
                                  WordMetadataDao metadataDao, WordAudioDao audioDao) {
        this.bookName = bookName;
        this.chapterIndex = chapterIndex;
        this.chapter = chapter;
        this.sourceAudioFile = sourceAudioFile;
        this.metadataDao = metadataDao;
        this.audioDao = audioDao;

        String runUuid = UUID.randomUUID().toString();
        runName = String.format("%s.c%s.%s", bookName, chapterIndex, UUID.randomUUID());
        runChapterAudioFileName = runName + OPUS;
        runS3Prefix = String.format("books/%s/chapter/%s/%s/", bookName, chapterIndex, runUuid);
    }

    @Override
    public void run() {
        convertSourceToOpusFile();

        audioDao.putTranscribeInputFile(runS3Prefix + runChapterAudioFileName, Paths.get(runChapterAudioFileName));

        var transcribeResults = runTranscriptionJob();

        transcribeResults.getItems().forEach(item -> {
            if (item.isPronunciationType()) {
                var alternativeChoice = selectItemAlternative(item);
                uploadWord(item, alternativeChoice);
            }
        });
    }

    private void convertSourceToOpusFile() {
        runFfmpeg(
                "-ss", chapter.getStartSeconds(),
                "-t", chapter.getLengthSeconds(),
                "-i", sourceAudioFile.toString(),
                "-c:a", "libopus",
                "-b:a", "64k",
                runChapterAudioFileName);
        validateFile(runChapterAudioFileName);
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

    private TranscriptionOutput runTranscriptionJob() {
        var inputKey = runS3Prefix + runName + OPUS;
        var outputKey = runS3Prefix + runName + JSON;

        try (var transcribeClient = TranscribeClient.create()) {
            transcribeClient.startTranscriptionJob(r -> r
                    .languageCode(LanguageCode.EN_US)
                    .media(m -> m.mediaFileUri(WordAudioDao.getTranscribeInputFileUri(inputKey)))
                    .outputBucketName(WordAudioDao.TRANSCRIBE_OUTPUT_BUCKET)
                    .outputKey(outputKey)
                    //.settings(s -> s.showAlternatives(true).maxAlternatives(5))
                    .transcriptionJobName(runName));

            TranscriptionJob transcriptionJob;
            do {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Unexpected interrupt while polling Transcribe");
                }
                transcriptionJob = transcribeClient.getTranscriptionJob(r -> r.transcriptionJobName(runName)).transcriptionJob();
                log.info("Transcribe job {} has status {}", transcriptionJob.transcriptionJobName(), transcriptionJob.transcriptionJobStatus());
            } while (Set.of(QUEUED, IN_PROGRESS).contains(transcriptionJob.transcriptionJobStatus()));

            if (transcriptionJob.transcriptionJobStatus() == FAILED) {
                throw new RuntimeException("Transcription job failed: " + transcriptionJob.failureReason());
            }
        }

        var rawJson = audioDao.getTranscribeOutputJson(outputKey);
        var resultsObj = JsonParser.parseString(rawJson).getAsJsonObject()
                .get("results").getAsJsonObject();
        return gson.fromJson(resultsObj, TranscriptionOutput.class);
    }

    private TranscriptionOutput.Item.Alternative selectItemAlternative(TranscriptionOutput.Item item) {
       return item.getAlternatives().stream()
                .max(Comparator.comparing(TranscriptionOutput.Item.Alternative::getConfidence))
                .orElseThrow();
    }

    private void uploadWord(TranscriptionOutput.Item item, TranscriptionOutput.Item.Alternative choice) {

        // TODO: Need a new approach. Transcribe word boundaries aren't precise enough :(

        // Metadata

        var itemStartMsChapter = secondsToMillis(item.getStartTimeSeconds());
        var itemStartMsBook = itemStartMsChapter + secondsToMillis(chapter.getStartSeconds());
        var itemEndMsChapter = secondsToMillis(item.getEndTimeSeconds());
        var itemLengthMs = itemEndMsChapter - itemStartMsChapter;

        var variantName = bookName + "_" + itemStartMsBook; // Timestamp is unique and easy to track to a book location
        var audioFileName = choice.getContent() + "_" + itemStartMsBook + OPUS;
        var audioS3Key = runS3Prefix + audioFileName;

        var bookLocation = new WordMetadataItem.BookLocation(
                bookName, chapterIndex, itemStartMsBook, itemStartMsChapter, itemLengthMs
        );
        var newWordMetadata = new WordMetadataItem(
                choice.getContent(), variantName, audioS3Key, bookLocation
        );
        log.info("Uploading word metadata: " + newWordMetadata);
        metadataDao.putWordMetadata(newWordMetadata);

        // Audio

        runFfmpeg(
                "-ss", item.getStartTimeSeconds(),
                "-to", item.getEndTimeSeconds(),
                "-i", runChapterAudioFileName,
                "-c", "copy",
                audioFileName
        );
        log.info("Uploading word file {} to S3 key {}", audioFileName, audioS3Key);
        audioDao.putAudioWordFile(audioS3Key, Paths.get(audioFileName));
    }

    private long secondsToMillis(String seconds) {
        return (long) (Float.parseFloat(seconds) * 1000);
    }
}
