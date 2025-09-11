package series_tracker.bot;

import jakarta.annotation.PostConstruct;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import series_tracker.service.SeriesService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SeriesBot extends TelegramLongPollingBot {

    private final SeriesService seriesService;

    private final Map<Long, Boolean> waitingForSeriesName = new ConcurrentHashMap<>();

    public SeriesBot(SeriesService seriesService) {
        this.seriesService = seriesService;
    }


    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
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

        if (waitingForSeriesName.getOrDefault(chatId, false)) {
            seriesService.addSeries(text, chatId);
            sendMessage(chatId, "Сериал \"" + text + "\" добавлен. Приятного просмотра!");
            waitingForSeriesName.remove(chatId);
            return;
        }

        if (text.toLowerCase().startsWith("начать")) {
            String name = text.substring(7);
            seriesService.addSeries(name, chatId);
            sendMessage(chatId, "Принято. Приятного просмотра!");
            return;
        }

        if (text.contains("сезон")) {
            String name = text.substring(0, text.indexOf("сезон") - 1);
            int season = Integer.parseInt(text.substring(text.lastIndexOf(" ") + 1));
            seriesService.checkSeason(seriesService.findByName(name).getId(), season);
            sendMessage(chatId, "Принято!");
            return;
        }

        if (text.contains("серия")) {
            String name = text.substring(0, text.indexOf("серия") - 1);
            int episode = Integer.parseInt(text.substring(text.lastIndexOf(" ") + 1));
            seriesService.checkEpisode(seriesService.findByName(name).getId(), episode);
            sendMessage(chatId, "Принято!");
            return;
        }

        switch (text) {
            case "/start", "старт" -> {
                sendMessage(chatId, "Привет! Этот бот запоминает, на какой серии сериала вы остановились. Используйте кнопки или напишите /commands, чтобы узнать, как добавлять новые сериалы и отмечать серии без кнопок.");
                sendMenu(chatId);
            }
            case "/menu", "меню" -> sendMenu(chatId);
            case "/list", "список" -> sendSeriesList(chatId);
            case "/commands" -> sendCommands(chatId);
            case "/finished" -> {
                var finished = seriesService.getFinishedByChatId(chatId);
                if (finished.isEmpty()) {
                    sendMessage(chatId, "Законченных сериалов пока нет");
                } else {
                    StringBuilder sb = new StringBuilder("Просмотренные сериалы:\n");
                    for (var s : finished) {
                        long days = seriesService.watchlasting(s.getId());
                        sb.append(s.getName())
                                .append(" (").append("сезонов: ").append(s.getSeason())
                                .append(", заняло дней: ").append(days)
                                .append(")\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }
        }
    }

    private void sendCommands(long chatId) throws TelegramApiException {
        String text = "Чтобы добавить новый сериал, напишите слово \"начать\", пробел и название сериала.\nНапример \"Начать Декстер\"." +
                "\n\nЧтобы отметить серию, напишите название сериала пробел, слово \"серия\" и номер сезона через пробел.\nНапример \"Декстер серия 17\"." +
                "\n\nЧтобы сменить сезон, напишите название сериала, пробел, слово \"сезон\" и номер сезона через пробел.\nНапример \"Декстер сезон 4\"." +
                "\n\nСлово \"меню\" вызывает меню с кнопками, слово \"список\" присылает список сериалов, которые вы сейчас смотрите.";
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
        InlineKeyboardButton finishedBtn = new InlineKeyboardButton("Законченные");
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

        InlineKeyboardButton deleteBtn = new InlineKeyboardButton("Удалить");
        deleteBtn.setCallbackData("delete:" + seriesId);

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(episodeBtn, seasonBtn, finishBtn, deleteBtn)
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }


    private void sendSeriesList(long chatId) throws TelegramApiException {
        var inProgress = seriesService.getInProgressByChatId(chatId);
        if (inProgress.isEmpty()) {
            sendMessage(chatId, "У вас пока нет сериалов в процессе");
        } else {
            for (var s : inProgress) {
                long days = seriesService.watchlasting(s.getId());
                SendMessage msg = new SendMessage();
                msg.setChatId(chatId);
                msg.setText(
                        s.getName() + " — Сезон " + s.getSeason() + ", Серия " + s.getEpisode() +
                                "\nДней: " + days
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

        if (data.startsWith("delete:")) {
            Long seriesId = Long.parseLong(data.split(":")[1]);
            seriesService.deleteSeries(seriesId);
            sendMessage(chatId, "Сериал удалён.");
        }

        switch (data) {
            case "add" -> {
                sendMessage(chatId, "Введите название сериала:");
                waitingForSeriesName.put(chatId, true);
            }
            case "list" -> {
                var inProgress = seriesService.getInProgressByChatId(chatId);
                if (inProgress.isEmpty()) {
                    sendMessage(chatId, "У вас пока нет сериалов в процессе");
                } else {
                    for (var s : inProgress) {
                        SendMessage msg = new SendMessage();
                        msg.setChatId(chatId);
                        long days = seriesService.watchlasting(s.getId());
                        msg.setText(s.getName() + " — Сезон " + s.getSeason() + ", Серия " + s.getEpisode() +
                                "\nДней: " + days);
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
                        long days = seriesService.watchlasting(s.getId());
                        sb.append(s.getName())
                                .append(" (").append("сезонов: ").append(s.getSeason())
                                .append(", заняло дней: ").append(days)
                                .append(")\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }
        }
    }

    @PostConstruct
    public void initCommands() {
        List<BotCommand> commands = List.of(
                new BotCommand("/start", "Начало работы с ботом"),
                new BotCommand("/menu", "Открыть меню"),
                new BotCommand("/commands", "Показать список команд")
        );

        try {
            execute(new SetMyCommands(commands, null, null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
