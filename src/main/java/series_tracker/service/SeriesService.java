package series_tracker.service;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import series_tracker.model.Series;
import series_tracker.repository.SeriesRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class SeriesService {

        private final SeriesRepository repo;

        public SeriesService(SeriesRepository repo) {
            this.repo = repo;
        }

        // добавить сериал
        public Series addSeries(String name, Long chatId) {
            Series series = new Series();
            series.setName(name);
            series.setChatId(chatId);
            return repo.save(series);
        }

        // удалить сериал по id с проверкой chatId
        public void deleteSeries(Long id, Long chatId) {
            Series series = repo.findById(id)
                    .filter(s -> s.getChatId().equals(chatId))
                    .orElseThrow(() -> new IllegalArgumentException("Сериал не найден или доступ запрещён"));
            repo.delete(series);
        }

        // отметить сезон
        public void checkSeason(Long id, int season, Long chatId) {
            Series series = repo.findById(id)
                    .filter(s -> s.getChatId().equals(chatId))
                    .orElseThrow(() -> new IllegalArgumentException("Сериал не найден или доступ запрещён"));
            series.setSeason(season);
            series.setEpisode(1);
            series.setLastWatched(LocalDate.now());
            repo.save(series);
        }

        // отметить серию
        public void checkEpisode(Long id, int episode, Long chatId) {
            Series series = repo.findById(id)
                    .filter(s -> s.getChatId().equals(chatId))
                    .orElseThrow(() -> new IllegalArgumentException("Сериал не найден или доступ запрещён"));
            series.setEpisode(episode);
            series.setLastWatched(LocalDate.now());
            repo.save(series);
        }

        // отметить сериал просмотренным
        public void markFinished(Long id, Long chatId) {
            Series series = repo.findById(id)
                    .filter(s -> s.getChatId().equals(chatId))
                    .orElseThrow(() -> new IllegalArgumentException("Сериал не найден или доступ запрещён"));
            series.setFinished(true);
            series.setLastWatched(LocalDate.now());
            repo.save(series);
        }

        // показать, как давно начали смотреть сериал
        public long watchlasting(Long id, Long chatId) {
            Series series = repo.findById(id)
                    .filter(s -> s.getChatId().equals(chatId))
                    .orElseThrow(() -> new IllegalArgumentException("Сериал не найден или доступ запрещён"));
            return ChronoUnit.DAYS.between(series.getStart(), LocalDate.now()) + 1;
        }

        // показать список сериалов в процессе
        public List<Series> getInProgressByChatId(Long chatId) {
            return repo.findAll().stream()
                    .filter(s -> !s.isFinished() && chatId.equals(s.getChatId()))
                    .toList();
        }

        // показать список просмотренных сериалов
        public List<Series> getFinishedByChatId(Long chatId) {
            return repo.findAll().stream()
                    .filter(s -> s.isFinished() && chatId.equals(s.getChatId()))
                    .toList();
        }

        // найти сериал по имени + chatId
        public Optional<Series> findByNameOptional(String name, long chatId) {
            return repo.findByNameAndChatId(name, chatId);
        }

        public Series findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Сериал с id " + id + " не найден"));
        }

}
