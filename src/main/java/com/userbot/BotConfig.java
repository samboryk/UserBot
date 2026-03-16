package com.userbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class BotConfig {
    private static final String CONFIG_RESOURCE = "config.properties";

    public final int apiId;
    public final String apiHash;
    public final long sourceChannelChatId;
    public final long notificationChatId;
    public final String phoneNumber;
    public final String keywords;
    public final String databaseDirectory;
    public final String filesDirectory;

    private BotConfig(
            int apiId,
            String apiHash,
            long sourceChannelChatId,
            long notificationChatId,
            String phoneNumber,
            String keywords,
            String databaseDirectory,
            String filesDirectory
    ) {
        this.apiId = apiId;
        this.apiHash = apiHash;
        this.sourceChannelChatId = sourceChannelChatId;
        this.notificationChatId = notificationChatId;
        this.phoneNumber = phoneNumber;
        this.keywords = keywords;
        this.databaseDirectory = databaseDirectory;
        this.filesDirectory = filesDirectory;
    }

    public static BotConfig load() {
        Properties properties = new Properties();
        try (InputStream is = BotConfig.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load config.properties", e);
        }

        int apiId = Integer.parseInt(readRequired("api_id", properties));
        String apiHash = readRequired("api_hash", properties);
        long sourceChatId = Long.parseLong(readRequired("source_chat_id", properties));

        long notificationChatId = Long.parseLong(readOptional("notification_chat_id", properties).orElse("0"));
        String phoneNumber = readOptional("phone_number", properties).orElse("");
        String keywords = readOptional("keywords", properties).orElse("");
        String dbDir = readOptional("database_directory", properties).orElse("tdlib-data");
        String filesDir = readOptional("files_directory", properties).orElse("tdlib-files");

        return new BotConfig(apiId, apiHash, sourceChatId, notificationChatId, phoneNumber, keywords, dbDir, filesDir);
    }

    private static String readRequired(String key, Properties props) {
        return readOptional(key, props)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Missing required config key: " + key));
    }

    private static Optional<String> readOptional(String key, Properties props) {
        String env = System.getenv(key.toUpperCase());
        if (env != null && !env.isBlank()) {
            return Optional.of(env.trim());
        }
        String property = props.getProperty(key);
        if (property != null && !property.isBlank()) {
            return Optional.of(property.trim());
        }
        return Optional.empty();
    }
}
