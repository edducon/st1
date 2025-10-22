package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class DetailsController {

    /* UI */
    @FXML private ImageView logo;
    @FXML private Label title;
    @FXML private Label date;
    @FXML private Label city;
    @FXML private Label organizer;
    @FXML private TextArea description;

    /* заполнение данными */
    public void setEvent(Event e) {
        if (e.getLogoPath() != null)
            logo.setImage(new Image("file:" + e.getLogoPath()));

        title.setText(e.getTitle());
        date.setText(e.getStart().format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        city.setText(e.getCity());
        organizer.setText(
                e.getOrganizer() == null ? "—" : e.getOrganizer());
        description.setText(e.getDescription());
    }

    /* статическая фабрика окна */
    public static void open(Event event) {
        try {
            FXMLLoader fx = new FXMLLoader(
                    DetailsController.class.getResource(
                            "/com/example/conferenceapp/fxml/Details.fxml"));
            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Информация о мероприятии");

            Scene scene = new Scene(fx.load());
            st.setScene(scene);

            DetailsController c = fx.getController();
            c.setEvent(event);

            st.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
