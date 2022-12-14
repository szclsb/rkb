package ch.szclsb.rkb.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class Main extends Application {
    private URL find(String resource) throws IOException {
        return Optional.ofNullable(getClass().getResource(resource))
                .orElseThrow(() -> new FileNotFoundException(resource));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var loader = new FXMLLoader(find("main.fxml"));
        var root = (Parent) loader.load();
        var controller = (FxController) loader.getController();

        var scene = new Scene(root);
//        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        primaryStage.setTitle("Remote Keyboard");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            try {
                controller.terminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
