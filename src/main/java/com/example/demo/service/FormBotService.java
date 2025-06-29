package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.UserResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormBotService extends TelegramLongPollingBot {

    private final UserResponseRepository repository;
    private final ReportService reportService;

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        String text = message.getText();
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        if (text.equals("/start")) {
            userStates.remove(userId);
            sendMessage(chatId, "Привет! Используйте команду /form, чтобы пройти опрос.");
            return;
        }

        if (text.equals("/form")) {
            UserState state = new UserState();
            state.setStep(FormStep.NAME);
            userStates.put(userId, state);
            sendMessage(chatId, "Введите ваше имя:");
            return;
        }

        if (text.equals("/report")) {
            userStates.remove(userId);
            sendMessage(chatId, "⏳ Генерирую отчет...");

            try {
                reportService.generateReport().thenAccept(bytes -> {
                    InputFile file = new InputFile(new ByteArrayInputStream(bytes), "report.docx");
                    SendDocument doc = new SendDocument();
                    doc.setChatId(chatId.toString());
                    doc.setDocument(file);
                    try {
                        execute(doc); // здесь все еще можно, т.к. мы внутри самого бота
                    } catch (Exception e) {
                        log.error("Ошибка при отправке отчета", e);
                        sendMessage(chatId, "❌ Ошибка при отправке отчета.");
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return;
        }

        handleFormInput(userId, chatId, text);
    }

    private void handleFormInput(Long userId, Long chatId, String text) {
        if (text.equalsIgnoreCase("/exit") || text.equalsIgnoreCase("выход")) {
            userStates.remove(userId);
            sendMessage(chatId, "Вы вышли из формы. Для нового начала введите /form.");
            return;
        }

        UserState state = userStates.get(userId);
        if (state == null) {
            sendMessage(chatId, "Введите /form чтобы начать опрос.");
            return;
        }

        switch (state.getStep()) {
            case NAME -> {
                state.setName(text);
                state.setStep(FormStep.EMAIL);
                sendMessage(chatId, "Введите email:");
            }
            case EMAIL -> {
                if (!text.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    sendMessage(chatId, "Некорректный email. Попробуйте снова:");
                    return;
                }
                state.setEmail(text);
                state.setStep(FormStep.SCORE);
                sendMessage(chatId, "Оцените наш сервис от 1 до 10:");
            }
            case SCORE -> {
                try {
                    int score = Integer.parseInt(text);
                    if (score < 1 || score > 10) {
                        sendMessage(chatId, "Оценка должна быть от 1 до 10. Введите снова:");
                        return;
                    }

                    UserResponse response = UserResponse.builder()
                            .telegramUserId(userId)
                            .name(state.getName())
                            .email(state.getEmail())
                            .score(score)
                            .build();
                    repository.save(response);

                    userStates.remove(userId);
                    sendMessage(chatId, "Спасибо! Ваши данные сохранены.");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Введите число от 1 до 10.");
                }
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        try {
            execute(msg);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
