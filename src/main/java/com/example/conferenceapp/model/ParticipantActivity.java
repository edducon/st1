package com.example.conferenceapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParticipantActivity {

    private final int activityId;
    private final int eventId;
    private final String activityTitle;
    private final String eventTitle;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final List<String> participants = new ArrayList<>();
    private final List<ResourceItem> resources = new ArrayList<>();

    public ParticipantActivity(int activityId, int eventId, String activityTitle,
                               String eventTitle, LocalDateTime start, LocalDateTime end) {
        this.activityId = activityId;
        this.eventId = eventId;
        this.activityTitle = activityTitle;
        this.eventTitle = eventTitle;
        this.start = start;
        this.end = end;
    }

    public int getActivityId() { return activityId; }
    public int getEventId() { return eventId; }
    public String getActivityTitle() { return activityTitle; }
    public String getEventTitle() { return eventTitle; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public List<String> getParticipants() { return Collections.unmodifiableList(participants); }
    public List<ResourceItem> getResources() { return Collections.unmodifiableList(resources); }

    public ParticipantActivity withParticipant(String participant) {
        if (participant != null) {
            participants.add(participant);
        }
        return this;
    }

    public ParticipantActivity withResource(ResourceItem resource) {
        if (resource != null) {
            resources.add(resource);
        }
        return this;
    }
}
