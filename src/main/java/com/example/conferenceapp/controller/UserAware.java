package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.User;

/**
 * Интерфейс для контроллеров, которым необходима информация
 * об авторизованном пользователе.
 */
public interface UserAware {
    /** Передаёт контроллеру авторизованного пользователя. */
    void setUser(User user);
}
