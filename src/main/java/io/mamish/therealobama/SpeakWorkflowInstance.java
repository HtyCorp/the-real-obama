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
import java.util.stream.Collectors;

public class SpeakWorkflowInstance implements Runnable {

    private final SlashCommandInteraction interaction;
    private final InteractionMessageUpdater messageUpdater;
    private final ServerVoiceChannel voiceChannel;
    private final List<String> transcriptWords;
    private final WordLoader wordLoader;

    public SpeakWorkflowInstance(SlashCommandInteraction interaction, InteractionMessageUpdater messageUpdater, ServerVoiceChannel voiceChannel,
                                 List<String> transcriptWords, WordLoader wordLoader) {
        this.interaction = interaction;
        this.messageUpdater = messageUpdater;
        this.voiceChannel = voiceChannel;
        this.transcriptWords = transcriptWords;
        this.wordLoader = wordLoader;
    }

    @Override
    public void run() {
        messageUpdater.set("Obama is preparing his speech...");
        Sentence sentence = loadSentence();
        if (sentence == null) {
            return;
        }

        // TODO: Queue mechanism (either a lock or an independent per-channel voice session object) in case multiple sentences get queued up

        messageUpdater.set("Obama will be giving his speech shortly...");
        AudioConnection voiceAudioConnection = joinVoiceChannel();

        messageUpdater.set("Obama is giving his speech...");
        completeSpeech(sentence, voiceAudioConnection);

        messageUpdater.set("Obama has spoken");
        leaveVoice(voiceAudioConnection);
    }

    private Sentence loadSentence() {
        List<Either<String,Word>> maybeWords = transcriptWords.parallelStream()
                .map(wordLoader::loadWord)
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
            waitSpeechFinished.get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted while waiting for audio source completion");
        } catch (ExecutionException e) {
            throw new RuntimeException("Unexpected audio source execution exception", e);
        }
    }

    private void leaveVoice(AudioConnection audioConnection) {
        audioConnection.close().join();
    }
}
