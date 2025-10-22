package com.example.conferenceapp.model;

import java.time.LocalDateTime;

public class Event {

    /* основные поля */
    private int id;
    private String title;
    private String direction;
    private LocalDateTime start;
    private String logoPath;

    /* новые поля */
    private String city;
    private String organizer;     // ФИО организатора
    private String description;

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

    /* getters */
    public int            getId()          { return id; }
    public String         getTitle()       { return title; }
    public String         getDirection()   { return direction; }
    public LocalDateTime  getStart()       { return start; }
    public String         getLogoPath()    { return logoPath; }
    public String         getCity()        { return city; }
    public String         getOrganizer()   { return organizer; }
    public String         getDescription() { return description; }
}
