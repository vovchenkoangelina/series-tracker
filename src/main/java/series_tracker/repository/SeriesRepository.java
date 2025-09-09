package series_tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import series_tracker.model.Series;

public interface SeriesRepository extends JpaRepository<Series, Long> {
}
