package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

public class TopUpWalletController {
    @FXML private Label balanceLabel;
    @FXML private TextField amountField;
    @FXML private Button topUpBtn;
    @FXML private Button backBtn;
    @FXML private VBox bankBox;
    @FXML private TextField bankNumberField;
    @FXML private Button confirmTopUpBtn;
    @FXML private Label messageLabel;
    @FXML private Button backToPaymentBtn;

    private String jwtToken;
    private Runnable onBack;
    private int balance = -1;
    private Integer prefillAmount = null;

    public void setJwtToken(String token) {
        this.jwtToken = token;
        if (balance == -1) fetchBalance();
    }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setBalance(int balance) {
        this.balance = balance;
        Platform.runLater(() -> balanceLabel.setText("Wallet Balance: " + balance));
    }
    public void setPrefillAmount(int amount) {
        this.prefillAmount = amount;
        Platform.runLater(() -> amountField.setText(String.valueOf(amount)));
    }

    @FXML
    public void initialize() {
        topUpBtn.setOnAction(e -> showBankStep());
        confirmTopUpBtn.setOnAction(e -> handleTopUp());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        backToPaymentBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        bankBox.setVisible(false); bankBox.setManaged(false);
        if (balance == -1) fetchBalance();
        if (prefillAmount != null) amountField.setText(String.valueOf(prefillAmount));
        backToPaymentBtn.setVisible(false); backToPaymentBtn.setManaged(false);
    }

    private void showBankStep() {
        String amountStr = amountField.getText();
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            messageLabel.setText("Enter a valid amount.");
            return;
        }
        bankBox.setVisible(true); bankBox.setManaged(true);
        messageLabel.setText("");
    }

    private void fetchBalance() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/wallet/balance");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200 && resp.contains("wallet_balance")) {
                    int idx = resp.indexOf(":");
                    int end = resp.indexOf("}", idx);
                    String balStr = resp.substring(idx + 1, end).replaceAll("[^0-9]", "").trim();
                    int bal = balStr.isEmpty() ? 0 : Integer.parseInt(balStr);
                    setBalance(bal);
                } else {
                    Platform.runLater(() -> balanceLabel.setText("Wallet Balance: ?"));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> balanceLabel.setText("Wallet Balance: ?"));
            }
        }).start();
    }

    private void handleTopUp() {
        String amountStr = amountField.getText();
        String bankNum = bankNumberField.getText();
        int amount;
        if (bankNum == null || bankNum.trim().isEmpty()) {
            messageLabel.setText("Enter a bank number.");
            return;
        }
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            messageLabel.setText("Enter a valid amount.");
            return;
        }
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/wallet/top-up");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                String json = String.format("{\"amount\":%d}", amount);
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
                        messageLabel.setText("Top up successful! You can now return to payment.");
                        fetchBalance();
                        bankBox.setVisible(false); bankBox.setManaged(false);
                        bankNumberField.clear();
                        amountField.clear();
                        if (onBack != null) {
                            backToPaymentBtn.setVisible(true); backToPaymentBtn.setManaged(true);
                        }
                    });
                } else {
                    Platform.runLater(() -> messageLabel.setText("Top up failed: " + resp));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Top up error: " + ex.getMessage()));
            }
        }).start();
    }
} 