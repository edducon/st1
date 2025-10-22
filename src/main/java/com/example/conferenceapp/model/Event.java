package com.example.conferenceapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Event {

    /* основные поля */
    private int id;
    private String title;
    private String direction;
    private LocalDateTime start;
    private LocalDateTime end;
    private String logoPath;

    /* новые поля */
    private String city;
    private String organizer;     // ФИО организатора
    private String description;

    private int directionId;
    private Integer cityId;
    private Integer organizerId;

    private final List<Activity> activities = new ArrayList<>();

    public Event(int id, String title, String direction,
                 LocalDateTime start, String logoPath,
                 String city, String organizer, String description) {
        this.id          = id;
        this.title       = title;
        this.direction   = direction;
        this.start       = start;
        this.logoPath    = logoPath;
        this.city        = city;
        this.organizer   = organizer;
        this.description = description;
    }

    public Event(int id, String title, int directionId, String direction,
                 LocalDateTime start, LocalDateTime end,
                 Integer cityId, String city,
                 Integer organizerId, String organizer,
                 String logoPath, String description) {
        this(id, title, direction, start, logoPath, city, organizer, description);
        this.directionId = directionId;
        this.end         = end;
        this.cityId      = cityId;
        this.organizerId = organizerId;
    }

    public Event withActivity(Activity activity) {
        this.activities.add(activity);
        return this;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public void setDirectionId(int directionId) {
        this.directionId = directionId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public void setOrganizerId(Integer organizerId) {
        this.organizerId = organizerId;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    /* getters */
    public int            getId()          { return id; }
    public String         getTitle()       { return title; }
    public String         getDirection()   { return direction; }
    public LocalDateTime  getStart()       { return start; }
    public LocalDateTime  getEnd()         { return end; }
    public String         getLogoPath()    { return logoPath; }
    public String         getCity()        { return city; }
    public String         getOrganizer()   { return organizer; }
    public String         getDescription() { return description; }
    public int            getDirectionId() { return directionId; }
    public Integer        getCityId()      { return cityId; }
    public Integer        getOrganizerId() { return organizerId; }
    public List<Activity> getActivities()  { return Collections.unmodifiableList(activities); }

    public LocalDate getStartDate() {
        return start != null ? start.toLocalDate() : null;
    }

    public LocalTime getStartTime() {
        return start != null ? start.toLocalTime() : null;
    }

    public LocalDate getEndDate() {
        return end != null ? end.toLocalDate() : null;
    }

    public LocalTime getEndTime() {
        return end != null ? end.toLocalTime() : null;
    }

    @Override
    public String toString() {
        return title;
    }
}
