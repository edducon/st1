-- ==============================
-- 1. Справочники / reference data
-- ==============================

CREATE TABLE country (
                         id          INT PRIMARY KEY AUTO_INCREMENT,
                         name_ru     VARCHAR(128) NOT NULL,
                         name_en     VARCHAR(128),
                         iso_alpha2  CHAR(2),
                         iso_num     INT
);

CREATE TABLE city (
                      id          INT PRIMARY KEY AUTO_INCREMENT,
                      country_id  INT NOT NULL,
                      name        VARCHAR(128) NOT NULL,
                      FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE role (
                      id   INT PRIMARY KEY AUTO_INCREMENT,
                      code VARCHAR(32) UNIQUE NOT NULL,  -- participant / moderator / organizer / jury
                      label_ru VARCHAR(64) NOT NULL
);

CREATE TABLE direction (
                           id   INT PRIMARY KEY AUTO_INCREMENT,
                           name VARCHAR(128) UNIQUE NOT NULL
);

-- =============
-- 2. Пользователи
-- =============

CREATE TABLE user (
                      id            INT PRIMARY KEY AUTO_INCREMENT,
                      id_number     VARCHAR(32) UNIQUE NOT NULL,
                      role_id       INT NOT NULL,
                      full_name     VARCHAR(255),
                      password_hash VARCHAR(255),
                      email         VARCHAR(255),
                      phone         VARCHAR(64),
                      photo         VARCHAR(128),
                      direction_id  INT,
                      country_id    INT,
                      birth_date    DATE,
                      gender        ENUM('male','female'),
                      remembered    TINYINT(1) DEFAULT 0,
                      FOREIGN KEY (role_id)      REFERENCES role(id),
                      FOREIGN KEY (direction_id) REFERENCES direction(id),
                      FOREIGN KEY (country_id)   REFERENCES country(id)
);

-- =========
-- 3. События
-- =========

CREATE TABLE event (
                       id              INT PRIMARY KEY AUTO_INCREMENT,
                       title           VARCHAR(512) NOT NULL,
                       direction_id    INT NOT NULL,
                       start_datetime  DATETIME NOT NULL,
                       end_datetime    DATETIME NOT NULL,
                       city_id         INT,
                       organizer_id    INT,
                       logo            VARCHAR(128),
                       description     TEXT,
                       FOREIGN KEY (direction_id) REFERENCES direction(id),
                       FOREIGN KEY (city_id)      REFERENCES city(id),
                       FOREIGN KEY (organizer_id) REFERENCES user(id)
);

CREATE TABLE activity (
                          id          INT PRIMARY KEY AUTO_INCREMENT,
                          event_id    INT NOT NULL,
                          title       VARCHAR(512) NOT NULL,
                          day_num     INT,
                          start_time  TIME,
                          end_time    TIME,
                          moderator_id INT,
                          FOREIGN KEY (event_id)     REFERENCES event(id),
                          FOREIGN KEY (moderator_id) REFERENCES user(id)
);

CREATE TABLE activity_jury (
                               activity_id INT,
                               jury_id     INT,
                               PRIMARY KEY (activity_id, jury_id),
                               FOREIGN KEY (activity_id) REFERENCES activity(id),
                               FOREIGN KEY (jury_id)     REFERENCES user(id)
);

CREATE TABLE activity_task (
                               id           INT PRIMARY KEY AUTO_INCREMENT,
                               activity_id  INT NOT NULL,
                               title        VARCHAR(512) NOT NULL,
                               created_by   INT,
                               FOREIGN KEY (activity_id) REFERENCES activity(id),
                               FOREIGN KEY (created_by)  REFERENCES user(id)
);

CREATE TABLE resource (
                           id           INT PRIMARY KEY AUTO_INCREMENT,
                           activity_id  INT NOT NULL,
                           name         VARCHAR(255) NOT NULL,
                           url          VARCHAR(512),
                           uploaded_by  INT,
                           uploaded_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (activity_id) REFERENCES activity(id),
                           FOREIGN KEY (uploaded_by) REFERENCES user(id)
);

CREATE TABLE moderator_application (
                                       id            INT PRIMARY KEY AUTO_INCREMENT,
                                       activity_id   INT NOT NULL,
                                       moderator_id  INT NOT NULL,
                                       status        ENUM('SENT','APPROVED','REJECTED') DEFAULT 'SENT',
                                       comment       VARCHAR(512),
                                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (activity_id)  REFERENCES activity(id),
                                       FOREIGN KEY (moderator_id) REFERENCES user(id)
);

CREATE TABLE moderator_assignment (
                                      user_id  INT NOT NULL,
                                      event_id INT NOT NULL,
                                      PRIMARY KEY (user_id, event_id),
                                      FOREIGN KEY (user_id) REFERENCES user(id),
                                      FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE TABLE participant_event (
                                   participant_id INT NOT NULL,
                                   event_id       INT NOT NULL,
                                   PRIMARY KEY (participant_id, event_id),
                                   FOREIGN KEY (participant_id) REFERENCES user(id),
                                   FOREIGN KEY (event_id)       REFERENCES event(id)
);

-- =========================================
-- 4. Начальные данные (роли + примеры)
-- =========================================
INSERT INTO role (code,label_ru) VALUES
                                     ('participant','Участник'),
                                     ('moderator'  ,'Модератор'),
                                     ('organizer'  ,'Организатор'),
                                     ('jury'       ,'Жюри');

-- Направления можно наполнить при импорте CSV
