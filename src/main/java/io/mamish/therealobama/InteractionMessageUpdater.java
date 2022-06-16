package io.mamish.therealobama;

import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.ArrayList;
import java.util.List;

public class InteractionMessageUpdater {

    private final InteractionBase interaction;
    private final List<String> messageLines = new ArrayList<>();

    private InteractionOriginalResponseUpdater interactionUpdater;

    public InteractionMessageUpdater(InteractionBase interaction) {
        this.interaction = interaction;
    }

    public void set(String message) {
        messageLines.clear();
        append(message);
    }

    public void append(String message) {
        messageLines.add(message);
        updateInteraction();
    }

    private void updateInteraction() {
        String newMessageContent = String.join("\n", messageLines);
        if (interactionUpdater == null) {
            interactionUpdater = interaction.createImmediateResponder()
                    .setContent(newMessageContent)
                    .respond()
                    .join();
        } else {
            interactionUpdater.setContent(newMessageContent).update().join();
        }
    }

}
