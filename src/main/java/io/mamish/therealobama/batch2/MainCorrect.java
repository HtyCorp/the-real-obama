package io.mamish.therealobama.batch2;

import com.google.gson.Gson;
import io.mamish.therealobama.batch.VoskRecogniserJson;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;

public class MainCorrect {

    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {

        var wordListFile = args[0];
        var initialVoskFile = args[1];
        var matchOutputFile = args[2];

        var wordList = Files.readAllLines(Paths.get(wordListFile)).stream()
                .filter(not(String::isBlank))
                .collect(Collectors.toList());

        VoskRecogniserJson voskJson;
        try (Reader voskFile = new FileReader(initialVoskFile)) {
            voskJson = GSON.fromJson(voskFile, VoskRecogniserJson.class);
        }

        // Please don't judge me for what I'm about to write.

        try (
                PrintWriter out = new PrintWriter(Files.newOutputStream(Paths.get(matchOutputFile)), true);
                Scanner in = new Scanner(System.in)
        ) {
            int voskIndex = 0;
            for (String word : wordList) {
                var nextVoskMatchIndex = nextMatchingIndex(voskJson, word, voskIndex);

                if (nextVoskMatchIndex.isPresent() && nextVoskMatchIndex.get().equals(voskIndex)) {
                    log("Index match for \"%s\" at vosk index %d", word, voskIndex);
                } else {
                    log("\nMismatch at vosk/%d: expecting \"%s\"", voskIndex, word);
                    Integer rel = null;
                    if (nextVoskMatchIndex.isPresent()) {
                        var abs = nextVoskMatchIndex.get();
                        rel = abs - voskIndex;
                        log("Found potential match at relative index %d (absolute=%d)", rel, abs);
                    } else {
                        log("No skip matches found");
                    }
                    int finalVoskIndex = voskIndex;
                    var next10Words = IntStream.range(0, 10+1)
                            .mapToObj(ix -> ix + "(" + voskJson.getResult().get(finalVoskIndex+ix).getWord() + ")")
                            .collect(Collectors.joining(", "));
                    log("Next 10 words are: %s", next10Words);
                    System.out.format("Choose index%s: ", (rel == null) ? "" : " [suggest " + rel + "]"); // No newline
                    var userRel = in.nextInt();
                    voskIndex = voskIndex + userRel;
                    log("Skipped %d to absolute index %d", userRel, voskIndex);
                }

                // Now we have a selected word
                var item = voskJson.getResult().get(voskIndex);
                out.format("word \"%s\" %s %s %s%n",
                        word, voskIndex, item.getStart(), item.getEnd());

                voskIndex++;
            }
        }
    }

    private static Optional<Integer> nextMatchingIndex(VoskRecogniserJson vosk, String searchWord, int startIndex) {
        return IntStream.range(startIndex, vosk.getResult().size())
                .filter(ix -> vosk.getResult().get(ix).getWord().equals(searchWord))
                .boxed()
                .findFirst();
    }

    private static void log(String fmt, Object... args) {
        System.out.format(fmt + "%n", args);
    }
}
