package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.dao.ReferenceDao;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrganizerEventsController implements UserAware {

    @FXML private ComboBox<String> directionFilter;
    @FXML private DatePicker dateFilter;
    @FXML private TableView<Event> table;
    @FXML private TableColumn<Event, ImageView> logoCol;
    @FXML private TableColumn<Event, String> titleCol;
    @FXML private TableColumn<Event, String> directionCol;
    @FXML private TableColumn<Event, String> startCol;
    @FXML private Label summaryLabel;
    @FXML private Button createBtn;
    @FXML private Button exportBtn;
    @FXML private Button kanbanBtn;

    private final ObservableList<Event> master = FXCollections.observableArrayList();
    private final FilteredList<Event> filtered = new FilteredList<>(master, e -> true);

    private final EventDao eventDao = new EventDao();
    private final ReferenceDao referenceDao = new ReferenceDao();
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private User organizer;

    public static void open(Scene parent, User organizer) {
        try {
            FXMLLoader loader = new FXMLLoader(OrganizerEventsController.class.getResource("/com/example/conferenceapp/fxml/OrganizerEvents.fxml"));
            DialogPane pane = loader.load();

            OrganizerEventsController controller = loader.getController();
            controller.setUser(organizer);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Мероприятия организатора");
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(parent.getWindow());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.show();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть окно мероприятий", e);
        }
    }

    public void initialize() {
        logoCol.setCellValueFactory(param -> {
            String logo = param.getValue().getLogoPath();
            ImageView view = new ImageView();
            if (logo != null && !logo.isBlank()) {
                view.setImage(new Image("file:" + logo, 48, 48, true, true));
            }
            view.setFitWidth(48);
            view.setFitHeight(48);
            return new ReadOnlyObjectWrapper<>(view);
        });
        titleCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getTitle()));
        directionCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getDirection()));
        startCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(DATE_FMT.format(p.getValue().getStart())));

        table.setItems(filtered);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    KanbanController.open(row.getItem(), organizer, table.getScene());
                }
            });
            return row;
        });

        directionFilter.setOnAction(e -> applyFilters());
        dateFilter.setOnAction(e -> applyFilters());

        kanbanBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        exportBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadData() {
        if (organizer == null) return;
        List<Event> events = eventDao.findByOrganizer(organizer.getId(), directionFilter.getValue(), dateFilter.getValue());
        master.setAll(events);
        filtered.setPredicate(e -> true);
        populateDirections();
        updateSummary();
    }

    private void populateDirections() {
        String selected = directionFilter.getValue();
        directionFilter.getItems().setAll(referenceDao.findAllDirections());
        if (selected != null) {
            directionFilter.setValue(selected);
        }
    }

    private void applyFilters() {
        loadData();
    }

    private void updateSummary() {
        summaryLabel.setText("Всего мероприятий: " + filtered.size());
    }

    @FXML private void onCreate() {
        EventFormController.createNew(table.getScene(), organizer, this::loadData);
    }

    @FXML private void onExportCsv() {
        Event selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт мероприятия в CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName(selected.getTitle().replaceAll("\\s+", "_") + ".csv");
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Название;Направление;Дата начала;Дата окончания;Город;Организатор;Описание\n");
            writer.write(String.join(";",
                    safe(selected.getTitle()),
                    safe(selected.getDirection()),
                    DATE_FMT.format(selected.getStart()),
                    DATE_FMT.format(selected.getEnd()),
                    safe(selected.getCity()),
                    safe(selected.getOrganizer()),
                    safe(selected.getDescription())));
        } catch (IOException ex) {
            showError("Ошибка при сохранении файла: " + ex.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(';', ',');
    }

    @FXML private void onOpenKanban() {
        Event selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        KanbanController.open(selected, organizer, table.getScene());
    }

    @FXML private void onReset() {
        directionFilter.getSelectionModel().clearSelection();
        dateFilter.setValue(null);
        loadData();
    }

    @Override
    public void setUser(User user) {
        this.organizer = user;
        loadData();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(table.getScene().getWindow());
        alert.showAndWait();
    }

    static class EventCell extends ListCell<Event> {
        @Override
        protected void updateItem(Event item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getTitle());
            }
        }
    }
}
