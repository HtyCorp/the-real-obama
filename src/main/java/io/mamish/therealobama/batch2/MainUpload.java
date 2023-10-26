package io.mamish.therealobama.batch2;

import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import io.mamish.therealobama.dao.WordMetadataItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* Hacked together from existing io.mamish.therealobama.batch code as a rush job, pls don't judge. */
public class MainUpload {

    private static final WordMetadataDao metadataDao = new WordMetadataDao();
    private static final WordAudioDao audioDao = new WordAudioDao();

    public static void main(String[] args) throws IOException {
        var audioFilePath = Paths.get(args[0]);
        var sessionFilePath = Paths.get(args[1]);
        var persona = args[2];

        var sessionBaseName = sessionFilePath.toFile().getName().split("\\.")[0];

        log("Starting upload for audio file '%s' against session file '%s' (basename '%s'), for persona '%s'.",
                audioFilePath, sessionFilePath, sessionBaseName, persona);

        Files.readAllLines(sessionFilePath).forEach(line -> {
            var tokens = line.split("\\s+");
            if (tokens.length > 0 && tokens[0].equals("word")) {
                var rawWord = tokens[1];
                var start = Double.parseDouble(tokens[3]);
                var length = Double.parseDouble(tokens[4]) - start;

                if (!rawWord.matches("\"[^\"]+\"")) {
                    throw new RuntimeException("Unexpected unquoted word: " + rawWord);
                }
                var word = rawWord.substring(1, rawWord.length()-1);
                log("Got word '%s' at time %s with length %s", word, start, length);

                uploadWord(word, persona, sessionBaseName, audioFilePath, start, length);
            }
            else {
                log("Ignoring line: '%s'", line);
            }
        });
    }

    private static void uploadWord(String word, String persona, String sessionName,
                                   Path sourceAudioFilePath, double startTimeSeconds, double lengthSeconds) {

        // Metadata

        var startTimeMillis = (long)(startTimeSeconds*1000);

        var variant = sessionName + "_" + startTimeMillis; // Timestamp is unique and easy to track to a session
        var audioFileName = String.format("%s_%d.opus", word, startTimeMillis);
        var tempOutputFileName = String.format("words/%s", audioFileName);
        var audioS3Key = String.format("persona/%s/session/%s/%s", persona, sessionName, audioFileName);

        var bookLocation = new WordMetadataItem.BookLocation(
                sessionName, 0,
                startTimeMillis, startTimeMillis,
                (long)(lengthSeconds * 1000f)
        );
        var wordWithPersona = persona + ":" + word;
        var newWordMetadata = new WordMetadataItem(
                // sessionName is kind of a duplicate; no difference between session and 'book' in self-recorded sessions
                wordWithPersona, variant, audioS3Key, bookLocation, sessionName
        );
        log("Uploading word metadata: %s", newWordMetadata);
        metadataDao.putWordMetadata(newWordMetadata);

        // Audio

        runFfmpeg(
                "-ss", String.format("%.4f", startTimeSeconds),
                "-t", String.format("%.4f", lengthSeconds),
                "-i", sourceAudioFilePath.toString(),
                "-c:a", "libopus",
                "-b:a", "64k",
                "-ar", "48k",
                "-map_metadata", "-1",
                "-y",
                tempOutputFileName
        );
        log("Uploading word file %s to S3 key %s", tempOutputFileName, audioS3Key);
        audioDao.putAudioWordFile(audioS3Key, Paths.get(tempOutputFileName));
    }

    private static void runFfmpeg(String... args) {
        List<String> command = Stream.concat(
                Stream.of("ffmpeg"),
                Arrays.stream(args)
        ).collect(Collectors.toList());

        log("Running ffpmeg: %s", command);

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

    private static void log(String fmt, Object... args) {
        System.out.format(fmt + "%n", args);
    }
}
