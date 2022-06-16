package io.mamish.therealobama;

import io.mamish.therealobama.audio.Sentence;
import io.mamish.therealobama.audio.SentenceAudioSource;
import io.mamish.therealobama.audio.Word;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SpeakWorkflowInstance implements Runnable {

    private static final long POST_JOIN_PAUSE_MILLIS = 1000;

    private final SlashCommandInteraction interaction;
    private final InteractionMessageUpdater messageUpdater;
    private final ServerVoiceChannel voiceChannel;
    private final List<String> transcriptWords;
    private final WordLoader wordLoader;

    private InteractionOriginalResponseUpdater responseUpdater;

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
        messageUpdater.println("Obama is preparing his speech...");
        Sentence sentence = loadSentence();

        messageUpdater.println("Obama will be giving his speech shortly...");
        AudioConnection voiceAudioConnection = joinVoiceChannel();
        sleepMillis(POST_JOIN_PAUSE_MILLIS);

        messageUpdater.println("Obama is giving his speech...");
        completeSpeech(sentence, voiceAudioConnection);

        messageUpdater.println("Obama has spoken");
        leaveVoice(voiceAudioConnection);
    }

    private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread unexpectedly interrupted");
        }
    }

    private Sentence loadSentence() {
        Queue<Word> words = transcriptWords.parallelStream()
                .map(wordLoader::loadWord)
                .collect(Collectors.toCollection(LinkedList::new));
        return new Sentence(words);
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
