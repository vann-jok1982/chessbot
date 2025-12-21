package com.chessbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Slf4j
@Service
public class InteractiveBoardService {

    // Эмодзи для фигур
    private static final Map<Character, String> PIECE_EMOJIS = new HashMap<>();

    static {
        PIECE_EMOJIS.put('K', "♔");
        PIECE_EMOJIS.put('Q', "♕");
        PIECE_EMOJIS.put('R', "♖");
        PIECE_EMOJIS.put('B', "♗");
        PIECE_EMOJIS.put('N', "♘");
        PIECE_EMOJIS.put('P', "♙");
        PIECE_EMOJIS.put('k', "♚");
        PIECE_EMOJIS.put('q', "♛");
        PIECE_EMOJIS.put('r', "♜");
        PIECE_EMOJIS.put('b', "♝");
        PIECE_EMOJIS.put('n', "♞");
        PIECE_EMOJIS.put('p', "♟");
        PIECE_EMOJIS.put('.', "·");
    }

    /**
     * 🎮 СОЗДАНИЕ ИНТЕРАКТИВНОЙ ДОСКИ С КНОПКАМИ
     */
    public InteractiveBoard createInteractiveBoard(String boardFen, String playerColor, List<String> legalMoves) {
        // Парсим FEN в массив 8x8
        char[][] board = parseFenToBoard(boardFen);

        // Создаем карту легальных ходов для быстрого поиска
        Map<String, List<String>> movesFromSquare = groupMovesByFromSquare(legalMoves);

        // Создаем клавиатуру
        InlineKeyboardMarkup keyboard = createBoardKeyboard(board, playerColor, movesFromSquare);

        // Создаем текстовое представление
        String boardText = createBoardText(board, playerColor);

        return new InteractiveBoard(keyboard, boardText);
    }

    /**
     * ♟️ ПАРСИНГ FEN В ДВУМЕРНЫЙ МАССИВ (ИСПРАВЛЕННЫЙ)
     */
    private char[][] parseFenToBoard(String fen) {
        char[][] board = new char[8][8];

        // Если приходит полный FEN (с информацией о ходе), берем только часть доски
        String boardPart = fen.split(" ")[0]; // "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"

        // Заполняем доску точками (пустыми клетками)
        for (int i = 0; i < 8; i++) {
            Arrays.fill(board[i], '.');
        }

        // Разбиваем FEN на ряды
        String[] rows = boardPart.split("/");

        // Проверяем что у нас 8 рядов
        if (rows.length != 8) {
            log.error("Некорректный FEN: ожидалось 8 рядов, получено {}", rows.length);
            return createDefaultBoard();
        }

        for (int row = 0; row < 8; row++) {
            String fenRow = rows[row];
            int col = 0;

            for (char c : fenRow.toCharArray()) {
                if (Character.isDigit(c)) {
                    // Цифра означает пустые клетки
                    int emptyCells = Character.getNumericValue(c);
                    col += emptyCells;
                } else {
                    // Фигура
                    if (col < 8 && row < 8) {
                        board[row][col] = c;
                        col++;
                    } else {
                        log.warn("Координаты вне доски: row={}, col={}", row, col);
                    }
                }

                // Защита от выхода за пределы
                if (col > 8) {
                    log.warn("Столбец {} превышает размер доски для ряда {}", col, fenRow);
                    break;
                }
            }
        }

        log.debug("Парсинг FEN завершен. Доска размером {}x{}", board.length, board[0].length);
        return board;
    }

    /**
     * 🎲 СОЗДАНИЕ СТАНДАРТНОЙ ДОСКИ ПО УМОЛЧАНИЮ
     */
    private char[][] createDefaultBoard() {
        char[][] board = new char[8][8];
        String defaultFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

        // Заполняем доску точками
        for (int i = 0; i < 8; i++) {
            Arrays.fill(board[i], '.');
        }

        // Расставляем начальную позицию
        String[] rows = defaultFen.split("/");

        for (int row = 0; row < 8; row++) {
            String fenRow = rows[row];
            int col = 0;

            for (char c : fenRow.toCharArray()) {
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                } else {
                    if (col < 8) {
                        board[row][col] = c;
                        col++;
                    }
                }
            }
        }

        return board;
    }

    /**
     * 🗺️ ГРУППИРОВКА ХОДОВ ПО НАЧАЛЬНОЙ КЛЕТКЕ
     */
    private Map<String, List<String>> groupMovesByFromSquare(List<String> legalMoves) {
        Map<String, List<String>> movesMap = new HashMap<>();

        if (legalMoves == null) return movesMap;

        for (String move : legalMoves) {
            if (move != null && move.length() >= 4) {
                String fromSquare = move.substring(0, 2); // e2, g1, etc.
                movesMap.computeIfAbsent(fromSquare, k -> new ArrayList<>()).add(move);
            }
        }

        return movesMap;
    }

    /**
     * 🎹 СОЗДАНИЕ КЛАВИАТУРЫ ДОСКИ (УПРОЩЕННЫЙ ВАРИАНТ)
     */
    private InlineKeyboardMarkup createBoardKeyboard(char[][] board, String playerColor,
                                                     Map<String, List<String>> movesFromSquare) {
        boolean isBlack = "BLACK".equals(playerColor);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Создаем ряды доски
        for (int row = 0; row < 8; row++) {
            List<InlineKeyboardButton> boardRow = new ArrayList<>();

            for (int col = 0; col < 8; col++) {
                // Определяем реальные координаты с учетом цвета игрока
                int displayRow = isBlack ? row : 7 - row;
                int displayCol = isBlack ? 7 - col : col;

                char piece = board[displayRow][displayCol];
                String square = getSquareName(displayRow, displayCol);

                // Текст кнопки
                String buttonText = PIECE_EMOJIS.getOrDefault(piece, "·");

                // Создаем кнопку
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonText);

                // Если есть ходы с этой клетки, делаем ее кликабельной
                if (movesFromSquare.containsKey(square) && !movesFromSquare.get(square).isEmpty()) {
                    button.setCallbackData("select:" + square);
                } else {
                    button.setCallbackData("none");
                }

                boardRow.add(button);
            }

            keyboard.add(boardRow);
        }

        // Добавляем управляющие кнопки
        keyboard.add(createControlButtons());

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * 🎯 СОЗДАНИЕ УПРАВЛЯЮЩИХ КНОПОК
     */
    private List<InlineKeyboardButton> createControlButtons() {
        List<InlineKeyboardButton> controlRow = new ArrayList<>();

        // Кнопка "Обновить доску"
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("🔄 Обновить");
        refreshButton.setCallbackData("refresh_board");
        controlRow.add(refreshButton);

        // Кнопка "Список ходов"
        InlineKeyboardButton movesButton = new InlineKeyboardButton();
        movesButton.setText("📋 Все ходы");
        movesButton.setCallbackData("show_legal_moves");
        controlRow.add(movesButton);

        // Кнопка "Предложить ничью"
        InlineKeyboardButton drawButton = new InlineKeyboardButton();
        drawButton.setText("🤝 Ничья");
        drawButton.setCallbackData("offer_draw");
        controlRow.add(drawButton);

        return controlRow;
    }

    /**
     * 📍 ПОЛУЧЕНИЕ ИМЕНИ КЛЕТКИ
     */
    private String getSquareName(int row, int col) {
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }

    /**
     * 📝 СОЗДАНИЕ ТЕКСТОВОГО ПРЕДСТАВЛЕНИЯ ДОСКИ (УПРОЩЕННЫЙ)
     */
    private String createBoardText(char[][] board, String playerColor) {
        boolean isBlack = "BLACK".equals(playerColor);
        StringBuilder sb = new StringBuilder();

        sb.append("<pre>\n");

        // Ряды доски
        for (int row = 0; row < 8; row++) {
            int displayRow = isBlack ? row : 7 - row;
            sb.append(8 - displayRow).append(" ");

            for (int col = 0; col < 8; col++) {
                int displayCol = isBlack ? 7 - col : col;
                char piece = board[displayRow][displayCol];
                String emoji = PIECE_EMOJIS.getOrDefault(piece, "·");
                sb.append(emoji).append(" ");
            }

            sb.append("\n");
        }

        sb.append("  a b c d e f g h");
        sb.append("\n</pre>");

        return sb.toString();
    }

    /**
     * 🎮 СОЗДАНИЕ КЛАВИАТУРЫ ДЛЯ ВЫБОРА ХОДА (УПРОЩЕННЫЙ)
     */
    public InlineKeyboardMarkup createMoveSelectionKeyboard(String fromSquare, List<String> movesTo) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопки ходов (группируем по 4 в ряд)
        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        for (String move : movesTo) {
            if (move != null && move.length() >= 4) {
                String toSquare = move.length() >= 4 ? move.substring(2, 4) : move;

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("➡️ " + toSquare);
                button.setCallbackData("move:" + move);

                currentRow.add(button);

                if (currentRow.size() >= 4) {
                    keyboard.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                }
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        // Кнопка отмены
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("cancel_move");
        cancelRow.add(cancelButton);
        keyboard.add(cancelRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * 📊 DTO ДЛЯ ВОЗВРАТА ИНТЕРАКТИВНОЙ ДОСКИ
     */
    public static class InteractiveBoard {
        private final InlineKeyboardMarkup keyboard;
        private final String boardText;

        public InteractiveBoard(InlineKeyboardMarkup keyboard, String boardText) {
            this.keyboard = keyboard;
            this.boardText = boardText;
        }

        public InlineKeyboardMarkup getKeyboard() {
            return keyboard;
        }

        public String getBoardText() {
            return boardText;
        }
    }
}