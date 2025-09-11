package series_tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import series_tracker.model.Series;

import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Series findByName(String name);
    Optional<Series> findByNameAndChatId(String name, long chatId);
}
