package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.LoginApp;
import java.util.*;
import java.util.stream.Collectors;

public class SearchController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> ratingCombo;
    @FXML private Button searchBtn;
    @FXML private Button clearBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private VBox foodsSection;
    @FXML private ListView<FoodItem> foodsList;
    @FXML private Label foodsLabel;

    private String jwtToken;
    private Runnable onBack;
    private LoginApp app;
    private ObservableList<FoodItem> foods = FXCollections.observableArrayList();
    private String initialSearch = null;
    public void setInitialSearch(String search) { this.initialSearch = search; }

    private static final List<String> RATING_OPTIONS = Arrays.asList(
        "Any Rating", "4+ Stars", "3+ Stars", "2+ Stars", "1+ Stars"
    );

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable callback) { this.onBack = callback; }
    public void setApp(LoginApp app) { this.app = app; }

    @FXML
    public void initialize() {
        foodsList.setItems(foods);
        ratingCombo.setItems(FXCollections.observableArrayList(RATING_OPTIONS));
        ratingCombo.setValue("Any Rating");
        searchBtn.setOnAction(e -> performSearch());
        clearBtn.setOnAction(e -> clearSearch());
        backBtn.setOnAction(e -> onBack.run());
        setupFoodCellFactory();
        foodsList.setOnMouseClicked(event -> {
            FoodItem selected = foodsList.getSelectionModel().getSelectedItem();
            if (selected != null && app != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) backBtn.getScene().getWindow();
                String lastSearch = searchField.getText();
                app.showRestaurantPage(stage, selected.getVendorId(), "Restaurant #" + selected.getVendorId(), "", () -> app.showSearchScreen(stage, lastSearch));
            }
        });
        if (initialSearch != null) {
            searchField.setText(initialSearch);
        }
        performSearch();
    }

    private void setupFoodCellFactory() {
        foodsList.setCellFactory(list -> new ListCell<FoodItem>() {
            @Override
            protected void updateItem(FoodItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + String.join(", ", item.getKeywords()) + ")");
                }
            }
        });
    }

    private void performSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        try {
            List<FoodItem> allFoods = getAllFoods();
            List<FoodItem> filtered;
            if (keyword.isEmpty()) {
                filtered = allFoods;
                messageLabel.setText("");
            } else {
                filtered = allFoods.stream()
                    .filter(food -> food.getKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(keyword)))
                    .collect(Collectors.toList());
                messageLabel.setText(filtered.isEmpty() ? "No foods found with that keyword." : "");
            }
            foods.setAll(filtered);
            foodsLabel.setText("Foods (" + filtered.size() + " found)");
        } catch (Exception e) {
            messageLabel.setText("Search failed: " + e.getMessage());
        }
    }

    private List<FoodItem> getAllFoods() throws Exception {
        com.example.foodapp.dao.FoodItemDao dao = new com.example.foodapp.dao.FoodItemDao();
        return dao.getAllFoodItems();
    }

    private void clearSearch() {
        searchField.clear();
        ratingCombo.setValue("Any Rating");
        foods.clear();
        foodsLabel.setText("Foods (0 found)");
        messageLabel.setText("");
    }
}