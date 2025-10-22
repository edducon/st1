package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.ReferenceDao;
import com.example.conferenceapp.dao.UserDao;
import com.example.conferenceapp.model.User;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProfileController {

    /* ---------- UI ---------- */
    @FXML private TextField      idField;
    @FXML private TextField      fullNameField;
    @FXML private TextField      emailField;
    @FXML private TextField      phoneField;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<String> countryBox;
    @FXML private PasswordField  pwd1;
    @FXML private PasswordField  pwd2;
    @FXML private CheckBox       showPwd;
    @FXML private Label          errorLabel;
    @FXML private Button         saveBtn;

    /* ---------- data ---------- */
    private final UserDao userDao = new UserDao();
    private final ReferenceDao ref = new ReferenceDao();
    private User user;

    /* ---------- init ---------- */
    public void initialize() {
        directionBox.getItems().setAll(ref.findAllDirections());
        countryBox  .getItems().setAll(ref.findAllCountries());

        showPwd.selectedProperty().addListener((o,ov,nv)->{
            pwd1.setPromptText(nv ? pwd1.getText() : "");
            pwd2.setPromptText(nv ? pwd2.getText() : "");
            pwd1.setDisable(nv); pwd2.setDisable(nv);
        });

        idField.setEditable(false);          // ID Number только для чтения
        saveBtn.setOnAction(e -> onSave());
    }

    /* ---------- external ---------- */
    public void loadUser(User u){
        this.user = u;

        idField      .setText(u.getIdNumber());
        fullNameField.setText(u.getFullName());
        emailField   .setText(u.getEmail());
        phoneField   .setText(u.getPhone());
        if(u.getDirection()!=null) directionBox.setValue(u.getDirection());
        if(u.getCountry()  !=null) countryBox  .setValue(u.getCountry());
    }

    /* ---------- save ---------- */
    private void onSave(){
        errorLabel.setText("");

        /* 1) читаем поля безопасно */
        user.setFullName (safe(fullNameField));
        user.setEmail    (safe(emailField));
        user.setPhone    (safe(phoneField));
        user.setDirection(directionBox.getValue());
        user.setCountry  (countryBox.getValue());

        /* 2) пароль (по-желанию) */
        if(!safe(pwd1).isBlank() || !safe(pwd2).isBlank()){
            if(!safe(pwd1).equals(safe(pwd2))){
                showError("Пароли не совпадают"); return;
            }
            user.setPasswordHash(pwd1.getText());   // raw pwd – DAO сам хэширует
        }else{
            user.setPasswordHash(null);             // не меняем
        }

        /* 3) DAO */
        if(userDao.update(user)){
            showOk("Данные сохранены");
        }else{
            showError("Ошибка сохранения");
        }
    }

    /* ---------- helpers ---------- */
    private static String safe(TextInputControl t){
        return t == null || t.getText()==null ? "" : t.getText().trim();
    }
    private void showError(String msg){ errorLabel.setStyle("-fx-text-fill:red;");   errorLabel.setText(msg);}
    private void showOk   (String msg){ errorLabel.setStyle("-fx-text-fill:green;"); errorLabel.setText(msg);}

    @FXML private void onClose(){
        ((Stage) idField.getScene().getWindow()).close();
    }

    public static void open(Scene parent, User u){
        try{
            var fx = new javafx.fxml.FXMLLoader(ProfileController.class
                    .getResource("/com/example/conferenceapp/fxml/Profile.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(fx.load()));
            st.setTitle("Профиль");
            fx.<ProfileController>getController().loadUser(u);
            st.initOwner(parent.getWindow());
            st.showAndWait();
        }catch(Exception ex){ ex.printStackTrace(); }
    }
}
