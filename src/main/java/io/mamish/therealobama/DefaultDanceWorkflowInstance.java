package io.mamish.therealobama;

import io.mamish.therealobama.audio.Sentence;
import io.mamish.therealobama.audio.SentenceAudioSource;
import io.mamish.therealobama.audio.Word;
import io.mamish.therealobama.codec.OpusStreamDecoder;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DefaultDanceWorkflowInstance implements Runnable {

    private static final long FRAME_INTERVAL_MILLIS = 1334; // Approx 90bpm or 2/3 seconds interval, at half rate due to discord message update rate limits

    private final SlashCommandInteraction interaction;
    private final ServerVoiceChannel voiceChannel;
    private final InteractionMessageUpdater messageUpdater;

    public DefaultDanceWorkflowInstance(SlashCommandInteraction interaction, ServerVoiceChannel voiceChannel, InteractionMessageUpdater messageUpdater) {
        this.interaction = interaction;
        this.voiceChannel = voiceChannel;
        this.messageUpdater = messageUpdater;
    }

    public void run() {
        var sentence = loadDefaultDanceAudio();
        var audioFinishedFuture = new CompletableFuture<Void>();
        var audioConnection = startPlayingAudio(sentence, audioFinishedFuture);

        MemeConstants.DANCE_FRAMES.forEach(frame -> {
            messageUpdater.set(frame);
            sleepFrameInterval();
        });

        try {
            audioFinishedFuture.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed waiting for audio completion", e);
        }

        audioConnection.close().join();
    }

    private Sentence loadDefaultDanceAudio() {
        byte[] defaultDanceOpusBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream("audio/defaultdance.opus")){
            defaultDanceOpusBytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var decoder = new OpusStreamDecoder(ByteBuffer.wrap(defaultDanceOpusBytes));

        // This is kind of abusing the "word" terminology but whatever
        var audioPseudoWord = new Word(new LinkedList<>(decoder.getAudioFrames()));
        return new Sentence(new LinkedList<>(Collections.singleton(audioPseudoWord)));
    }

    private AudioConnection startPlayingAudio(Sentence sentence, CompletableFuture<Void> finishedFuture) {
        AudioSource audioSource = new SentenceAudioSource(interaction.getApi(), sentence, finishedFuture);
        AudioConnection connection = voiceChannel.connect(false, false).join();
        connection.setAudioSource(audioSource);
        return connection;
    }

    private void sleepFrameInterval() {
        try {
            Thread.sleep(FRAME_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
