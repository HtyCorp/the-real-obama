package io.mamish.therealobama;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class InteractionHandler implements SlashCommandCreateListener {

    private static final String COMMAND_NAME = "obamasay";
    private static final String COMMAND_DESCRIPTION = "Have the real Obama say something in your voice channel";
    private static final String SCRIPT_ARG_NAME = "script";
    private static final String SCRIPT_ARG_DESCRIPTION = "The full script for Obama to say";

    public void putSlashCommands(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, COMMAND_DESCRIPTION, List.of(SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                SCRIPT_ARG_NAME,
                SCRIPT_ARG_DESCRIPTION,
                true
        ))).createGlobal(discordApi).join();
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        var slashCommandInteraction = event.getInteraction().asSlashCommandInteraction().orElseThrow();
        if (slashCommandInteraction.getCommandName().equals(COMMAND_NAME)) {
            String script = event.getSlashCommandInteraction().getOptionByIndex(0).flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow();
            handleObamaSayCommand(slashCommandInteraction, script);
        }

        handleUnknownCommand(slashCommandInteraction);
    }

    private void handleObamaSayCommand(SlashCommandInteraction command, String script) {
        var maybeUserVoiceChannel = command.getUser().getConnectedVoiceChannels().stream().findAny();
        if (maybeUserVoiceChannel.isEmpty()) {
            respondImmediately(command, "Obama can't speak unless you're in a voice channel");
        }

        // TODO
        var transcriptWords = tokenizeScript(script);
    }

    private void handleUnknownCommand(SlashCommandInteraction command) {
        respondImmediately(command, "Sorry, I'm not programmed to understand that command.");
    }

    private List<String> tokenizeScript(String script) {
        return Arrays.stream(script.split("[^A-Za-z\\d]+"))
                .filter(not(String::isBlank))
                .collect(Collectors.toList());
    }

    private void respondImmediately(SlashCommandInteraction command, String response) {
        command.createImmediateResponder()
                .append(response)
                .respond()
                .join();
    }
}
