package com.example.conferenceapp.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Activity {

    private int id;
    private int eventId;
    private String title;
    private int dayNum;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer moderatorId;
    private String moderatorName;

    private final List<String> jury = new ArrayList<>();
    private final List<ActivityTask> tasks = new ArrayList<>();
    private LocalDate date;   // удобство для Kanban

    public Activity(int id, int eventId, String title, int dayNum,
                    LocalTime startTime, LocalTime endTime,
                    Integer moderatorId, String moderatorName) {
        this.id            = id;
        this.eventId       = eventId;
        this.title         = title;
        this.dayNum        = dayNum;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.moderatorId   = moderatorId;
        this.moderatorName = moderatorName;
    }

    public Activity(int eventId, String title, int dayNum,
                    LocalTime startTime, LocalTime endTime) {
        this(0, eventId, title, dayNum, startTime, endTime, null, null);
    }

    public Activity withJury(String fullName) {
        this.jury.add(fullName);
        return this;
    }

    public Activity withTask(ActivityTask task) {
        this.tasks.add(task);
        return this;
    }

    public int getId() {
        return id;
    }

    public int getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public int getDayNum() {
        return dayNum;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Integer getModeratorId() {
        return moderatorId;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public List<String> getJury() {
        return Collections.unmodifiableList(jury);
    }

    public List<ActivityTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
