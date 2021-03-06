package com.sqlservice;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    //klassen som startar hela programmet

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("studentView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        StudentController studentController = fxmlLoader.getController();
        studentController.setHostServices(getHostServices());
        stage.setTitle("Contoso University Administrator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}