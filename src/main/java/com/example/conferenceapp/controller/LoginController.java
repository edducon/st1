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
import java.util.prefs.Preferences;

import java.io.IOException;

public class LoginController {

    @FXML private TextField      idField;
    @FXML private PasswordField  passwordField;
    @FXML private ImageView      captchaImage;
    @FXML private TextField      captchaInput;
    @FXML private CheckBox       rememberCheck;
    @FXML private Label          errorLabel;
    @FXML private Label          lockLabel;
    @FXML private Button         loginBtn;

    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginController.class);
    private static final String PREF_REMEMBER = "remember.enabled";
    private static final String PREF_ID       = "remember.id";
    private static final String PREF_PWD      = "remember.pwd";

    private final UserDao userDao = new UserDao();
    private String captchaCode = "";
    private int    attempts    = 0;
    private boolean locked     = false;

    /* ----------------- init ----------------- */
    public void initialize() {
        refreshCaptcha();
        loadRememberedCredentials();
        rememberCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (!isSelected) {
                PREFS.putBoolean(PREF_REMEMBER, false);
                PREFS.remove(PREF_ID);
                PREFS.remove(PREF_PWD);
            }
        });
    }

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

        errorLabel.setText("");

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
        handleRememberChoice(idNum, pwd);
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

    private void handleRememberChoice(String idNum, String pwd) {
        if (rememberCheck.isSelected()) {
            PREFS.putBoolean(PREF_REMEMBER, true);
            PREFS.put(PREF_ID, idNum);
            PREFS.put(PREF_PWD, pwd);
        } else {
            PREFS.putBoolean(PREF_REMEMBER, false);
            PREFS.remove(PREF_ID);
            PREFS.remove(PREF_PWD);
        }
    }

    private void loadRememberedCredentials() {
        boolean remember = PREFS.getBoolean(PREF_REMEMBER, false);
        rememberCheck.setSelected(remember);
        if (remember) {
            idField.setText(PREFS.get(PREF_ID, ""));
            passwordField.setText(PREFS.get(PREF_PWD, ""));
        }
    }

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
