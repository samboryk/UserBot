package com.userbot;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelegramUserbot {

    public static void main(String[] args) {
        while (true) {
            try {
                runBot();
            } catch (Throwable t) {
                System.err.println("Bot crashed, restarting in 5 seconds: " + t.getMessage());
                t.printStackTrace(System.err);
                sleep(5);
            }
        }
    }

    private static void runBot() throws InterruptedException {
        BotConfig config = BotConfig.load();

        Client.execute(new TdApi.SetLogVerbosityLevel(1));
        Client client = Client.create(null, null, null);

        AtomicBoolean authorized = new AtomicBoolean(false);
        CountDownLatch authLatch = new CountDownLatch(1);
        Scanner scanner = new Scanner(System.in);

        MessageSender messageSender = new MessageSender(client, config.notificationChatId);
        UpdateHandler updateHandler = new UpdateHandler(
                client,
                config.sourceChannelChatId,
                config.keywords,
                messageSender,
                config.notificationChatId
        );

        client.send(new TdApi.GetAuthorizationState(), state -> {
            if (state instanceof TdApi.AuthorizationState authorizationState) {
                onAuthorizationState(client, authorizationState, config, scanner, authorized, authLatch);
            }
        });

        client.setUpdatesHandler(update -> {
            if (update instanceof TdApi.UpdateAuthorizationState authUpdate) {
                onAuthorizationState(client, authUpdate.authorizationState, config, scanner, authorized, authLatch);
            }
            updateHandler.onResult(update);
        }, error -> {
            System.err.println("TDLib fatal error: " + error.message + " (" + error.code + ")");
        });

        if (!authLatch.await(180, TimeUnit.SECONDS) || !authorized.get()) {
            throw new IllegalStateException("Authorization timeout");
        }

        resolveSelfChatId(client, updateHandler);

        System.out.println("Userbot started. Listening for UpdateMessageEdited in chatId=" + config.sourceChannelChatId);

        while (authorized.get()) {
            sleep(2);
        }
    }

    private static void onAuthorizationState(
            Client client,
            TdApi.AuthorizationState state,
            BotConfig config,
            Scanner scanner,
            AtomicBoolean authorized,
            CountDownLatch authLatch
    ) {
        switch (state.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                TdApi.SetTdlibParameters params = new TdApi.SetTdlibParameters();
                params.databaseDirectory = config.databaseDirectory;
                params.filesDirectory = config.filesDirectory;
                params.useMessageDatabase = true;
                params.useSecretChats = false;
                params.apiId = config.apiId;
                params.apiHash = config.apiHash;
                params.systemLanguageCode = "en";
                params.deviceModel = "Desktop";
                params.systemVersion = "Unknown";
                params.applicationVersion = "1.0";
                params.enableStorageOptimizer = true;
                params.ignoreFileNames = true;
                sendOrThrow(client, params, "SetTdlibParameters");
            }
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                String phone = config.phoneNumber;
                if (phone == null || phone.isBlank()) {
                    System.out.print("Enter phone number (international format): ");
                    phone = scanner.nextLine().trim();
                }
                sendOrThrow(client, new TdApi.SetAuthenticationPhoneNumber(phone, null), "SetAuthenticationPhoneNumber");
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                System.out.print("Enter Telegram auth code: ");
                String code = scanner.nextLine().trim();
                sendOrThrow(client, new TdApi.CheckAuthenticationCode(code), "CheckAuthenticationCode");
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                System.out.print("Enter 2FA password: ");
                String password = scanner.nextLine();
                sendOrThrow(client, new TdApi.CheckAuthenticationPassword(password), "CheckAuthenticationPassword");
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                if (!authorized.getAndSet(true)) {
                    System.out.println("Authorization successful.");
                    authLatch.countDown();
                }
            }
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                authorized.set(false);
                System.err.println("Authorization state closed. Reconnect loop will restart bot.");
            }
            default -> {
                // other states are informational
            }
        }
    }

    private static void resolveSelfChatId(Client client, UpdateHandler updateHandler) {
        client.send(new TdApi.GetMe(), userResult -> {
            if (!(userResult instanceof TdApi.User me)) {
                return;
            }
            client.send(new TdApi.CreatePrivateChat(me.id, true), chatResult -> {
                if (chatResult instanceof TdApi.Chat chat) {
                    updateHandler.setTargetChatId(chat.id);
                    System.out.println("Resolved Saved Messages chatId=" + chat.id);
                }
            });
        });
    }

    private static void sendOrThrow(Client client, TdApi.Function<?> request, String op) {
        client.send(request, result -> {
            if (result instanceof TdApi.Error error) {
                throw new IllegalStateException(op + " failed: " + error.message + " (" + error.code + ")");
            }
        });
    }

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
