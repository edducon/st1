package com.example.conferenceapp.model;

public class ResourceItem {

    private final int id;
    private final int activityId;
    private final String name;
    private final String url;
    private final String uploadedBy;
    private final java.time.LocalDateTime uploadedAt;

    public ResourceItem(int id, int activityId, String name, String url,
                        String uploadedBy, java.time.LocalDateTime uploadedAt) {
        this.id         = id;
        this.activityId = activityId;
        this.name       = name;
        this.url        = url;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public int getId() {
        return id;
    }

    public int getActivityId() {
        return activityId;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getUploadedBy() { return uploadedBy; }
    public java.time.LocalDateTime getUploadedAt() { return uploadedAt; }
}
