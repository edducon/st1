package com.example.conferenceapp.model;

public class ActivityTask {

    private final int id;
    private final int activityId;
    private final String title;
    private final String author;

    public ActivityTask(int id, int activityId, String title, String author) {
        this.id         = id;
        this.activityId = activityId;
        this.title      = title;
        this.author     = author;
    }

    public int getId() {
        return id;
    }

    public int getActivityId() {
        return activityId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
