package com.example.conferenceapp.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class ModeratorSlot {

    public enum Status { AVAILABLE, SENT, APPROVED }

    private final int activityId;
    private final String activityTitle;
    private final String eventTitle;
    private final String direction;
    private final LocalDate date;
    private final LocalTime start;
    private final LocalTime end;
    private Status status;

    public ModeratorSlot(int activityId, String activityTitle, String eventTitle,
                         String direction, LocalDate date, LocalTime start, LocalTime end,
                         Status status) {
        this.activityId = activityId;
        this.activityTitle = activityTitle;
        this.eventTitle = eventTitle;
        this.direction = direction;
        this.date = date;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public int getActivityId() { return activityId; }
    public String getActivityTitle() { return activityTitle; }
    public String getEventTitle() { return eventTitle; }
    public String getDirection() { return direction; }
    public LocalDate getDate() { return date; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
