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
                String overrideWord = null;
                boolean abandonWord = false;

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
                    var numPrevWords = Math.min(5, voskIndex);
                    var previousWords = IntStream.range(-numPrevWords, 0)
                            .mapToObj(offs -> offs + "(" + voskJson.getResult().get(finalVoskIndex+offs).getWord() + ")")
                            .collect(Collectors.joining(", "));
                    log("Last %d words: %s", numPrevWords, previousWords);
                    var numNextWords = Math.min(10, voskJson.getResult().size() - voskIndex - 1);
                    var nextWords = IntStream.range(0, numNextWords)
                            .mapToObj(offs -> offs + "(" + voskJson.getResult().get(finalVoskIndex+offs).getWord() + ")")
                            .collect(Collectors.joining(", "));
                    log("Next %d words: %s", numNextWords, nextWords);
                    var nextTimestamp = (long) Double.parseDouble(voskJson.getResult().get(voskIndex).getStart());
                    log("Next timestamp: %s", formatTimestamp(nextTimestamp));
                    log("|");
                    log("Expecting/lead: %s/%s", word, voskJson.getResult().get(voskIndex).getWord());

                    // TODO: Add daiquiri detection
                    // If you're reading this note 5 years from now and don't know what this means, sorry ;)

                    while (true) {
                        System.out.format("Index or override%s: ", (rel == null) ? "" : " [suggest " + rel + "]"); // No newline
                        var userReq = in.nextLine();
                        if (userReq.equals("_")) {
                            abandonWord = true;
                            log("Abandoned word");
                            out.format("abandon word \"%s\"%n", word);
                            break;
                        }
                        if (userReq.matches("-?\\d+")) {
                            var userOffset = Integer.parseInt(userReq);
                            voskIndex = voskIndex + userOffset;
                            log("Skipped %d to absolute index %d", userOffset, voskIndex);
                            out.format("offset %d to absolute voskIndex %d%n", userOffset, voskIndex);
                            break;
                        }
                        if (userReq.matches("[a-z'./-]+")) {
                            // Write to file as a debug marker later on if we need
                            overrideWord = userReq;
                            log("Overwrote word \"%s\" with replacement \"%s\"", word, overrideWord);
                            out.format("override word \"%s\" with \"%s\"%n", word, overrideWord);
                            break;
                        }
                        log("Invalid input \"%s\"", userReq);
                    }
                }

                if (!abandonWord) {
                    // Now we have a selected word
                    var item = voskJson.getResult().get(voskIndex);
                    out.format("word \"%s\" %s %s %s%n",
                            overrideWord != null ? overrideWord : word, voskIndex, item.getStart(), item.getEnd());

                    voskIndex++;
                }
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

    private static String formatTimestamp(long duration) {
        // This is a weird-looking way to lay it out, but I just like the pattern it reveals.
        var hours = duration/(60L*60L); // this would be modulo-24 if we included days
        var minutes =   (duration/60L) % 60L;
        var seconds =         duration % 60L;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
