package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.User;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Контроллер окна участника / экрана «Мои активности».
 * В демо-версии использует статические данные и даёт возможность
 * управлять ресурсами выбранной активности.
 */
public class ParticipantController implements UserAware {

    /* ─ UI ─────────────────────────────────────────────────────────── */
    @FXML private Label greetingLabel;
    @FXML private Label activityInfoLabel;
    @FXML private ListView<ActivityCard> activityList;
    @FXML private ListView<String> participantList;
    @FXML private TableView<ResourceEntry> resourceTable;
    @FXML private TableColumn<ResourceEntry, String> resourceNameCol;
    @FXML private TableColumn<ResourceEntry, String> resourceOwnerCol;
    @FXML private TableColumn<ResourceEntry, String> resourceDateCol;
    @FXML private TableColumn<ResourceEntry, ResourceEntry> resourceActionsCol;
    @FXML private Button addResourceBtn;
    @FXML private Button kanbanBtn;
    @FXML private Label statusLabel;

    /* ─ data ───────────────────────────────────────────────────────── */
    private final ObservableList<ActivityCard> activities = FXCollections.observableArrayList();
    private User user;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /* ─ init ───────────────────────────────────────────────────────── */
    public void initialize() {
        prepareTable();
        prepareActivityList();
        loadSampleData();

        addResourceBtn.setOnAction(e -> onAddResource());
        kanbanBtn.setOnAction(e -> showInfo("Функция Kanban появится в полной версии."));
    }

    private void prepareActivityList() {
        activityList.setItems(activities);
        activityList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ActivityCard item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        activityList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                showActivity(newV));
    }

    private void prepareTable() {
        resourceNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        resourceOwnerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));
        resourceDateCol.setCellValueFactory(cell ->
                Bindings.createStringBinding(() -> {
                    LocalDate date = cell.getValue().getDate();
                    return date == null ? "" : DATE_FMT.format(date);
                }, cell.getValue().dateProperty()));

        resourceActionsCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        resourceActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button downloadBtn = new Button("Скачать");
            private final Button deleteBtn   = new Button("Удалить");
            {
                downloadBtn.setOnAction(e -> onDownload(getTableRow().getItem()));
                deleteBtn  .setOnAction(e -> onDelete  (getTableRow().getItem()));
                downloadBtn.getStyleClass().add("button-primary");
            }

            @Override
            protected void updateItem(ResourceEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(new ToolBar(downloadBtn, deleteBtn));
                }
                setText(null);
            }
        });
    }

    private void loadSampleData() {
        ActivityCard hackathon = new ActivityCard(
                "Хакатон WorldSkills",
                "Кибербезопасность",
                LocalDateTime.of(2025, 3, 14, 9, 0),
                LocalDateTime.of(2025, 3, 14, 11, 30));
        hackathon.getParticipants().addAll("Иванов Иван", "Петров Пётр", "Сидорова Анна");
        hackathon.getResources().addAll(
                new ResourceEntry("Брифинг.pdf", "Организатор", LocalDate.of(2025, 2, 28)),
                new ResourceEntry("Презентация.pptx", "Иванов Иван", LocalDate.of(2025, 3, 1))
        );

        ActivityCard ctf = new ActivityCard(
                "CTF-квест",
                "Инфозащита",
                LocalDateTime.of(2025, 4, 5, 12, 0),
                LocalDateTime.of(2025, 4, 5, 14, 0));
        ctf.getParticipants().addAll("Команда «CyberFox»", "Команда «Root»");
        ctf.getResources().add(new ResourceEntry("Правила соревнований.docx", "Модератор", LocalDate.of(2025, 3, 20)));

        ActivityCard design = new ActivityCard(
                "UI-дизайн безопасности",
                "Дизайн",
                LocalDateTime.of(2025, 5, 2, 10, 0),
                LocalDateTime.of(2025, 5, 2, 12, 30));
        design.getParticipants().addAll("Команда «SkyNet»", "Алексеева Мария");

        activities.setAll(hackathon, ctf, design);
        if (!activities.isEmpty()) {
            activityList.getSelectionModel().selectFirst();
        }
    }

    /* ─ actions ────────────────────────────────────────────────────── */
    private void onAddResource() {
        ActivityCard current = activityList.getSelectionModel().getSelectedItem();
        if (current == null) {
            showError("Выберите активность");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новый ресурс");
        dialog.setHeaderText("Добавить файл для участников");
        dialog.setContentText("Название файла:");
        dialog.initOwner(getWindow());

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String name = result.get().trim();
        if (name.isEmpty()) {
            showError("Название файла не может быть пустым");
            return;
        }

        String author = user != null ? user.getFullName() : "Участник";
        ResourceEntry entry = new ResourceEntry(name, author, LocalDate.now());
        current.getResources().add(entry);
        resourceTable.getSelectionModel().select(entry);
        showInfo("Ресурс «" + name + "» добавлен");
    }

    private void onDownload(ResourceEntry entry) {
        if (entry == null) {
            return;
        }
        showInfo("Файл «" + entry.getName() + "» отправлен на загрузку.");
    }

    private void onDelete(ResourceEntry entry) {
        if (entry == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление ресурса");
        alert.setHeaderText("Удалить «" + entry.getName() + "»?");
        alert.setContentText("Ресурс будет недоступен другим участникам.");
        alert.initOwner(getWindow());

        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            ActivityCard current = activityList.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.getResources().remove(entry);
                showInfo("Ресурс удалён");
            }
        }
    }

    /* ─ helpers ───────────────────────────────────────────────────── */
    private void showActivity(ActivityCard activity) {
        if (activity == null) {
            activityInfoLabel.setText("Нет выбранной активности");
            participantList.setItems(FXCollections.observableArrayList());
            resourceTable.setItems(FXCollections.observableArrayList());
            return;
        }

        String info = "%s (%s) — %s %s–%s".formatted(
                activity.getTitle(),
                activity.getEventName(),
                DATE_FMT.format(activity.getStart().toLocalDate()),
                TIME_FMT.format(activity.getStart().toLocalTime()),
                TIME_FMT.format(activity.getEnd().toLocalTime())
        );
        activityInfoLabel.setText(info);

        participantList.setItems(activity.getParticipants());
        resourceTable.setItems(activity.getResources());
    }

    private void showInfo(String message) {
        setStatus(message, "-fx-text-fill: #008000;");
    }

    private void showError(String message) {
        setStatus(message, "-fx-text-fill: #CC0000;");
    }

    private void setStatus(String message, String style) {
        statusLabel.setStyle(style);
        statusLabel.setText(message);
    }

    private Window getWindow() {
        return greetingLabel != null ? greetingLabel.getScene().getWindow() : null;
    }

    /* ─ UserAware ─────────────────────────────────────────────────── */
    @Override
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            greetingLabel.setText(buildGreeting(user));
        } else {
            greetingLabel.setText("Добро пожаловать!");
        }
    }

    private static String buildGreeting(User user) {
        LocalTime now = LocalTime.now();
        String part;
        if (now.isBefore(LocalTime.of(11, 1))) {
            part = "Доброе утро";
        } else if (now.isBefore(LocalTime.of(18, 1))) {
            part = "Добрый день";
        } else {
            part = "Добрый вечер";
        }
        return "%s, %s %s!".formatted(part, user.getFirstName(), user.getMiddleName());
    }

    /* ─ model classes ─────────────────────────────────────────────── */
    public static class ActivityCard {
        private final String title;
        private final String eventName;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final ObservableList<String> participants = FXCollections.observableArrayList();
        private final ObservableList<ResourceEntry> resources = FXCollections.observableArrayList();

        public ActivityCard(String title, String eventName,
                            LocalDateTime start, LocalDateTime end) {
            this.title = title;
            this.eventName = eventName;
            this.start = Objects.requireNonNull(start);
            this.end = Objects.requireNonNull(end);
        }

        public String getTitle() { return title; }
        public String getEventName() { return eventName; }
        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        public ObservableList<String> getParticipants() { return participants; }
        public ObservableList<ResourceEntry> getResources() { return resources; }

        public String getDisplayName() {
            return "%s, %s–%s".formatted(
                    title,
                    TIME_FMT.format(start.toLocalTime()),
                    TIME_FMT.format(end.toLocalTime())
            );
        }
    }

    public static class ResourceEntry {
        private final StringProperty name  = new SimpleStringProperty();
        private final StringProperty owner = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();

        public ResourceEntry(String name, String owner, LocalDate date) {
            this.name.set(name);
            this.owner.set(owner);
            this.date.set(date);
        }

        public String getName() { return name.get(); }
        public StringProperty nameProperty() { return name; }
        public String getOwner() { return owner.get(); }
        public StringProperty ownerProperty() { return owner; }
        public LocalDate getDate() { return date.get(); }
        public ObjectProperty<LocalDate> dateProperty() { return date; }
    }
}
