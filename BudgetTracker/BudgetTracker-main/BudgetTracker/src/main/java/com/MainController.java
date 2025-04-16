//package com;
//
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.scene.layout.StackPane;
//import javafx.stage.Stage;
//
//import java.io.IOException;
//
//public class MainController {
//
//    @FXML
//    private StackPane mainPane; // The main layout pane of the app
//
//    public void initialize() {
//        // Initial setup (if needed)
//    }
//
//    // Method to switch to the Home screen
//    public void showHomeScreen() throws IOException {
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("java/com/HomeScreen.fxml"));
//        Scene scene = new Scene(loader.load());
//
//        Stage primaryStage = (Stage) mainPane.getScene().getWindow();
//        primaryStage.setScene(scene);
//    }
//}
