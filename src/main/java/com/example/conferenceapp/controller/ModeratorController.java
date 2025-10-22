package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.User;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер окна модератора. Использует демо-данные и
 * реализует фильтрацию, подачу заявок и просмотр своих активностей.
 */
public class ModeratorController implements UserAware {

    /* ─ UI ─────────────────────────────────────────────────────────── */
    @FXML private Label greetingLabel;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<String> eventBox;
    @FXML private TableView<ActivityEntry> activityTable;
    @FXML private TableColumn<ActivityEntry, String> activityCol;
    @FXML private TableColumn<ActivityEntry, String> eventCol;
    @FXML private TableColumn<ActivityEntry, String> dateCol;
    @FXML private TableColumn<ActivityEntry, String> timeCol;
    @FXML private TableColumn<ActivityEntry, String> statusCol;
    @FXML private Button applyBtn;
    @FXML private Button myActivitiesBtn;
    @FXML private Label infoLabel;

    /* ─ data ───────────────────────────────────────────────────────── */
    private final ObservableList<ActivityEntry> master = FXCollections.observableArrayList();
    private final FilteredList<ActivityEntry> filtered = new FilteredList<>(master, e -> true);
    private User user;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /* ─ init ───────────────────────────────────────────────────────── */
    public void initialize() {
        configureTable();
        loadSampleData();
        setupFilters();

        activityTable.setItems(filtered);
        applyBtn.disableProperty().bind(activityTable.getSelectionModel().selectedItemProperty().isNull());

        applyBtn.setOnAction(e -> onApply());
        myActivitiesBtn.setOnAction(e -> showMyActivities());

        applyFilters();
    }

    private void configureTable() {
        activityCol.setCellValueFactory(cell -> cell.getValue().activityProperty());
        eventCol   .setCellValueFactory(cell -> cell.getValue().eventProperty());
        dateCol    .setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DATE_FMT.format(cell.getValue().getDate())));
        timeCol    .setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                "%s–%s".formatted(
                        TIME_FMT.format(cell.getValue().getStart()),
                        TIME_FMT.format(cell.getValue().getEnd()))));
        statusCol  .setCellValueFactory(cell -> cell.getValue().statusTextProperty());

        activityTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadSampleData() {
        master.setAll(List.of(
                new ActivityEntry("UX-аудит", "Digital Security Day", "Дизайн",
                        LocalDateTime.of(2025, 3, 12, 9, 0),
                        LocalDateTime.of(2025, 3, 12, 10, 30)),
                new ActivityEntry("CTF Junior", "Security Weekend", "Кибербезопасность",
                        LocalDateTime.of(2025, 3, 12, 10, 30),
                        LocalDateTime.of(2025, 3, 12, 12, 0)),
                new ActivityEntry("Доклад «Zero Trust»", "Security Weekend", "Кибербезопасность",
                        LocalDateTime.of(2025, 3, 12, 13, 0),
                        LocalDateTime.of(2025, 3, 12, 14, 30)),
                new ActivityEntry("Разбор кейса", "Digital Security Day", "Менеджмент",
                        LocalDateTime.of(2025, 3, 13, 9, 0),
                        LocalDateTime.of(2025, 3, 13, 10, 30)),
                new ActivityEntry("Панельная дискуссия", "CyberLeaders", "Менеджмент",
                        LocalDateTime.of(2025, 3, 13, 12, 0),
                        LocalDateTime.of(2025, 3, 13, 13, 30))
        ));
    }

    private void setupFilters() {
        directionBox.getItems().setAll(extractDistinct(master.stream()
                .map(ActivityEntry::getDirection).collect(Collectors.toList())));
        eventBox.getItems().setAll(extractDistinct(master.stream()
                .map(ActivityEntry::getEvent).collect(Collectors.toList())));

        directionBox.setOnAction(e -> applyFilters());
        eventBox.setOnAction(e -> applyFilters());
    }

    private List<String> extractDistinct(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void applyFilters() {
        String direction = directionBox.getValue();
        String event = eventBox.getValue();

        filtered.setPredicate(entry ->
                (direction == null || direction.equals(entry.getDirection())) &&
                        (event == null || event.equals(entry.getEvent()))
        );

        infoLabel.setText("Найдено активностей: " + filtered.size());
    }

    /* ─ actions ────────────────────────────────────────────────────── */
    private void onApply() {
        ActivityEntry selected = activityTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        if (selected.getStatus() == ActivityEntry.Status.APPROVED) {
            showError("Вы уже одобрены на эту активность");
            return;
        }
        if (selected.getStatus() == ActivityEntry.Status.REQUESTED) {
            showInfo("Заявка уже отправлена и ожидает подтверждения");
            return;
        }

        Optional<ActivityEntry> conflict = master.stream()
                .filter(e -> e != selected)
                .filter(ActivityEntry::isReserved)
                .filter(e -> e.overlaps(selected))
                .findFirst();

        if (conflict.isPresent()) {
            ActivityEntry conflicting = conflict.get();
            ButtonType cancelPrevious = new ButtonType("Отменить прошлое", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelCurrent  = new ButtonType("Отменить данное", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "В выбранное время уже есть активность \"" + conflicting.getActivity() + "\".",
                    cancelPrevious, cancelCurrent);
            alert.setHeaderText("Обнаружено пересечение по времени");
            alert.setTitle("Конфликт активностей");
            alert.initOwner(applyBtn.getScene().getWindow());

            ButtonType result = alert.showAndWait().orElse(cancelCurrent);
            if (result == cancelCurrent) {
                showError("Заявка не отправлена");
                return;
            }

            conflicting.setStatus(ActivityEntry.Status.AVAILABLE);
        }

        selected.setStatus(ActivityEntry.Status.REQUESTED);
        showInfo("Заявка отправлена организатору и ожидает подтверждения");
        activityTable.refresh();
    }

    private void showMyActivities() {
        List<ActivityEntry> mine = master.stream()
                .filter(ActivityEntry::isReserved)
                .sorted(Comparator.comparing(ActivityEntry::getDate)
                        .thenComparing(ActivityEntry::getStart))
                .collect(Collectors.toList());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Мои активности");
        dialog.setHeaderText(mine.isEmpty() ? "Пока нет заявок" : "Ваши активности");
        dialog.initOwner(myActivitiesBtn.getScene().getWindow());

        ListView<String> listView = new ListView<>();
        listView.getItems().setAll(mine.stream()
                .map(entry -> "%s (%s) %s–%s — %s".formatted(
                        entry.getActivity(),
                        DATE_FMT.format(entry.getDate()),
                        TIME_FMT.format(entry.getStart()),
                        TIME_FMT.format(entry.getEnd()),
                        entry.getStatus().getDisplayName()))
                .collect(Collectors.toList()));

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /* ─ helpers ───────────────────────────────────────────────────── */
    private void showInfo(String message) {
        infoLabel.setStyle("-fx-text-fill: #006400;");
        infoLabel.setText(message);
    }

    private void showError(String message) {
        infoLabel.setStyle("-fx-text-fill: #CC0000;");
        infoLabel.setText(message);
    }

    /* ─ UserAware ─────────────────────────────────────────────────── */
    @Override
    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            greetingLabel.setText("Добро пожаловать!");
            return;
        }

        LocalTime now = LocalTime.now();
        String part;
        if (now.isBefore(LocalTime.of(11, 1))) {
            part = "Доброе утро";
        } else if (now.isBefore(LocalTime.of(18, 1))) {
            part = "Добрый день";
        } else {
            part = "Добрый вечер";
        }
        greetingLabel.setText("%s, %s!".formatted(part, user.getFirstName()));
    }

    /* ─ model ──────────────────────────────────────────────────────── */
    public static class ActivityEntry {
        enum Status { AVAILABLE("Свободно"), REQUESTED("Заявка отправлена"), APPROVED("Одобрено");
            private final String displayName;
            Status(String displayName) { this.displayName = displayName; }
            public String getDisplayName() { return displayName; }
        }

        private final StringProperty activity = new SimpleStringProperty();
        private final StringProperty event = new SimpleStringProperty();
        private final StringProperty direction = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> start = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> end = new SimpleObjectProperty<>();
        private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.AVAILABLE);
        private final StringProperty statusText = new SimpleStringProperty(Status.AVAILABLE.getDisplayName());

        public ActivityEntry(String activity, String event, String direction,
                             LocalDateTime start, LocalDateTime end) {
            this.activity.set(activity);
            this.event.set(event);
            this.direction.set(direction);
            this.date.set(start.toLocalDate());
            this.start.set(start.toLocalTime());
            this.end.set(end.toLocalTime());

            status.addListener((obs, oldV, newV) ->
                    statusText.set(newV.getDisplayName()));
        }

        public boolean overlaps(ActivityEntry other) {
            if (!Objects.equals(getDate(), other.getDate())) {
                return false;
            }
            return !getEnd().isBefore(other.getStart()) && !other.getEnd().isBefore(getStart());
        }

        public boolean isReserved() {
            return getStatus() != Status.AVAILABLE;
        }

        public StringProperty activityProperty() { return activity; }
        public StringProperty eventProperty() { return event; }
        public StringProperty statusTextProperty() { return statusText; }
        public String getActivity() { return activity.get(); }
        public String getEvent() { return event.get(); }
        public String getDirection() { return direction.get(); }
        public LocalDate getDate() { return date.get(); }
        public LocalTime getStart() { return start.get(); }
        public LocalTime getEnd() { return end.get(); }
        public Status getStatus() { return status.get(); }
        public void setStatus(Status status) { this.status.set(status); }
    }
}
