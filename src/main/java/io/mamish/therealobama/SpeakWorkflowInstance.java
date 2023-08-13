package io.mamish.therealobama;

import io.mamish.therealobama.audio.Sentence;
import io.mamish.therealobama.audio.SentenceAudioSource;
import io.mamish.therealobama.audio.Word;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.interaction.SlashCommandInteraction;
import software.amazon.awssdk.utils.Either;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class SpeakWorkflowInstance implements Runnable {

    private static final long SPEECH_LIMIT_SECONDS = 45;

    private final SlashCommandInteraction interaction;
    private final InteractionMessageUpdater messageUpdater;
    private final ServerVoiceChannel voiceChannel;
    private final String characterId;
    private final String characterDisplayName;
    private final List<String> transcriptWords;
    private final WordLoader wordLoader;
    private final Lock serverVoiceLock;

    public SpeakWorkflowInstance(SlashCommandInteraction interaction, InteractionMessageUpdater messageUpdater,
                                 ServerVoiceChannel voiceChannel, String characterId, String characterDisplayName,
                                 List<String> transcriptWords, WordLoader wordLoader, Lock serverVoiceLock) {
        this.interaction = interaction;
        this.messageUpdater = messageUpdater;
        this.voiceChannel = voiceChannel;
        this.characterId = characterId;
        this.characterDisplayName = characterDisplayName;
        this.transcriptWords = transcriptWords;
        this.wordLoader = wordLoader;
        this.serverVoiceLock = serverVoiceLock;
    }

    @Override
    public void run() {
        messageUpdater.set(characterFmt("%s is preparing his speech..."));
        Sentence sentence = loadSentence();
        if (sentence == null) {
            return;
        }

        if (!serverVoiceLock.tryLock()) {
            messageUpdater.set(characterFmt("%s is still finishing up another speech in this server..."));
            serverVoiceLock.lock();
        }

        try {
            messageUpdater.set(characterFmt("%s will be speaking shortly..."));
            AudioConnection voiceAudioConnection = joinVoiceChannel();

            messageUpdater.set(characterFmt("%s is speaking..."));
            completeSpeech(sentence, voiceAudioConnection);

            messageUpdater.set(finalSignoffText());
            leaveVoice(voiceAudioConnection);

        } finally {
            // Always release voice lock, even if an exception occurs
            serverVoiceLock.unlock();
        }
    }

    private Sentence loadSentence() {
        List<Either<String,Word>> maybeWords = transcriptWords.parallelStream()
                .map(w -> wordLoader.loadWord(characterId, w))
                .collect(Collectors.toList());

        List<String> unknownWords = maybeWords.stream()
                .flatMap(e -> e.left().stream())
                .collect(Collectors.toList());

        if (unknownWords.isEmpty()) {
            // All words are known
            Queue<Word> wordQueue = maybeWords.stream()
                    .map(e -> e.right().get())
                    .collect(Collectors.toCollection(LinkedList::new));
            return new Sentence(wordQueue);
        } else {
            // Some words were unrecognised so we proceed with speech
            messageUpdater.append("The following words are unknown and can't be used: " + String.join(", ", unknownWords));
            return null;
        }
    }

    private AudioConnection joinVoiceChannel() {
        return voiceChannel.connect(false, false).join();
    }

    private void completeSpeech(Sentence sentence, AudioConnection audioConnection) {
        CompletableFuture<Void> waitSpeechFinished = new CompletableFuture<>();
        AudioSource speechAudio = new SentenceAudioSource(interaction.getApi(), sentence, waitSpeechFinished);
        audioConnection.setAudioSource(speechAudio);
        try {
            waitSpeechFinished.get(SPEECH_LIMIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted while waiting for audio source completion");
        } catch (ExecutionException e) {
            throw new RuntimeException("Unexpected audio source execution exception", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Speech finish marker didn't complete quickly enough", e);
        }
    }

    private String finalSignoffText() {
        return transcriptWords.stream().collect(Collectors.joining(" ", "\"", "\""));
    }

    private void leaveVoice(AudioConnection audioConnection) {
        audioConnection.close().join();
    }

    private String characterFmt(String fmt) {
        return String.format(fmt, characterDisplayName);
    }
}
