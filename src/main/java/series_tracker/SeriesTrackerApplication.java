package series_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import series_tracker.bot.SeriesBot;

@SpringBootApplication
public class SeriesTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeriesTrackerApplication.class, args);
	}

	@Bean
	public TelegramBotsApi telegramBotsApi(SeriesBot seriesBot) throws TelegramApiException {
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
		botsApi.registerBot(seriesBot);
		return botsApi;
	}

}
