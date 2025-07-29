package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.model.entity.Transaction;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.example.foodapp.handler.CouponHandler;
import java.util.ArrayList;

public class OrderHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();
    private final CouponHandler couponHandler = new CouponHandler();

    public OrderHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Require Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        Claims claims;
        try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
        String role = claims.get("role", String.class);
        int userId = Integer.parseInt(claims.getSubject());

        try {
            if (method.equalsIgnoreCase("POST") && "/orders".equals(path)) {
                // Submit order (buyer only)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Order order = mapper.readValue(json, Order.class);
                order.setCustomerId(userId);
                // Price calculation logic & inventory check
                int rawPrice = 0;
                java.util.Map<Integer, Integer> toReduce = new java.util.HashMap<>(); // item_id -> quantity
                for (OrderItem oi : order.getItems()) {
                    FoodItem fi;
                    try {
                        fi = foodItemDao.getFoodItemById(oi.getItem_id());
                        if (fi == null) { sendJson(exchange, 400, Map.of("error", "Invalid item_id: " + oi.getItem_id())); return; }
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to fetch item: " + oi.getItem_id()));
                        return;
                    }
                    if (oi.getQuantity() > fi.getSupply()) {
                        sendJson(exchange, 409, Map.of("error", "Insufficient stock for item_id: " + oi.getItem_id()));
                        return;
                    }
                    toReduce.put(oi.getItem_id(), oi.getQuantity());
                    rawPrice += fi.getPrice() * oi.getQuantity();
                }
                // Calculate total price including tax and fees
                int taxAmount = (int) Math.round(rawPrice * (order.getTaxFee() / 100.0));
                
                // Get restaurant's actual additional fee from the first food item
                int additionalFee = 0;
                if (!order.getItems().isEmpty()) {
                    try {
                        FoodItem firstItem = foodItemDao.getFoodItemById(order.getItems().get(0).getItem_id());
                        if (firstItem != null) {
                            com.example.foodapp.dao.RestaurantDao restaurantDao = new com.example.foodapp.dao.RestaurantDao();
                            com.example.foodapp.model.entity.Restaurant restaurant = restaurantDao.findById(firstItem.getVendorId());
                            if (restaurant != null) {
                                additionalFee = restaurant.getAdditionalFee();
                                System.out.println("[DEBUG] Order creation: Restaurant ID=" + restaurant.getId() + ", Additional Fee=" + additionalFee);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[ERROR] Failed to get restaurant additional fee: " + e.getMessage());
                        additionalFee = order.getAdditionalFee(); // fallback to order request
                    }
                } else {
                    additionalFee = order.getAdditionalFee(); // fallback to order request
                }
                
                int courierFee = order.getCourierFee();
                int totalPrice = rawPrice + taxAmount + additionalFee + courierFee;
                
                // Coupon discount logic
                int couponDiscount = 0;
                if (order.getCouponId() != null && order.getCouponId() > 0) {
                    try {
                        com.example.foodapp.model.entity.Coupon coupon = couponHandler.getCouponById(order.getCouponId());
                        if (coupon == null) {
                            sendJson(exchange, 400, Map.of("error", "Invalid coupon_id"));
                            return;
                        }
                        // Validate coupon for this specific user and total order price
                        com.example.foodapp.model.entity.Coupon validCoupon = couponHandler.validateCouponForUser(coupon.getCouponCode(), totalPrice, userId);
                        if (validCoupon == null) {
                            sendJson(exchange, 400, Map.of("error", "Coupon not valid for this order or user"));
                            return;
                        }
                        couponDiscount = couponHandler.calculateDiscount(validCoupon, totalPrice);
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to validate coupon: " + e.getMessage()));
                        return;
                    }
                }
                // All items have enough stock, reduce supply
                for (Map.Entry<Integer, Integer> entry : toReduce.entrySet()) {
                    try {
                        FoodItem fi = foodItemDao.getFoodItemById(entry.getKey());
                        fi.setSupply(fi.getSupply() - entry.getValue());
                        foodItemDao.updateFoodItem(fi);
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to update stock for item_id: " + entry.getKey()));
                        return;
                    }
                }
                order.setRawPrice(rawPrice);
                order.setTaxFee(taxAmount);
                order.setAdditionalFee(additionalFee);
                order.setCourierFee(courierFee);
                // After calculating rawPrice, taxAmount, and additionalFee
                int extraExpensesTotal = 0;
                try {
                    com.example.foodapp.dao.ExtraExpenseDao extraExpenseDao = new com.example.foodapp.dao.ExtraExpenseDao();
                    int restaurantId = order.getItems().isEmpty() ? -1 : foodItemDao.getFoodItemById(order.getItems().get(0).getItem_id()).getVendorId();
                    if (restaurantId != -1) {
                        List<com.example.foodapp.model.entity.ExtraExpense> extras = extraExpenseDao.getExtraExpensesByRestaurant(restaurantId);
                        for (com.example.foodapp.model.entity.ExtraExpense e : extras) {
                            extraExpensesTotal += e.getAmount();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to fetch extra expenses: " + e.getMessage());
                }
                // When setting payPrice:
                order.setPayPrice(rawPrice + taxAmount + additionalFee + extraExpensesTotal);
                
                System.out.println("[DEBUG] Order price breakdown:");
                System.out.println("[DEBUG]   Raw Price: " + rawPrice);
                System.out.println("[DEBUG]   Tax Amount: " + taxAmount);
                System.out.println("[DEBUG]   Additional Fee: " + additionalFee);
                System.out.println("[DEBUG]   Courier Fee: " + courierFee);
                System.out.println("[DEBUG]   Total Price: " + totalPrice);
                System.out.println("[DEBUG]   Coupon Discount: " + couponDiscount);
                System.out.println("[DEBUG]   Final Pay Price: " + order.getPayPrice());
                
                order.setStatus("submitted");
                orderDao.addOrder(order);
                orderDao.insertOrderStatusHistory(order.getId(), "submitted", "buyer");
                
                // Record coupon usage after successful order creation
                if (order.getCouponId() != null && order.getCouponId() > 0) {
                    try {
                        couponHandler.recordCouponUsage(order.getCouponId(), userId);
                    } catch (Exception e) {
                        // Log the error but don't fail the order
                        System.err.println("Failed to record coupon usage: " + e.getMessage());
                    }
                }
                
                sendJson(exchange, 200, order);
                return;
            } else if (method.equalsIgnoreCase("GET") && path.matches("/orders/\\d+")) {
                // Get order details (buyer only, must own order)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                int orderId = extractIdFromPath(path, "/orders/");
                Order order = orderDao.getOrderById(orderId);
                if (order == null || order.getCustomerId() != userId) {
                    sendJson(exchange, 404, Map.of("error", "Order not found"));
                    return;
                }
                // If a courier is assigned, include their info in the response (as 'courier_info')
                Map<String, Object> response = mapper.convertValue(order, Map.class);
                // Add item_details: [{name, quantity}]
                java.util.List<Map<String, Object>> itemDetails = new java.util.ArrayList<>();
                if (order.getItems() != null) {
                    for (OrderItem oi : order.getItems()) {
                        try {
                            FoodItem fi = foodItemDao.getFoodItemById(oi.getItem_id());
                            String name = (fi != null) ? fi.getName() : ("Item #" + oi.getItem_id());
                            itemDetails.add(Map.of("name", name, "quantity", oi.getQuantity()));
                        } catch (Exception e) {
                            itemDetails.add(Map.of("name", "Item #" + oi.getItem_id(), "quantity", oi.getQuantity()));
                        }
                    }
                }
                response.put("item_details", itemDetails);
                if (order.getCourierId() != null) {
                    try {
                        User courier = new UserDao().findById(order.getCourierId());
                        if (courier != null) {
                            response.put("courier_info", Map.of(
                                "id", courier.getId(),
                                "full_name", courier.getFullName(),
                                "phone", courier.getPhone()
                            ));
                        }
                    } catch (Exception e) { /* ignore, just omit courier_info if error */ }
                }
                // Add status history timeline
                try {
                    List<com.example.foodapp.model.entity.OrderStatusHistory> history = orderDao.getOrderStatusHistory(order.getId());
                    response.put("status_history", history);
                } catch (Exception e) { /* ignore, just omit if error */ }
                sendJson(exchange, 200, response);
                return;
            } else if (method.equalsIgnoreCase("GET") && "/orders/history".equals(path)) {
                // Get order history (buyer only)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                List<Order> orders = orderDao.getOrdersByCustomer(userId);
                
                // Add restaurant names to each order
                List<Map<String, Object>> ordersWithRestaurantNames = new ArrayList<>();
                com.example.foodapp.dao.RestaurantDao restaurantDao = new com.example.foodapp.dao.RestaurantDao();
                
                for (Order order : orders) {
                    Map<String, Object> orderMap = mapper.convertValue(order, Map.class);
                    
                    // Fetch restaurant name
                    try {
                        com.example.foodapp.model.entity.Restaurant restaurant = restaurantDao.findById(order.getVendorId());
                        if (restaurant != null) {
                            orderMap.put("vendor_name", restaurant.getName());
                        } else {
                            orderMap.put("vendor_name", "Unknown Restaurant");
                        }
                    } catch (Exception e) {
                        orderMap.put("vendor_name", "Unknown Restaurant");
                    }
                    
                    ordersWithRestaurantNames.add(orderMap);
                }
                
                sendJson(exchange, 200, ordersWithRestaurantNames);
                return;
            } else {
                sendJson(exchange, 404, Map.of("error", "Not Found"));
                return;
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = mapper.writeValueAsString(payload);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private int extractIdFromPath(String path, String prefix) {
        String temp = path.substring(path.indexOf(prefix) + prefix.length());
        return Integer.parseInt(temp.replaceAll("[^0-9]", ""));
    }
} 