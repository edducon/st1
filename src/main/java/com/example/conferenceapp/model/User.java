package com.example.conferenceapp.model;

/** Доменная модель пользователя (таблица {@code user}). */
public class User {

    /* ------------ поля, которые реально есть в БД ------------- */
    private int    id;
    private String idNumber;          // уникальный ID Number
    private String fullName;          // Фамилия Имя Отчество
    private Role   role;              // enum
    private String photoPath;         // путь к фотографии или null
    private String email;
    private String phone;
    private String direction;         // название направления (join-ом)
    private String country;           // название страны      (join-ом)
    private String passwordHash;      // SHA-256 (может быть null при update)

    /* ------------ роли ------------- */
    public enum Role { PARTICIPANT, MODERATOR, ORGANIZER, JURY }

    /* ------------ конструктор для DAO ------------- */
    public User(int id, String idNumber, String fullName,
                Role role, String photoPath,
                String email, String phone) {
        this.id        = id;
        this.idNumber  = idNumber;
        this.fullName  = fullName;
        this.role      = role;
        this.photoPath = photoPath;
        this.email     = email;
        this.phone     = phone;
    }

    /* ------------ геттеры ------------- */
    public int    getId()        { return id; }
    public String getIdNumber()  { return idNumber; }
    public String getFullName()  { return fullName; }
    public Role   getRole()      { return role; }
    public String getPhotoPath() { return photoPath; }
    public String getEmail()     { return email; }
    public String getPhone()     { return phone; }
    public String getDirection() { return direction; }
    public String getCountry()   { return country; }
    public String getPasswordHash() { return passwordHash; }

    /* ------------ вспомогательные методы разбора ФИО ------------- */
    /** Возвращает первое слово (фамилию). */
    public String getSecondName() {
        return fullName.split("\\s+")[0];
    }

    /** Возвращает второе слово (имя) либо пустую строку. */
    public String getFirstName() {
        String[] p = fullName.split("\\s+");
        return p.length > 1 ? p[1] : "";
    }

    /** Возвращает третье слово (отчество) либо пустую строку. */
    public String getMiddleName() {
        String[] p = fullName.split("\\s+");
        return p.length > 2 ? p[2] : "";
    }

    /* ------------ сеттеры (редактируемые поля) ------------- */
    public void setFullName    (String fullName)  { this.fullName   = fullName; }
    public void setEmail       (String email)     { this.email      = email; }
    public void setPhone       (String phone)     { this.phone      = phone; }
    public void setDirection   (String direction) { this.direction  = direction; }
    public void setCountry     (String country)   { this.country    = country; }
    public void setPasswordHash(String hash)      { this.passwordHash = hash; }
}
