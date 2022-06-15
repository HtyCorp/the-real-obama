package io.mamish.therealobama;

import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.List;

public class SpeakWorkflowInstance implements Runnable {

    private final ServerVoiceChannel voiceChannel;
    private final List<String> transcriptWords;

    public SpeakWorkflowInstance(ServerVoiceChannel voiceChannel, List<String> transcriptWords) {
        this.voiceChannel = voiceChannel;
        this.transcriptWords = transcriptWords;
    }

    @Override
    public void run() {
        // TODO
        // voiceChannel.connect().thenAccept( ... )
    }
}
