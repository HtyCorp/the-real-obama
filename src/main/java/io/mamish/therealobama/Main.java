package io.mamish.therealobama;

import org.javacord.api.DiscordApiBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class Main {

    private static final SecretsManagerClient secretsManagerClient = SecretsManagerClient.create();

    public static void main(String[] args) {

        var interactionHandler = new InteractionHandler();
        var discordApi = new DiscordApiBuilder()
                .setToken(fetchBotToken())
                .addSlashCommandCreateListener(interactionHandler)
                .login()
                .join();
        var hamishCharacterDiscordApi = new DiscordApiBuilder()
                .setToken(fetchHamishCharacterBotToken())
                .addSlashCommandCreateListener(interactionHandler)
                .login()
                .join();
        interactionHandler.putSlashCommands(discordApi, hamishCharacterDiscordApi);
    }

    private static String fetchBotToken() {
        return secretsManagerClient.getSecretValue(r -> r.secretId("DiscordBotToken")).secretString();
    }

    private static String fetchHamishCharacterBotToken() {
        return secretsManagerClient.getSecretValue(r -> r.secretId("HamishCharacterDiscordBotToken")).secretString();
    }
}
