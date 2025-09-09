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

    //добавить сериал
    public Series addSeries(String name) {
        Series series = new Series();
        series.setName(name);
        return repo.save(series);
    }

    //удалить сериал
    public void deleteSeries(Long id) {
        Optional<Series> optionalSeries = repo.findById(id);
        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            repo.delete(series);
        }
    }

    //отметить сезон
    public void checkSeason(Long id, int season) {
        Optional<Series> optionalSeries = repo.findById(id);
        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            series.setSeason(season);
            repo.save(series);
        }
    }
    //отметить серию
    public void checkEpisode(Long id, int episode) {
        Optional<Series> optionalSeries = repo.findById(id);
        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            series.setEpisode(episode);
            repo.save(series);
        }
    }

    //отметить сериал просмотренным
    public void markFinished(Long id) {
        Optional<Series> optionalSeries = repo.findById(id);
        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            series.setFinished(true);
            repo.save(series);
        }
    }

    //показать, как давно начали смотреть сериал
    public long watchlasting(Long id) {
        Optional<Series> optionalSeries = repo.findById(id);
        if (optionalSeries.isPresent()) {
            Series series = optionalSeries.get();
            return ChronoUnit.DAYS.between(series.getStart(), LocalDate.now());
        }
        return 0;
    }

    //показать список сериалов в процессе
    public List<Series> getInProgress() {
        return repo.findAll().stream()
                .filter(s -> !s.isFinished())
                .toList();
    }

    //показать список просмотренных сериалов
    public List<Series> getFinished() {
        return repo.findAll().stream()
                .filter(Series::isFinished)
                .toList();
    }

}
