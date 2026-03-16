# Telegram Userbot (Java + TDLib)

Userbot для відслідковування редагувань постів у конкретному каналі Telegram та миттєвого сповіщення у Saved Messages (або інший чат).

## Можливості
- Авторизація через `api_id` + `api_hash` (перший запуск: телефон + код).
- Збереження TDLib-сесії на диск (повторна авторизація не потрібна).
- Обробка `UpdateMessageEdited` для заданого `source_chat_id`.
- Опціональний фільтр по ключових словах (`keywords`).
- Відправка повідомлення з:
  - текстом редагованого поста,
  - `messageId`,
  - часом редагування.
- Логування в консоль.
- Безкінечний цикл перезапуску при аварії або втраті з'єднання.

## Вимоги
- Java 17+
- Maven 3.9+

## Налаштування
Заповніть `src/main/resources/config.properties`:

```properties
api_id=123456
api_hash=xxxxxxxxxxxxxxxxxxxxxxxx
source_chat_id=-1001234567890

# 0 = автоматично надсилати в Saved Messages
notification_chat_id=0

phone_number=+380...
keywords=promo,news
database_directory=tdlib-data
files_directory=tdlib-files
```

> Також можна задавати значення через environment variables: `API_ID`, `API_HASH`, `SOURCE_CHAT_ID`, тощо.

## Запуск
```bash
mvn clean package
java -jar target/telegram-userbot-1.0.0.jar
```

При першому запуску бот попросить код підтвердження (та 2FA пароль, якщо увімкнений).

## Структура
- `TelegramUserbot` — старт TDLib, авторизація, автоперезапуск.
- `UpdateHandler` — слухає `UpdateMessageEdited`, фільтрує за `chatId`/`keywords`.
- `MessageSender` — формує та відправляє алерти.
- `BotConfig` — завантажує конфіг з properties/env.
