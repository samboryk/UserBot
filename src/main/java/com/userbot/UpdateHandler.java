package com.userbot;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateHandler implements Client.ResultHandler {
    private final Client client;
    private final long sourceChatId;
    private final Set<String> keywords;
    private final MessageSender messageSender;
    private volatile long targetChatId;

    public UpdateHandler(
            Client client,
            long sourceChatId,
            String keywordsCsv,
            MessageSender messageSender,
            long configuredNotificationChatId
    ) {
        this.client = client;
        this.sourceChatId = sourceChatId;
        this.messageSender = messageSender;
        this.targetChatId = configuredNotificationChatId;
        this.keywords = parseKeywords(keywordsCsv);
    }

    public void setTargetChatId(long targetChatId) {
        if (targetChatId != 0) {
            this.targetChatId = targetChatId;
        }
    }

    @Override
    public void onResult(TdApi.Object object) {
        switch (object.getConstructor()) {
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> onMessageEdited((TdApi.UpdateMessageEdited) object);
            case TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                TdApi.UpdateAuthorizationState update = (TdApi.UpdateAuthorizationState) object;
                System.out.println("Authorization state: " + update.authorizationState.getClass().getSimpleName());
            }
            default -> {
                // no-op
            }
        }
    }

    private void onMessageEdited(TdApi.UpdateMessageEdited update) {
        if (update.chatId != sourceChatId) {
            return;
        }

        client.send(new TdApi.GetMessage(update.chatId, update.messageId), result -> {
            if (result instanceof TdApi.Error error) {
                System.err.printf("GetMessage failed for chatId=%d messageId=%d: %s (%d)%n",
                        update.chatId, update.messageId, error.message, error.code);
                return;
            }

            TdApi.Message message = (TdApi.Message) result;
            String text = MessageSender.extractText(message.content);

            if (!keywords.isEmpty() && keywords.stream().noneMatch(k -> text.toLowerCase(Locale.ROOT).contains(k))) {
                return;
            }

            System.out.printf("Edited message detected | chatId=%d | messageId=%d | editDate=%d | text=%s%n",
                    message.chatId,
                    message.id,
                    message.editDate,
                    text);

            messageSender.sendEditedMessageAlert(targetChatId, message);
        });
    }

    private static Set<String> parseKeywords(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
