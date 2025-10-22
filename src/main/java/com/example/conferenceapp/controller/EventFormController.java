package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.ActivityDao;
import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.dao.PersonDao;
import com.example.conferenceapp.dao.ReferenceDao;
import com.example.conferenceapp.model.Activity;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.LookupValue;
import com.example.conferenceapp.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class EventFormController {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<String> cityBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<LocalTime> startTimeBox;
    @FXML private ComboBox<LocalTime> endTimeBox;
    @FXML private TextArea descriptionArea;
    @FXML private TableView<ActivityDraft> activityTable;
    @FXML private TableColumn<ActivityDraft, String> activityTitleCol;
    @FXML private TableColumn<ActivityDraft, String> activityStartCol;
    @FXML private TableColumn<ActivityDraft, String> activityEndCol;
    @FXML private TableColumn<ActivityDraft, String> activityJuryCol;
    @FXML private Button addActivityBtn;
    @FXML private Button removeActivityBtn;

    private final ObservableList<ActivityDraft> activities = FXCollections.observableArrayList();
    private final ReferenceDao referenceDao = new ReferenceDao();
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();
    private final ActivityDao activityDao = new ActivityDao();

    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private User organizer;
    private Runnable onSaved;

    public static void createNew(Scene parent, User organizer, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(EventFormController.class.getResource("/com/example/conferenceapp/fxml/EventForm.fxml"));
            DialogPane pane = loader.load();

            EventFormController controller = loader.getController();
            controller.organizer = organizer;
            controller.onSaved = onSaved;
            controller.populateLookups();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Создание мероприятия");
            dialog.setDialogPane(pane);
            dialog.initOwner(parent.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);

            Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!controller.save()) {
                    ev.consume();
                }
            });

            dialog.showAndWait();

        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть форму создания мероприятия", e);
        }
    }

    public void initialize() {
        directionBox.setEditable(true);
        cityBox.setEditable(true);

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        startTimeBox.setItems(generateTimes());
        endTimeBox.setItems(generateTimes());

        activityTitleCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().title()));
        activityStartCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(TIME_FMT.format(p.getValue().start())));
        activityEndCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(TIME_FMT.format(p.getValue().end())));
        activityJuryCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(String.join(", ", p.getValue().juryNames())));

        activityTable.setItems(activities);
        activityTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        removeActivityBtn.disableProperty().bind(activityTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private ObservableList<LocalTime> generateTimes() {
        ObservableList<LocalTime> times = FXCollections.observableArrayList();
        LocalTime time = LocalTime.of(6, 0);
        while (!time.isAfter(LocalTime.of(23, 0))) {
            times.add(time);
            time = time.plusMinutes(15);
        }
        times.add(LocalTime.of(23, 45));
        return times;
    }

    private void populateLookups() {
        directionBox.getItems().setAll(referenceDao.findAllDirections());
        cityBox.getItems().setAll(referenceDao.findAllCities());
    }

    @FXML private void onAddActivity() {
        if (!validateEventTime(false)) {
            return;
        }

        Dialog<ActivityDraft> dialog = new Dialog<>();
        dialog.setTitle("Добавление активности");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleInput = new TextField();
        titleInput.setPromptText("Название активности");

        ComboBox<LocalTime> startBox = new ComboBox<>();
        startBox.setItems(FXCollections.observableArrayList(computeAvailableSlots()));
        startBox.setDisable(startBox.getItems().isEmpty());

        ListView<LookupValue> juryList = new ListView<>();
        List<LookupValue> juryOptions = personDao.loadUsersByRole("jury");
        juryList.getItems().setAll(juryOptions);
        juryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        juryList.setPrefHeight(150);

        grid.addRow(0, new Label("Название"), titleInput);
        grid.addRow(1, new Label("Начало"), startBox);
        grid.addRow(2, new Label("Жюри"), juryList);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            LocalTime start = startBox.getValue();
            if (start == null) {
                showError("Выберите время начала активности");
                return null;
            }
            String title = titleInput.getText();
            if (title == null || title.isBlank()) {
                showError("Введите название активности");
                return null;
            }

            LocalTime end = start.plusMinutes(90);

            List<LookupValue> selectedJury = new ArrayList<>(juryList.getSelectionModel().getSelectedItems());
            return new ActivityDraft(title, start, end, selectedJury);
        });

        dialog.showAndWait().ifPresent(activity -> {
            activities.add(activity);
            activities.sort(Comparator.comparing(ActivityDraft::start));
            activityTable.refresh();
        });
    }

    private List<LocalTime> computeAvailableSlots() {
        LocalTime eventStart = startTimeBox.getValue();
        LocalTime eventEnd = endTimeBox.getValue();
        if (eventStart == null || eventEnd == null) {
            return List.of();
        }

        List<ActivityDraft> sorted = new ArrayList<>(activities);
        sorted.sort(Comparator.comparing(ActivityDraft::start));

        List<LocalTime> available = new ArrayList<>();
        LocalTime cursor = eventStart;
        for (ActivityDraft draft : sorted) {
            while (!cursor.plusMinutes(90).isAfter(draft.start())) {
                if (!cursor.plusMinutes(90).isAfter(eventEnd)) {
                    available.add(cursor);
                }
                cursor = cursor.plusMinutes(15);
            }
            cursor = draft.end().plusMinutes(15);
        }

        while (!cursor.plusMinutes(90).isAfter(eventEnd)) {
            available.add(cursor);
            cursor = cursor.plusMinutes(15);
        }
        return available;
    }

    @FXML private void onRemoveActivity() {
        ActivityDraft selected = activityTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            activities.remove(selected);
        }
    }

    private boolean save() {
        if (!validateEventTime(true)) {
            return false;
        }

        if (titleField.getText() == null || titleField.getText().isBlank()) {
            showError("Введите название мероприятия");
            return false;
        }
        if (directionBox.getEditor().getText() == null || directionBox.getEditor().getText().isBlank()) {
            showError("Укажите направление");
            return false;
        }
        if (cityBox.getEditor().getText() == null || cityBox.getEditor().getText().isBlank()) {
            showError("Укажите город проведения");
            return false;
        }
        if (activities.isEmpty()) {
            showError("Добавьте хотя бы одну активность");
            return false;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        LocalTime startTime = startTimeBox.getValue();
        LocalTime endTime = endTimeBox.getValue();

        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

        int directionId = referenceDao.ensureDirection(directionBox.getEditor().getText().trim());
        int cityId = referenceDao.ensureCity(cityBox.getEditor().getText().trim());

        Event event = new Event(0, titleField.getText().trim(), directionId, directionBox.getEditor().getText().trim(),
                startDateTime, endDateTime, cityId, cityBox.getEditor().getText().trim(),
                organizer != null ? organizer.getId() : null, organizer != null ? organizer.getFullName() : null,
                null, descriptionArea.getText());

        int eventId = eventDao.insert(event);
        if (eventId == 0) {
            showError("Не удалось сохранить мероприятие");
            return false;
        }

        for (ActivityDraft draft : activities) {
            Activity activity = new Activity(eventId, draft.title(), 1, draft.start(), draft.end());
            int activityId = activityDao.insert(activity);
            if (!draft.jury().isEmpty()) {
                activityDao.assignJury(activityId, draft.juryIds());
            }
        }

        if (onSaved != null) {
            onSaved.run();
        }
        return true;
    }

    private boolean validateEventTime(boolean showMessage) {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        LocalTime startTime = startTimeBox.getValue();
        LocalTime endTime = endTimeBox.getValue();

        if (startDate == null || endDate == null || startTime == null || endTime == null) {
            if (showMessage) {
                showError("Заполните дату и время начала и окончания");
            }
            return false;
        }

        if (endDate.isBefore(startDate) || (endDate.isEqual(startDate) && !endTime.isAfter(startTime))) {
            if (showMessage) {
                showError("Дата и время окончания должны быть позже начала");
            }
            return false;
        }

        if (endTime.isAfter(LocalTime.MAX)) {
            if (showMessage) {
                showError("Время окончания не может превышать 24:00");
            }
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private record ActivityDraft(String title, LocalTime start, LocalTime end, List<LookupValue> jury) {
        List<String> juryNames() {
            return jury.stream().map(LookupValue::getLabel).toList();
        }

        List<Integer> juryIds() {
            return jury.stream().map(LookupValue::getId).toList();
        }
    }
}
