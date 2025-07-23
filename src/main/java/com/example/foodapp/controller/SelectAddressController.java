package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.util.function.Consumer;

public class SelectAddressController {
    @FXML private ListView<AddressItem> addressList;
    @FXML private Button selectBtn;
    @FXML private Button backBtn;
    @FXML private TextField titleField;
    @FXML private TextField addressField;
    @FXML private Button addBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private Runnable onBack;
    private Consumer<String> onSelect;
    private ObservableList<AddressItem> addresses = FXCollections.observableArrayList();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnSelect(Consumer<String> c) { this.onSelect = c; }

    @FXML
    public void initialize() {
        addressList.setItems(addresses);
        addressList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(AddressItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title + ": " + item.address);
            }
        });
        selectBtn.setOnAction(e -> handleSelect());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        addBtn.setOnAction(e -> handleAddAddress());
        fetchAddresses();
    }

    private void fetchAddresses() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/addresses");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch addresses: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<AddressItem> items = parseAddresses(json);
                Platform.runLater(() -> addresses.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    addresses.clear();
                    messageLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private List<AddressItem> parseAddresses(String json) {
        List<AddressItem> list = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\":", idx)) != -1) {
            int titleIdx = json.indexOf("\"title\":", idx);
            int titleStart = json.indexOf('"', titleIdx + 8) + 1;
            int titleEnd = json.indexOf('"', titleStart);
            String title = json.substring(titleStart, titleEnd);
            int addrIdx = json.indexOf("\"address\":", idx);
            int addrStart = json.indexOf('"', addrIdx + 10) + 1;
            int addrEnd = json.indexOf('"', addrStart);
            String address = json.substring(addrStart, addrEnd);
            list.add(new AddressItem(title, address));
            idx = addrEnd;
        }
        return list;
    }

    private void handleAddAddress() {
        String title = titleField.getText().trim();
        String address = addressField.getText().trim();
        if (title.isEmpty() || address.isEmpty()) {
            messageLabel.setText("Title and address required.");
            return;
        }
        new Thread(() -> {
            try {
                String json = String.format("{\"title\":\"%s\",\"address\":\"%s\"}", title.replace("\"", "'"), address.replace("\"", "'"));
                java.net.URL url = new java.net.URL("http://localhost:8000/addresses");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200 || code == 201) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Address added!");
                        titleField.clear();
                        addressField.clear();
                        fetchAddresses();
                    });
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed: " + resp));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void handleSelect() {
        AddressItem selected = addressList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select an address.");
            return;
        }
        if (onSelect != null) onSelect.accept(selected.address);
    }

    public static class AddressItem {
        public final String title;
        public final String address;
        public AddressItem(String title, String address) { this.title = title; this.address = address; }
        @Override public String toString() { return title + ": " + address; }
    }
} 