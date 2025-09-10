package series_tracker.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;

    private String name;
    private int season;
    private int episode;
    private boolean isFinished;
    private LocalDate start;
    private LocalDate lastWatched;

    public Series() {
        this.season = 1;
        this.episode = 1;
        this.start = LocalDate.now();
        this.isFinished = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getLastWatched() {
        return lastWatched;
    }

    public void setLastWatched(LocalDate lastWatched) {
        this.lastWatched = lastWatched;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
