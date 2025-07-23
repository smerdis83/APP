package com.example.foodapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FrontendTestApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: white;");
        Button btn = new Button("Click me to turn background green");
        btn.setOnAction(e -> {
            root.setStyle("-fx-background-color: limegreen;");
            System.out.println("[FrontendTestApp] Button clicked, background should be green now.");
        });
        root.getChildren().add(btn);
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Frontend Minimal Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 