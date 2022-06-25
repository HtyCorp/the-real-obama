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
    private static final String COMMAND_NAME = "obamasay";
    private static final String COMMAND_DESCRIPTION = "Have the real Obama say something in your voice channel";
    private static final String SCRIPT_ARG_NAME = "script";
    private static final String SCRIPT_ARG_DESCRIPTION = "The full script for Obama to say";

    private static final String DEFAULTDANCE_COMMAND_NAME = "default";
    private static final String DEFAULTDANCE_COMMAND_DESCRIPTION = "now that's pretty poggers";

    private final WordLoader wordLoader = new WordLoader();
    private final Map<Long, Lock> serverIdToVoiceLock = new ConcurrentHashMap<>();

    public void putSlashCommands(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, COMMAND_DESCRIPTION, List.of(SlashCommandOption.create(
                SlashCommandOptionType.STRING,
                SCRIPT_ARG_NAME,
                SCRIPT_ARG_DESCRIPTION,
                true
        ))).createGlobal(discordApi).join();

        SlashCommand.with(DEFAULTDANCE_COMMAND_NAME, DEFAULTDANCE_COMMAND_DESCRIPTION).createGlobal(discordApi).join();
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        var slashCommandInteraction = event.getInteraction().asSlashCommandInteraction().orElseThrow();
        if (slashCommandInteraction.getCommandName().equals(COMMAND_NAME)) {
            String script = event.getSlashCommandInteraction().getOptionByIndex(0).flatMap(SlashCommandInteractionOption::getStringValue).orElseThrow();
            handleObamaSayCommand(slashCommandInteraction, script);
        } else if (slashCommandInteraction.getCommandName().equals(DEFAULTDANCE_COMMAND_NAME)) {
            handleDefaultdanceCommand(slashCommandInteraction);
        } else {
            handleUnknownCommand(slashCommandInteraction);
        }
    }

    private void handleObamaSayCommand(SlashCommandInteraction command, String script) {
        InteractionMessageUpdater messageUpdater = new InteractionMessageUpdater(command);
        var maybeUserVoiceChannel = command.getUser().getConnectedVoiceChannels().stream().findAny();
        if (maybeUserVoiceChannel.isEmpty()) {
            messageUpdater.set("Obama can't speak unless you're in a voice channel");
            return;
        }

        var transcriptWords = tokenizeScript(script);

        if (transcriptWords.isEmpty()) {
            messageUpdater.set("Obama can't work with an empty script");
            return;
        }

        var voiceLock = serverIdToVoiceLock.computeIfAbsent(maybeUserVoiceChannel.get().getServer().getId(), _id -> new ReentrantLock(true));

        SpeakWorkflowInstance workflowInstance = new SpeakWorkflowInstance(
                command, messageUpdater, maybeUserVoiceChannel.get(), transcriptWords, wordLoader, voiceLock
        );
        CompletableFuture<Void> workflowFuture = CompletableFuture.runAsync(workflowInstance);
        CompletableFuture.runAsync(() -> {
            try {
                workflowFuture.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                messageUpdater.append("Obama was unexpectedly interrupted");
            } catch (ExecutionException e) {
                messageUpdater.append("Obama ran into a problem, apparently '" + e.getCause().getMessage() + "'");
            } catch (TimeoutException e) {
                messageUpdater.append("Obama has run out of time to give his speech");
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
        return Arrays.stream(script.split("[^A-Za-z\\d']+"))
                .filter(not(String::isBlank))
                .collect(Collectors.toList());
    }
}
