package io.mamish.therealobama;

import io.mamish.therealobama.audio.OpusFrame;
import io.mamish.therealobama.audio.Word;
import io.mamish.therealobama.codec.OpusStreamDecoder;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import io.mamish.therealobama.dao.WordMetadataItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.Either;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.util.function.Predicate.not;

public class WordLoader {

    private static final Logger log = LoggerFactory.getLogger(WordLoader.class);

    private final WordMetadataDao wordMetadataDao = new WordMetadataDao();
    private final WordAudioDao wordAudioDao = new WordAudioDao();

    public Either<String,Word> loadWord(String wordText) {

        String wordLower = wordText.toLowerCase();

        List<WordMetadataItem> wordVariants = wordMetadataDao.queryWordMetadata(wordLower);

        if (wordVariants.isEmpty()) {
            return Either.left(wordText);
        }

        var chosenWordVariant = selectWordVariant(wordVariants);
        log.info("Selected word variant for '{}': {}", wordLower, chosenWordVariant);

        ByteBuffer wordVariantAudioFileData = wordAudioDao.getAudioWordFile(chosenWordVariant.getAudioFileS3Key());

        OpusStreamDecoder opusStreamDecoder = new OpusStreamDecoder(wordVariantAudioFileData);

        Queue<OpusFrame> audioFrameQueue = new LinkedList<>(opusStreamDecoder.getAudioFrames());
        return Either.right(new Word(audioFrameQueue));
    }

    private WordMetadataItem selectWordVariant(List<WordMetadataItem> variants) {
        return variants.stream()
                .filter(not(WordMetadataItem::isManuallyExcluded))
                .max(Comparator.comparing(variant -> variant.getBookLocation().getLengthMs()))
                .orElseThrow();
        // return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
    }

}
