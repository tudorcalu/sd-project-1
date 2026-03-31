package org.search;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.search.controller.SearchController;
import org.search.service.IndexBuilder;

import java.io.File;
import java.util.List;

public class Main extends Application {

    private final SearchController searchController = new SearchController();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Local File Search Engine");

        BorderPane root = new BorderPane();

        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        Button btnIndex = new Button("Index Directory");
        Label lblStatus = new Label("Ready");
        topBox.getChildren().addAll(btnIndex, lblStatus);

        btnIndex.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                lblStatus.setText("Indexing: " + selectedDirectory.getAbsolutePath());
                new Thread(() -> {
                    IndexBuilder builder = new IndexBuilder(4);
                    builder.buildIndex(selectedDirectory.getAbsolutePath());
                    Platform.runLater(() -> lblStatus.setText("Indexing Complete"));
                }).start();
            }
        });

        root.setTop(topBox);

        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10));
        
        HBox searchBox = new HBox(10);
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Enter search term");
        txtSearch.setPrefWidth(300);
        Button btnSearch = new Button("Search");
        searchBox.getChildren().addAll(txtSearch, btnSearch);

        ListView<SearchController.SearchResult> resultList = new ListView<>();
        resultList.setPrefHeight(400);

        btnSearch.setOnAction(e -> {
            String query = txtSearch.getText().trim();
            if (!query.isEmpty()) {
                List<SearchController.SearchResult> results = searchController.search(query);
                resultList.getItems().setAll(results);
            }
        });

        centerBox.getChildren().addAll(searchBox, resultList);
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
