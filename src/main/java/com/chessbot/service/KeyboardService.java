package com.chessbot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {

    /**
     * 🎮 ОСНОВНАЯ КЛАВИАТУРА
     */
    public ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/newgame"));
        row1.add(new KeyboardButton("/listgames"));
        keyboard.add(row1);

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/board"));
        row2.add(new KeyboardButton("/moves"));
        keyboard.add(row2);

        // Третий ряд
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("/draw"));
        row3.add(new KeyboardButton("/resign"));
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * ♟️ КЛАВИАТУРА ДЛЯ ХОДОВ (inline)
     */
    public InlineKeyboardMarkup createMovesKeyboard(List<String> legalMoves) {
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Группируем ходы по 4 в ряд
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (String move : legalMoves) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(move);
            button.setCallbackData("/move " + move);

            currentRow.add(button);

            if (currentRow.size() >= 4) {
                keyboard.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }

        // Добавляем оставшиеся кнопки
        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    /**
     * 🤝 КЛАВИАТУРА ДЛЯ НИЧЬЕЙ
     */
    public InlineKeyboardMarkup createDrawKeyboard(String gameId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        // Кнопка "Принять ничью"
        InlineKeyboardButton acceptButton = new InlineKeyboardButton();
        acceptButton.setText("🤝 Принять ничью");
        acceptButton.setCallbackData("/draw accept " + gameId);
        row.add(acceptButton);

        // Кнопка "Отклонить"
        InlineKeyboardButton declineButton = new InlineKeyboardButton();
        declineButton.setText("❌ Отклонить");
        declineButton.setCallbackData("/draw decline " + gameId);
        row.add(declineButton);

        keyboard.add(row);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    /**
     * 🎮 КЛАВИАТУРА ДЛЯ НОВОЙ ИГРЫ
     */
    public InlineKeyboardMarkup createNewGameKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("🎮 Создать новую игру");
        button.setCallbackData("/newgame");

        row.add(button);
        keyboard.add(row);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}