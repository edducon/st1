package com.example.conferenceapp.model;

public class PersonCard {

    private final int id;
    private final String idNumber;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String role;
    private final String direction;
    private final String eventTitle;
    private final String photoPath;

    public PersonCard(int id, String idNumber, String fullName,
                      String email, String phone, String role,
                      String direction, String eventTitle, String photoPath) {
        this.id         = id;
        this.idNumber   = idNumber;
        this.fullName   = fullName;
        this.email      = email;
        this.phone      = phone;
        this.role       = role;
        this.direction  = direction;
        this.eventTitle = eventTitle;
        this.photoPath  = photoPath;
    }

    public int getId() {
        return id;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getDirection() {
        return direction;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}
