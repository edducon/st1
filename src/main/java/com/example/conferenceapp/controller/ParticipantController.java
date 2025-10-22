package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.ActivityDao;
import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.ParticipantActivity;
import com.example.conferenceapp.model.ResourceItem;
import com.example.conferenceapp.model.User;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Рабочее место участника: отображение активностей и ресурсов.
 */
public class ParticipantController implements UserAware {

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

    private final ObservableList<ActivityCard> activities = FXCollections.observableArrayList();
    private final ActivityDao activityDao = new ActivityDao();
    private final EventDao eventDao = new EventDao();
    private User user;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void initialize() {
        prepareActivityList();
        prepareResourceTable();

        activityList.setItems(activities);
        activityList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> showActivity(newV));

        addResourceBtn.disableProperty().bind(activityList.getSelectionModel().selectedItemProperty().isNull());
        addResourceBtn.setOnAction(e -> onAddResource());

        kanbanBtn.disableProperty().bind(activityList.getSelectionModel().selectedItemProperty().isNull());
        kanbanBtn.setOnAction(e -> openKanban());

        resourceTable.setPlaceholder(new Label("Ресурсы пока не добавлены"));
        participantList.setPlaceholder(new Label("Участники не найдены"));
        statusLabel.setText("");
    }

    private void prepareActivityList() {
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
    }

    private void prepareResourceTable() {
        resourceNameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        resourceOwnerCol.setCellValueFactory(data -> data.getValue().ownerProperty());
        resourceDateCol.setCellValueFactory(cell ->
                Bindings.createStringBinding(() -> {
                    LocalDateTime ts = cell.getValue().getUploadedAt();
                    return ts == null ? "" : DATE_TIME_FMT.format(ts);
                }, cell.getValue().uploadedAtProperty()));

        resourceActionsCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        resourceActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button downloadBtn = new Button("Открыть");
            private final Button copyLinkBtn = new Button("Скопировать ссылку");
            private final Button deleteBtn = new Button("Удалить");

            {
                downloadBtn.getStyleClass().add("button-primary");
                downloadBtn.setOnAction(e -> onDownload(getTableRow().getItem()));
                copyLinkBtn.setOnAction(e -> copyLink(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> onDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(ResourceEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    ToolBar bar = new ToolBar(downloadBtn, copyLinkBtn, deleteBtn);
                    setGraphic(bar);
                }
                setText(null);
            }
        });
    }

    private void reloadActivities() {
        activities.clear();
        participantList.setItems(FXCollections.observableArrayList());
        resourceTable.setItems(FXCollections.observableArrayList());

        if (user == null) {
            activityInfoLabel.setText("Авторизуйтесь для просмотра активностей");
            return;
        }

        List<ParticipantActivity> loaded = activityDao.findForParticipant(user.getId());
        activities.setAll(loaded.stream().map(ActivityCard::new).collect(Collectors.toList()));

        if (activities.isEmpty()) {
            activityInfoLabel.setText("Активности не найдены");
            setStatus("Нет запланированных активностей", false);
        } else {
            activityList.getSelectionModel().selectFirst();
            setStatus("Найдено активностей: " + activities.size(), true);
        }
    }

    private void onAddResource() {
        ActivityCard current = activityList.getSelectionModel().getSelectedItem();
        if (current == null) {
            showError("Выберите активность");
            return;
        }

        Optional<ResourceForm> result = showResourceDialog();
        if (result.isEmpty()) {
            return;
        }

        ResourceForm form = result.get();
        String author = user != null ? user.getFullName() : "Участник";
        Integer userId = user != null ? user.getId() : null;

        ResourceItem saved = activityDao.addResource(current.getActivityId(), form.name(), form.url(), userId, author);
        if (saved == null) {
            showError("Не удалось сохранить ресурс. Попробуйте позже.");
            return;
        }

        ResourceEntry entry = ResourceEntry.from(saved);
        current.getResources().add(entry);
        resourceTable.setItems(current.getResources());
        resourceTable.getSelectionModel().select(entry);
        showInfo("Ресурс «" + form.name() + "» добавлен");
    }

    private void onDownload(ResourceEntry entry) {
        if (entry == null) {
            return;
        }
        String url = entry.getUrl();
        if (url == null || url.isBlank()) {
            showInfo("Для ресурса «" + entry.getName() + "» не указана ссылка.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                showInfo("Ссылка открыта в браузере");
            } else {
                showInfo("Ссылка на ресурс: " + url);
            }
        } catch (Exception ex) {
            showError("Не удалось открыть ссылку: " + ex.getMessage());
        }
    }

    private void copyLink(ResourceEntry entry) {
        if (entry == null || entry.getUrl() == null || entry.getUrl().isBlank()) {
            showInfo("Ссылка отсутствует");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(entry.getUrl());
        Clipboard.getSystemClipboard().setContent(content);
        showInfo("Ссылка скопирована в буфер обмена");
    }

    private void onDelete(ResourceEntry entry) {
        if (entry == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление ресурса");
        alert.setHeaderText("Удалить «" + entry.getName() + "»?");
        alert.setContentText("Файл станет недоступен другим участникам.");
        alert.initOwner(getWindow());

        if (alert.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        if (activityDao.deleteResource(entry.getId())) {
            ActivityCard current = activityList.getSelectionModel().getSelectedItem();
            if (current != null) {
                current.getResources().remove(entry);
                showInfo("Ресурс удалён");
            }
        } else {
            showError("Не удалось удалить ресурс");
        }
    }

    private void showActivity(ActivityCard activity) {
        if (activity == null) {
            activityInfoLabel.setText("Активность не выбрана");
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

    private void openKanban() {
        ActivityCard selected = activityList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Event event = eventDao.findById(selected.getEventId());
        if (event == null) {
            showError("Не удалось загрузить данные мероприятия");
            return;
        }
        KanbanController.open(event, null, greetingLabel.getScene());
    }

    private Optional<ResourceForm> showResourceDialog() {
        Dialog<ResourceForm> dialog = new Dialog<>();
        dialog.setTitle("Новый ресурс");
        dialog.setHeaderText("Добавьте материал для участников");
        dialog.initOwner(getWindow());

        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Название файла или ссылки");
        TextField urlField = new TextField();
        urlField.setPromptText("URL (необязательно)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Название:"), nameField);
        grid.addRow(1, new Label("Ссылка:"), urlField);
        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.disableProperty().bind(nameField.textProperty().trim().isEmpty());

        dialog.setResultConverter(button -> {
            if (button == saveBtnType) {
                return new ResourceForm(nameField.getText().trim(), urlField.getText().trim());
            }
            return null;
        });

        Platform.runLater(nameField::requestFocus);
        return dialog.showAndWait().filter(form -> !form.name().isEmpty());
    }

    private void showInfo(String message) {
        setStatus(message, true);
    }

    private void showError(String message) {
        setStatus(message, false);
    }

    private void setStatus(String message, boolean ok) {
        statusLabel.setText(message);
        statusLabel.setStyle(ok ? "-fx-text-fill: #008000;" : "-fx-text-fill: #CC0000;");
    }

    private Window getWindow() {
        return greetingLabel != null ? greetingLabel.getScene().getWindow() : null;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            greetingLabel.setText(buildGreeting(user));
        } else {
            greetingLabel.setText("Добро пожаловать!");
        }
        reloadActivities();
    }

    private static String buildGreeting(User user) {
        LocalDateTime now = LocalDateTime.now();
        String part;
        if (now.toLocalTime().isBefore(java.time.LocalTime.of(11, 1))) {
            part = "Доброе утро";
        } else if (now.toLocalTime().isBefore(java.time.LocalTime.of(18, 1))) {
            part = "Добрый день";
        } else {
            part = "Добрый вечер";
        }
        return "%s, %s!".formatted(part, user.getFirstName());
    }

    private record ResourceForm(String name, String url) { }

    public static class ActivityCard {
        private final int activityId;
        private final int eventId;
        private final String title;
        private final String eventName;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final ObservableList<String> participants = FXCollections.observableArrayList();
        private final ObservableList<ResourceEntry> resources = FXCollections.observableArrayList();

        public ActivityCard(ParticipantActivity activity) {
            this.activityId = activity.getActivityId();
            this.eventId = activity.getEventId();
            this.title = activity.getActivityTitle();
            this.eventName = activity.getEventTitle();
            this.start = activity.getStart();
            this.end = activity.getEnd();
            this.participants.setAll(activity.getParticipants());
            this.resources.setAll(activity.getResources().stream().map(ResourceEntry::from).collect(Collectors.toList()));
        }

        public int getActivityId() { return activityId; }
        public int getEventId() { return eventId; }
        public String getTitle() { return title; }
        public String getEventName() { return eventName; }
        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        public ObservableList<String> getParticipants() { return participants; }
        public ObservableList<ResourceEntry> getResources() { return resources; }

        public String getDisplayName() {
            return "%s — %s %s–%s".formatted(title,
                    DATE_FMT.format(start.toLocalDate()),
                    TIME_FMT.format(start.toLocalTime()),
                    TIME_FMT.format(end.toLocalTime()));
        }
    }

    public static class ResourceEntry {
        private final int id;
        private final String url;
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty owner = new SimpleStringProperty();
        private final ObjectProperty<LocalDateTime> uploadedAt = new SimpleObjectProperty<>();

        public ResourceEntry(int id, String name, String owner, LocalDateTime uploadedAt, String url) {
            this.id = id;
            this.name.set(name);
            this.owner.set(owner);
            this.uploadedAt.set(uploadedAt);
            this.url = url;
        }

        public static ResourceEntry from(ResourceItem item) {
            return new ResourceEntry(item.getId(), item.getName(), item.getUploadedBy(), item.getUploadedAt(), item.getUrl());
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public StringProperty nameProperty() { return name; }
        public String getOwner() { return owner.get(); }
        public StringProperty ownerProperty() { return owner; }
        public LocalDateTime getUploadedAt() { return uploadedAt.get(); }
        public ObjectProperty<LocalDateTime> uploadedAtProperty() { return uploadedAt; }
        public String getUrl() { return url; }
    }
}
