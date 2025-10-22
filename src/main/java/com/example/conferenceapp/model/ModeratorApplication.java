package com.example.conferenceapp.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class ModeratorApplication {

    public enum Status { SENT, APPROVED, REJECTED }

    private final int id;
    private final int activityId;
    private final int moderatorId;
    private final String activityTitle;
    private final String eventTitle;
    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;
    private Status status;
    private String comment;

    public ModeratorApplication(int id, int activityId, int moderatorId,
                                String activityTitle, String eventTitle,
                                LocalDate date, LocalTime start, LocalTime end,
                                Status status, String comment) {
        this.id             = id;
        this.activityId     = activityId;
        this.moderatorId    = moderatorId;
        this.activityTitle  = activityTitle;
        this.eventTitle     = eventTitle;
        this.date           = date;
        this.start          = start;
        this.end            = end;
        this.status         = status;
        this.comment        = comment;
    }

    public int getId() {
        return id;
    }

    public int getActivityId() {
        return activityId;
    }

    public int getModeratorId() {
        return moderatorId;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
