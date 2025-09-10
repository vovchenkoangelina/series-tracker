package series_tracker.bot;

import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import series_tracker.service.SeriesService;

import java.util.List;

@Service
public class SeriesBot extends TelegramLongPollingBot {

    private final SeriesService seriesService;

    @Value("${telegram.bot.token}")
    private String botToken;

    public SeriesBot(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) throws TelegramApiException {
        String text = message.getText();
        long chatId = message.getChatId();

        if (text.toLowerCase().startsWith("начать")) {
            String name = text.substring(7);
            seriesService.addSeries(name, chatId);
            sendMessage(chatId, "Принято. Приятного просмотра!");
        }

        if (text.contains("сезон")) {
            String name = text.substring(0, text.indexOf("сезон") - 1);
            int season = Integer.parseInt(text.substring(text.lastIndexOf(" ") + 1));
            seriesService.checkSeason(seriesService.findByName(name).getId(), season);
        }

        if (text.contains("серия")) {
            String name = text.substring(0, text.indexOf("серия") - 1);
            int episode = Integer.parseInt(text.substring(text.lastIndexOf(" ") + 1));
            seriesService.checkEpisode(seriesService.findByName(name).getId(), episode);
        }

        switch (text) {
            case "/start", "старт" -> {
                sendMessage(chatId, "Привет! Этот бот запоминает, на какой серии сериала вы остановились. Используйте кнопки или напишите /команды, чтобы узнать, как добавлять новые сериалы и отмечать серии без кнопок.");
                sendMenu(chatId);
            }
            case "/menu", "меню" -> sendMenu(chatId);
            case "/list", "список" -> sendSeriesList(chatId);
            case "/команды" -> sendCommands(chatId);
            case "finished" -> {
                var finished = seriesService.getFinishedByChatId(chatId);
                if (finished.isEmpty()) {
                    sendMessage(chatId, "Законченных сериалов пока нет");
                } else {
                    StringBuilder sb = new StringBuilder("Просмотренные сериалы:\n\n");
                    for (var s : finished) {
                        long days = seriesService.watchlasting(s.getId());
                        sb.append(s.getName())
                                .append(" (").append(days).append(" дн. в процессе)")
                                .append("\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }
            default -> sendMessage(chatId, "Неизвестная команда. Используй /menu для выбора действий.");
        }
    }

    private void sendCommands(long chatId) throws TelegramApiException {
        String text = "Чтобы добавить новый сериал, напишите слово \"начать\", пробел и название сериала. Например \"Начать Декстер\". " +
                "Чтобы отметить серию, напишите название сериала и номер серии через пробел. Например \"Декстер 17\". " +
                "Чтобы сменить сезон, напишите название сериала, пробел, слово \"сезон\" и номер сезона через пробел. Например \"Декстер сезон 4\".";
        sendMessage(chatId, text);
    }

    private void sendMessage(long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }

    private void sendMenu(long chatId) throws TelegramApiException {
        InlineKeyboardButton addBtn = new InlineKeyboardButton("Добавить сериал");
        addBtn.setCallbackData("add");
        InlineKeyboardButton listBtn = new InlineKeyboardButton("Сериалы в процессе");
        listBtn.setCallbackData("list");
        InlineKeyboardButton finishedBtn = new InlineKeyboardButton("Законченные сериалы");
        finishedBtn.setCallbackData("finished");
        sendInlineKeyboard(chatId, "Выберите действие:", List.of(List.of(addBtn, listBtn, finishedBtn)));
    }

    private void sendInlineKeyboard(long chatId, String text, List<List<InlineKeyboardButton>> buttons) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);

        message.setReplyMarkup(markup);
        execute(message);
    }


    private InlineKeyboardMarkup buildSeriesKeyboard(Long seriesId) {
        InlineKeyboardButton episodeBtn = new InlineKeyboardButton("+ серия");
        episodeBtn.setCallbackData("episode:" + seriesId);

        InlineKeyboardButton seasonBtn = new InlineKeyboardButton("+ сезон");
        seasonBtn.setCallbackData("season:" + seriesId);

        InlineKeyboardButton finishBtn = new InlineKeyboardButton("Закончить");
        finishBtn.setCallbackData("finish:" + seriesId);

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(episodeBtn, seasonBtn, finishBtn)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private void sendSeriesList(long chatId) throws TelegramApiException {
        var inProgress = seriesService.getInProgressByChatId(chatId);
        if (inProgress.isEmpty()) {
            sendMessage(chatId, "У вас пока нет сериалов в процессе 📺");
        } else {
            for (var s : inProgress) {
                long days = seriesService.watchlasting(s.getId());
                SendMessage msg = new SendMessage();
                msg.setChatId(chatId);
                msg.setText(
                        s.getName() + " — Сезон " + s.getSeason() + ", Серия " + s.getEpisode() +
                                "\nСмотрю уже " + days + " дн."
                );
                msg.setReplyMarkup(buildSeriesKeyboard(s.getId()));
                execute(msg);
            }
        }
    }

    private void handleCallback(Update update) throws TelegramApiException {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("episode:")) {
            Long seriesId = Long.parseLong(data.split(":")[1]);
            seriesService.checkEpisode(seriesId,
                    seriesService.findById(seriesId).getEpisode() + 1);
            sendMessage(chatId, "Готово!");
        }

        if (data.startsWith("season:")) {
            Long seriesId = Long.parseLong(data.split(":")[1]);
            seriesService.checkSeason(seriesId,
                    seriesService.findById(seriesId).getSeason() + 1);
            sendMessage(chatId, "Начали новый сезон!");
        }

        if (data.startsWith("finish:")) {
            Long seriesId = Long.parseLong(data.split(":")[1]);
            seriesService.markFinished(seriesId);
            sendMessage(chatId, "Всё, сериал закончился.");
        }

        switch (data) {
            case "add" -> sendMessage(chatId, "Напиши: начать <название сериала>");
            case "list" -> {
                var inProgress = seriesService.getInProgressByChatId(chatId);
                if (inProgress.isEmpty()) {
                    sendMessage(chatId, "У вас пока нет сериалов в процессе");
                } else {
                    for (var s : inProgress) {
                        SendMessage msg = new SendMessage();
                        msg.setChatId(chatId);
                        msg.setText(s.getName() + " — Сезон " + s.getSeason() + ", Серия " + s.getEpisode());
                        msg.setReplyMarkup(buildSeriesKeyboard(s.getId()));
                        execute(msg);
                    }
                }
            }
            case "finished" -> {
                var finished = seriesService.getFinishedByChatId(chatId);
                if (finished.isEmpty()) {
                    sendMessage(chatId, "Законченных сериалов пока нет");
                } else {
                    StringBuilder sb = new StringBuilder("Просмотренные сериалы:\n");
                    for (var s : finished) {
                        sb.append(s.getName()).append("\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }
            default -> sendMessage(chatId, "Неизвестное действие");
        }
    }

    @Override
    public String getBotUsername() {
        return "";
    }
}
