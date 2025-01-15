package com.lazydev.pdf_convert;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 700);
        primaryStage.setTitle("PDF Convert Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
