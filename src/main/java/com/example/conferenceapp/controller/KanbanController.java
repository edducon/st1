package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.ActivityDao;
import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.model.Activity;
import com.example.conferenceapp.model.ActivityTask;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.print.PrinterJob;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KanbanController {

    @FXML private ComboBox<Event> eventBox;
    @FXML private ScrollPane boardScroll;
    @FXML private AnchorPane boardPane;
    @FXML private Label infoLabel;
    @FXML private Button exportBtn;

    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final EventDao eventDao = new EventDao();
    private final ActivityDao activityDao = new ActivityDao();
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private User organizer;

    public static void open(Event selected, User organizer, Scene parent) {
        try {
            FXMLLoader loader = new FXMLLoader(KanbanController.class.getResource("/com/example/conferenceapp/fxml/Kanban.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Kanban-доска мероприятия");
            stage.initOwner(parent.getWindow());
            stage.initModality(Modality.NONE);

            KanbanController controller = loader.getController();
            controller.organizer = organizer;
            controller.loadEvents(selected);

            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть Kanban-доску", e);
        }
    }

    public void initialize() {
        eventBox.setItems(events);
        eventBox.setButtonCell(new EventCell());
        eventBox.setCellFactory(cb -> new EventCell());
        eventBox.setOnAction(e -> refreshBoard());

        boardScroll.setFitToWidth(true);
        boardScroll.setFitToHeight(true);

        exportBtn.setOnAction(e -> exportToPdf());
    }

    private void loadEvents(Event selected) {
        if (organizer == null) return;
        events.setAll(eventDao.findByOrganizer(organizer.getId(), null, null));
        if (!events.isEmpty()) {
            if (selected != null) {
                events.stream().filter(ev -> ev.getId() == selected.getId()).findFirst()
                        .ifPresent(eventBox::setValue);
            }
            if (eventBox.getValue() == null) {
                eventBox.setValue(events.get(0));
            }
            refreshBoard();
        }
    }

    private void refreshBoard() {
        boardPane.getChildren().clear();
        Event event = eventBox.getValue();
        if (event == null) {
            infoLabel.setText("Выберите мероприятие");
            return;
        }

        List<Activity> activities = activityDao.findByEvent(event.getId());
        if (activities.isEmpty()) {
            infoLabel.setText("Для мероприятия пока нет активностей");
            return;
        }

        infoLabel.setText("Активностей: " + activities.size());

        double x = 20;
        double y = 20;
        double maxHeight = 0;
        for (Activity activity : activities) {
            VBox card = createCard(activity);
            card.setLayoutX(x);
            card.setLayoutY(y);

            enableDrag(card);

            boardPane.getChildren().add(card);

            y += card.getPrefHeight() + 20;
            maxHeight = Math.max(maxHeight, card.getPrefHeight());
            if (y + maxHeight > boardScroll.getHeight()) {
                y = 20;
                x += 260;
            }
        }
        boardPane.setPrefSize(x + 300, Math.max(boardScroll.getHeight(), y + 200));
    }

    private VBox createCard(Activity activity) {
        VBox box = new VBox(6);
        box.getStyleClass().add("kanban-card");
        box.setPrefWidth(240);
        box.setPrefHeight(160);
        box.setStyle("-fx-padding: 12; -fx-background-color: #f2f4f8; -fx-border-color: #d0d5dd; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label title = new Label(activity.getTitle());
        title.getStyleClass().add("kanban-title");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label time = new Label(TIME_FMT.format(activity.getStartTime()) + " – " + TIME_FMT.format(activity.getEndTime()));
        time.setStyle("-fx-text-fill: #475467;");

        Label jury = new Label("Жюри: " + (activity.getJury().isEmpty() ? "не назначено" : String.join(", ", activity.getJury())));
        jury.setWrapText(true);

        box.getChildren().addAll(title, time, jury);

        if (!activity.getTasks().isEmpty()) {
            Label tasksLabel = new Label("Задач: " + activity.getTasks().size());
            tasksLabel.setStyle("-fx-text-fill: #344054; -fx-font-size: 12px;");
            box.getChildren().add(tasksLabel);
        }

        box.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                showTasks(activity);
            }
        });

        return box;
    }

    private void enableDrag(Pane card) {
        final double[] offset = new double[2];
        card.setOnMousePressed(ev -> {
            offset[0] = ev.getSceneX() - card.getLayoutX();
            offset[1] = ev.getSceneY() - card.getLayoutY();
            card.toFront();
        });
        card.setOnMouseDragged(ev -> {
            card.setLayoutX(ev.getSceneX() - offset[0]);
            card.setLayoutY(ev.getSceneY() - offset[1]);
        });
    }

    private void showTasks(Activity activity) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Задачи активности");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(8);
        content.setPrefWidth(380);
        content.setStyle("-fx-padding: 15;");

        Label header = new Label(activity.getTitle());
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        content.getChildren().add(header);

        if (activity.getTasks().isEmpty()) {
            content.getChildren().add(new Label("Задачи пока не добавлены"));
        } else {
            for (ActivityTask task : activity.getTasks()) {
                VBox box = new VBox(2);
                Label title = new Label(task.getTitle());
                title.setStyle("-fx-font-weight: bold;");
                Label author = new Label(task.getAuthor() == null ? "Автор неизвестен" : task.getAuthor());
                author.setStyle("-fx-text-fill: #475467; -fx-font-size: 12px;");
                box.getChildren().addAll(title, author);
                box.setStyle("-fx-background-color: #eef2ff; -fx-padding: 8; -fx-background-radius: 6;");
                content.getChildren().add(box);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.initModality(Modality.NONE);
        dialog.show();
    }

    private void exportToPdf() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            if (job.showPrintDialog(exportBtn.getScene().getWindow())) {
                if (job.printPage(boardPane)) {
                    job.endJob();
                }
            }
        }
    }

    private static class EventCell extends ListCell<Event> {
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
