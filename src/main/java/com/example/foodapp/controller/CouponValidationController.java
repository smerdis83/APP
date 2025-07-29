package com.example.foodapp.controller;

import com.example.foodapp.handler.CouponHandler;
import com.example.foodapp.model.entity.Coupon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.SQLException;

public class CouponValidationController extends VBox {
    @FXML private TextField couponCodeField;
    @FXML private Button validateCouponBtn;
    @FXML private Label couponStatusLabel;
    @FXML private Label discountLabel;
    @FXML private VBox couponBox;
    
    private CouponHandler couponHandler;
    private Coupon currentCoupon;
    private int orderTotal;
    private int userId;
    private CouponValidationCallback callback;

    public interface CouponValidationCallback {
        void onCouponApplied(Coupon coupon, int discount);
        void onCouponRemoved();
    }

    public CouponValidationController() {
        this.couponHandler = new CouponHandler();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CouponValidation.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void initialize() {
        System.out.println("CouponValidationController initialized");
        validateCouponBtn.setOnAction(e -> {
            System.out.println("Apply coupon button clicked");
            validateCoupon();
        });
        couponCodeField.setOnAction(e -> {
            System.out.println("Coupon code field enter pressed");
            validateCoupon();
        });
        // Clear status when user starts typing
        couponCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                clearCouponStatus();
            }
        });
    }

    public void setOrderTotal(int total) {
        System.out.println("CouponValidationController.setOrderTotal called with: " + total);
        this.orderTotal = total;
        if (currentCoupon != null) {
            updateDiscountDisplay();
        }
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCallback(CouponValidationCallback callback) {
        this.callback = callback;
    }

    @FXML
    private void validateCoupon() {
        String code = couponCodeField.getText().trim();
        System.out.println("Validating coupon: " + code + ", orderTotal=" + orderTotal + ", userId=" + userId);
        if (code.isEmpty()) {
            showStatus("Please enter a coupon code", false);
            return;
        }
        try {
            Coupon coupon = couponHandler.validateCouponForUser(code, orderTotal, userId);
            System.out.println("Coupon validation result: " + (coupon != null ? "VALID" : "INVALID"));
            if (coupon != null) {
                currentCoupon = coupon;
                int discount = couponHandler.calculateDiscount(coupon, orderTotal);
                System.out.println("Coupon discount: " + discount);
                showStatus("Coupon applied successfully! Discount: " + discount, true);
                updateDiscountDisplay();
                if (callback != null) {
                    callback.onCouponApplied(coupon, discount);
                }
            } else {
                showStatus("Invalid coupon code, conditions not met, or already used", false);
                clearCoupon();
            }
        } catch (SQLException e) {
            System.out.println("Coupon validation error: " + e.getMessage());
            showStatus("Error validating coupon: " + e.getMessage(), false);
        }
    }

    public void clearCoupon() {
        currentCoupon = null;
        couponCodeField.clear();
        clearCouponStatus();
        if (callback != null) {
            callback.onCouponRemoved();
        }
    }

    public Coupon getCurrentCoupon() {
        return currentCoupon;
    }

    public int getCurrentDiscount() {
        if (currentCoupon != null) {
            return couponHandler.calculateDiscount(currentCoupon, orderTotal);
        }
        return 0;
    }

    private void updateDiscountDisplay() {
        if (currentCoupon != null) {
            int discount = couponHandler.calculateDiscount(currentCoupon, orderTotal);
            discountLabel.setText("Discount: " + discount);
            discountLabel.setVisible(true);
        } else {
            discountLabel.setVisible(false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        couponStatusLabel.setText(message);
        couponStatusLabel.setStyle(isSuccess ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        couponStatusLabel.setVisible(true);
    }

    private void clearCouponStatus() {
        couponStatusLabel.setVisible(false);
        discountLabel.setVisible(false);
    }
} 