package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.UserDao;
import com.example.conferenceapp.model.User;
import com.example.conferenceapp.util.CaptchaUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField      idField;
    @FXML private PasswordField  passwordField;
    @FXML private ImageView      captchaImage;
    @FXML private TextField      captchaInput;
    @FXML private CheckBox       rememberCheck;       // пока без логики
    @FXML private Label          errorLabel;
    @FXML private Label          lockLabel;
    @FXML private Button         loginBtn;

    private final UserDao userDao = new UserDao();
    private String captchaCode = "";
    private int    attempts    = 0;
    private boolean locked     = false;

    /* ----------------- init ----------------- */
    public void initialize() { refreshCaptcha(); }

    @FXML private void onRefreshCaptcha(){ refreshCaptcha(); }

    private void refreshCaptcha(){
        CaptchaUtil.Captcha c = CaptchaUtil.generate();
        captchaImage.setImage(c.image());
        captchaCode = c.code();
        captchaInput.clear();
    }

    /* ----------------- login ----------------- */
    @FXML private void onLogin(){
        if(locked) return;

        /* captcha */
        if(!captchaInput.getText().equalsIgnoreCase(captchaCode)){
            showError("Неверная CAPTCHA");
            handleFail();
            return;
        }

        String idNum = idField.getText().trim();
        String pwd   = passwordField.getText().trim();   // сырой пароль

        User u = userDao.authenticate(idNum, pwd);
        if(u == null){
            showError("Неверный ID или пароль");
            handleFail();
            return;
        }

        /* ---- успех ---- */
        openRoleWindow(u);

        /* скрываем главное окно и закрываем окно авторизации */
        Stage loginStage = (Stage) loginBtn.getScene().getWindow();
        Stage mainStage  = (Stage) loginStage.getOwner();
        if(mainStage!=null) mainStage.hide();
        loginStage.close();
    }

    /* ----------------- helpers ----------------- */
    private void handleFail(){
        attempts++;
        if(attempts>=3) lockFor10s();
        refreshCaptcha();
    }

    private void lockFor10s(){
        locked = true;
        lockLabel.setText("Блокировка на 10 секунд…");
        loginBtn.setDisable(true);

        new Thread(() -> {
            try{ Thread.sleep(10_000);}catch(InterruptedException ignored){}
            javafx.application.Platform.runLater(() -> {
                attempts = 0;
                locked   = false;
                loginBtn.setDisable(false);
                lockLabel.setText("");
            });
        }).start();
    }

    private void showError(String msg){ errorLabel.setText(msg); }

    private void openRoleWindow(User u){
        String fxml = switch (u.getRole()){
            case ORGANIZER  -> "/com/example/conferenceapp/fxml/Organizer.fxml";
            case MODERATOR  -> "/com/example/conferenceapp/fxml/Moderator.fxml";
            case JURY       -> "/com/example/conferenceapp/fxml/Jury.fxml";
            default         -> "/com/example/conferenceapp/fxml/Participant.fxml";
        };
        try{
            FXMLLoader fx = new FXMLLoader(getClass().getResource(fxml));
            Stage st = new Stage();
            st.setScene(new Scene(fx.load()));
            st.setTitle("Добро пожаловать, " + u.getSecondName());
            st.initModality(Modality.WINDOW_MODAL);

            Object controller = fx.getController();
            if (controller instanceof UserAware aware) {
                aware.setUser(u);
            }

            st.show();
        }catch(IOException ex){ ex.printStackTrace(); }
    }

    /* open() вызывается из MainController */
    public static void open(Scene ownerScene){
        try{
            Stage st = new Stage();
            st.initOwner(ownerScene.getWindow());
            st.initModality(Modality.APPLICATION_MODAL);
            st.setScene(new Scene(FXMLLoader.load(
                    LoginController.class.getResource(
                            "/com/example/conferenceapp/fxml/Login.fxml"))));
            st.setTitle("Авторизация");
            st.showAndWait();
        }catch(IOException ex){ ex.printStackTrace(); }
    }
}
