package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.model.Event;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    /* ---------- UI-элементы ---------- */
    @FXML private ImageView logoBig;          // логотип в шапке
    @FXML private ComboBox<String> directionFilter;
    @FXML private DatePicker dateFilter;
    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, ImageView> logoCol;
    @FXML private TableColumn<Event, String> titleCol;
    @FXML private TableColumn<Event, String> directionCol;
    @FXML private TableColumn<Event, String> dateCol;
    @FXML private Button loginBtn;

    /* ---------- данные ---------- */
    private final ObservableList<Event> master   = FXCollections.observableArrayList();
    private final FilteredList<Event>   filtered = new FilteredList<>(master, p -> true);
    private final EventDao eventDao = new EventDao();

    /* ---------- инициализация контроллера ---------- */
    public void initialize() {

        /* логотип шапки */
        URL logoUrl = getClass().getResource(
                "/com/example/conferenceapp/images/logo.png");
        if (logoUrl != null)
            logoBig.setImage(new Image(logoUrl.toExternalForm()));

        /* колонки таблицы */
        logoCol.setCellValueFactory(p -> {
            String path = p.getValue().getLogoPath();
            ImageView iv = new ImageView(path != null
                    ? new Image("file:" + path, 64, 64, true, true)
                    : null);
            iv.setFitHeight(60); iv.setFitWidth(60);
            return new ReadOnlyObjectWrapper<>(iv);
        });
        titleCol    .setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getTitle()));
        directionCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getDirection()));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        dateCol     .setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getStart().format(df)));

        eventTable.setItems(filtered);

        /* данные + фильтры */
        loadData();
        directionFilter.setOnAction(e -> applyFilters());
        dateFilter     .setOnAction(e -> applyFilters());

        loginBtn.setOnAction(e ->
                LoginController.open(loginBtn.getScene())   // ← передаём сцену
        );

        /* двойной клик → карточка Details */
        eventTable.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    DetailsController.open(row.getItem());
                }
            });
            return row;
        });
    }


    /* ---------- загрузка всех мероприятий из DAO ---------- */
    private void loadData() {
        List<Event> all = eventDao.find(null, null);
        master.setAll(all);
        populateDirectionFilter();
    }

    private void populateDirectionFilter() {
        directionFilter.getItems().clear();
        master.stream()
                .map(Event::getDirection)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .forEach(directionFilter.getItems()::add);
    }

    /* ---------- применение фильтров ---------- */
    private void applyFilters() {
        String    dir = directionFilter.getValue();
        LocalDate d   = dateFilter.getValue();

        filtered.setPredicate(ev ->
                (dir == null || ev.getDirection().equals(dir)) &&
                        (d   == null || ev.getStart().toLocalDate().isEqual(d))
        );
    }

    /* ---------- кнопка «Сбросить» ---------- */
    @FXML private void onClearFilters() {
        directionFilter.setValue(null);
        dateFilter.setValue(null);
        applyFilters();
    }
}
