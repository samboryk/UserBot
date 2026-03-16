package com.userbot;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageSender {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Client client;
    private final long fallbackNotificationChatId;

    public MessageSender(Client client, long fallbackNotificationChatId) {
        this.client = client;
        this.fallbackNotificationChatId = fallbackNotificationChatId;
    }

    public void sendEditedMessageAlert(long preferredTargetChatId, TdApi.Message message) {
        String text = extractText(message.content);
        long editedAt = message.editDate > 0 ? message.editDate : message.date;
        String readableTime = TIME_FORMATTER.format(Instant.ofEpochSecond(editedAt));

        String notification = "✏️ Пост відредаговано\n"
                + "chatId: " + message.chatId + "\n"
                + "messageId: " + message.id + "\n"
                + "editedAt: " + readableTime + "\n\n"
                + "Текст:\n" + text;

        long targetChatId = preferredTargetChatId != 0 ? preferredTargetChatId : fallbackNotificationChatId;
        TdApi.InputMessageContent content = new TdApi.InputMessageText(
                new TdApi.FormattedText(notification, null),
                false,
                true
        );

        client.send(new TdApi.SendMessage(
                targetChatId,
                0,
                null,
                null,
                content
        ), result -> {
            if (result instanceof TdApi.Error error) {
                System.err.println("Failed to send alert: " + error.message + " (" + error.code + ")");
            }
        });
    }

    public static String extractText(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }
        return "[Непідтримуваний тип контенту: constructor=" + content.getConstructor() + "]";
    }
}
