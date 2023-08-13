package io.mamish.therealobama;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class InteractionHandler implements SlashCommandCreateListener {

    private static final long COMMAND_TIMEOUT_SECONDS = 60;
    private static final String COMMAND_DESCRIPTION_FMT = "Have the real %s say something in your voice channel";
    private static final String SCRIPT_ARG_NAME = "script";
    private static final String SCRIPT_ARG_DESCRIPTION = "The full script to read";

    private static final String DEFAULTDANCE_COMMAND_NAME = "default";
    private static final String DEFAULTDANCE_COMMAND_DESCRIPTION = "now that's pretty poggers";

    private final WordLoader wordLoader = new WordLoader();
    private final Map<Long, Lock> serverIdToVoiceLock = new ConcurrentHashMap<>();

    public void putSlashCommands(DiscordApi discordApi, DiscordApi hamishCharacterDiscordApi) {

        SlashCommand.with("obamasay", String.format(COMMAND_DESCRIPTION_FMT, "Obama"), List.of(SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                SCRIPT_ARG_NAME,
                SCRIPT_ARG_DESCRIPTION,
                true
        ))).createGlobal(discordApi).join();

        SlashCommand.with("hamishsay", String.format(COMMAND_DESCRIPTION_FMT, "Hamish"), List.of(SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                SCRIPT_ARG_NAME,
                SCRIPT_ARG_DESCRIPTION,
                true
        ))).createGlobal(hamishCharacterDiscordApi).join();

        SlashCommand.with(DEFAULTDANCE_COMMAND_NAME, DEFAULTDANCE_COMMAND_DESCRIPTION).createGlobal(discordApi).join();
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        var slashCommandInteraction = event.getInteraction().asSlashCommandInteraction().orElseThrow();
        if (slashCommandInteraction.getCommandName().equals("obamasay")) {
            String script = event.getSlashCommandInteraction().getOptionByIndex(0).flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow();
            handleCharacterSayCommand(slashCommandInteraction, script, "obama", "Obama");
        } else if (slashCommandInteraction.getCommandName().equals("hamishsay")) {
            String script = event.getSlashCommandInteraction().getOptionByIndex(0).flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow();
            handleCharacterSayCommand(slashCommandInteraction, script, "hamish", "Hamish");
        } else if (slashCommandInteraction.getCommandName().equals(DEFAULTDANCE_COMMAND_NAME)) {
            handleDefaultdanceCommand(slashCommandInteraction);
        } else {
            handleUnknownCommand(slashCommandInteraction);
        }
    }

    private void handleCharacterSayCommand(SlashCommandInteraction command, String script, String characterId, String characterDisplayName) {
        InteractionMessageUpdater messageUpdater = new InteractionMessageUpdater(command);
        var maybeUserVoiceChannel = command.getUser().getConnectedVoiceChannels().stream().findAny();
        if (maybeUserVoiceChannel.isEmpty()) {
            messageUpdater.set("You must be in a voice channel to use this");
            return;
        }

        var transcriptWords = tokenizeScript(script);

        if (transcriptWords.isEmpty()) {
            messageUpdater.set("The script cannot be empty");
            return;
        }

        var voiceLock = serverIdToVoiceLock.computeIfAbsent(maybeUserVoiceChannel.get().getServer().getId(), _id -> new ReentrantLock(true));

        SpeakWorkflowInstance workflowInstance = new SpeakWorkflowInstance(
                command, messageUpdater, maybeUserVoiceChannel.get(), characterId, characterDisplayName,
                transcriptWords, wordLoader, voiceLock
        );
        CompletableFuture<Void> workflowFuture = CompletableFuture.runAsync(workflowInstance);
        CompletableFuture.runAsync(() -> {
            try {
                workflowFuture.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                messageUpdater.append("The speech was unexpectedly interrupted");
            } catch (ExecutionException e) {
                messageUpdater.append("The speech ran into a problem (details for nerds: " + e.getCause().getMessage() + ")");
            } catch (TimeoutException e) {
                messageUpdater.append("The speech timed out");
            } finally {
                workflowFuture.cancel(true);
            }
        });
    }

    private void handleDefaultdanceCommand(SlashCommandInteraction command) {
        InteractionMessageUpdater messageUpdater = new InteractionMessageUpdater(command);
        var maybeUserVoiceChannel = command.getUser().getConnectedVoiceChannels().stream().findAny();
        if (maybeUserVoiceChannel.isEmpty()) {
            messageUpdater.set("You need to be in a voice channel for this");
            return;
        }

        var defaultdanceWorkflowInstance = new DefaultDanceWorkflowInstance(
                command, maybeUserVoiceChannel.get(), messageUpdater
        );
        CompletableFuture<Void> workflowFuture = CompletableFuture.runAsync(defaultdanceWorkflowInstance);
        CompletableFuture.runAsync(() -> {
            try {
                workflowFuture.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                messageUpdater.append("Dance unexpectedly interrupted");
            } catch (ExecutionException e) {
                messageUpdater.append("Dance ran into a problem: " + e.getCause().getMessage());
            } catch (TimeoutException e) {
                messageUpdater.append("Dance ran out of time");
            } finally {
                workflowFuture.cancel(true);
            }
        });
    }

    private void handleUnknownCommand(SlashCommandInteraction command) {
        new InteractionMessageUpdater(command).append("This isn't supposed to happen - I don't recognise that command");
    }

    private List<String> tokenizeScript(String script) {
        return Arrays.stream(script.split("[^A-Za-z\\d':\\-]+"))
                .filter(not(String::isBlank))
                .collect(Collectors.toList());
    }
}
