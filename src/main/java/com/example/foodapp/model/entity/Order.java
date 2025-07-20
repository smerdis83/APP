package com.example.foodapp.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.OrderStatusHistory;

public class Order {
    private int id;
    private String deliveryAddress;
    private int customerId;
    private int vendorId;
    private Integer couponId;
    private List<OrderItem> items;
    private int rawPrice;
    private int taxFee;
    private int additionalFee;
    private int courierFee;
    private int payPrice;
    private Integer courierId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderStatusHistory> statusHistory;

    public Order() {}

    public Order(int id, String deliveryAddress, int customerId, int vendorId, Integer couponId, List<OrderItem> items, int rawPrice, int taxFee, int additionalFee, int courierFee, int payPrice, Integer courierId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.deliveryAddress = deliveryAddress;
        this.customerId = customerId;
        this.vendorId = vendorId;
        this.couponId = couponId;
        this.items = items;
        this.rawPrice = rawPrice;
        this.taxFee = taxFee;
        this.additionalFee = additionalFee;
        this.courierFee = courierFee;
        this.payPrice = payPrice;
        this.courierId = courierId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public Integer getCouponId() { return couponId; }
    public void setCouponId(Integer couponId) { this.couponId = couponId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public int getRawPrice() { return rawPrice; }
    public void setRawPrice(int rawPrice) { this.rawPrice = rawPrice; }

    public int getTaxFee() { return taxFee; }
    public void setTaxFee(int taxFee) { this.taxFee = taxFee; }

    public int getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(int additionalFee) { this.additionalFee = additionalFee; }

    public int getCourierFee() { return courierFee; }
    public void setCourierFee(int courierFee) { this.courierFee = courierFee; }

    public int getPayPrice() { return payPrice; }
    public void setPayPrice(int payPrice) { this.payPrice = payPrice; }

    public Integer getCourierId() { return courierId; }
    public void setCourierId(Integer courierId) { this.courierId = courierId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderStatusHistory> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<OrderStatusHistory> statusHistory) { this.statusHistory = statusHistory; }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", customerId=" + customerId +
                ", vendorId=" + vendorId +
                ", couponId=" + couponId +
                ", items=" + items +
                ", rawPrice=" + rawPrice +
                ", taxFee=" + taxFee +
                ", additionalFee=" + additionalFee +
                ", courierFee=" + courierFee +
                ", payPrice=" + payPrice +
                ", courierId=" + courierId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 