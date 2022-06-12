package io.mamish.therealobama;

import org.javacord.api.DiscordApiBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class Main {

    private static final String BOT_TOKEN_SECRET_NAME = "DiscordBotToken";

    private static final SecretsManagerClient secretsManagerClient = SecretsManagerClient.create();

    public static void main(String[] args) {
        var botTokenString = fetchBotToken();
        var interactionHandler = new InteractionHandler();
        var discordApi = new DiscordApiBuilder()
                .setToken(botTokenString)
                .addSlashCommandCreateListener(interactionHandler)
                .login()
                .join();
        interactionHandler.putSlashCommands(discordApi);
    }

    private static String fetchBotToken() {
        return secretsManagerClient.getSecretValue(r -> r.secretId(BOT_TOKEN_SECRET_NAME)).secretString();
    }

}
