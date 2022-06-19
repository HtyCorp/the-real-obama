package io.mamish.therealobama;

import io.mamish.therealobama.audio.OpusFrame;
import io.mamish.therealobama.audio.Word;
import io.mamish.therealobama.codec.OpusStreamDecoder;
import io.mamish.therealobama.dao.WordAudioDao;
import io.mamish.therealobama.dao.WordMetadataDao;
import io.mamish.therealobama.dao.WordMetadataItem;
import software.amazon.awssdk.utils.Either;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class WordLoader {

    private final WordMetadataDao wordMetadataDao = new WordMetadataDao();
    private final WordAudioDao wordAudioDao = new WordAudioDao();

    public Either<String,Word> loadWord(String wordText) {

        String wordLower = wordText.toLowerCase();

        List<WordMetadataItem> wordVariants = wordMetadataDao.queryWordMetadata(wordLower);

        if (wordVariants.isEmpty()) {
            return Either.left(wordText);
        }

        var randomWordVariant = wordVariants.get(ThreadLocalRandom.current().nextInt(wordVariants.size()));

        ByteBuffer wordVariantAudioFileData = wordAudioDao.getAudioWordFile(randomWordVariant.getAudioFileS3Key());

        OpusStreamDecoder opusStreamDecoder = new OpusStreamDecoder(wordVariantAudioFileData);

        Queue<OpusFrame> audioFrameQueue = new LinkedList<>(opusStreamDecoder.getAudioFrames());
        return Either.right(new Word(audioFrameQueue));
    }

}
