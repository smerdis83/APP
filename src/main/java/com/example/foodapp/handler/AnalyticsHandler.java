package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();
    private final RestaurantDao restaurantDao = new RestaurantDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();

    public AnalyticsHandler() {
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

        Claims claims;
        try {
            claims = JwtUtil.parseToken(authHeader.substring("Bearer ".length()).trim());
        } catch (Exception e) {
            sendJson(exchange, 401, Map.of("error", "Invalid token"));
            return;
        }

        String role = claims.get("role", String.class);
        int userId = Integer.parseInt(claims.getSubject());

        // Only sellers and admins can access analytics
        if (!"SELLER".equals(role) && !"ADMIN".equals(role)) {
            sendJson(exchange, 403, Map.of("error", "Forbidden: must be a seller or admin"));
            return;
        }

        try {
            if (method.equalsIgnoreCase("GET") && path.startsWith("/analytics/sales")) {
                handleSalesAnalytics(exchange, userId, role);
                return;
            } else {
                sendJson(exchange, 404, Map.of("error", "Not Found"));
                return;
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void handleSalesAnalytics(HttpExchange exchange, int userId, String role) throws IOException, SQLException {
        // Parse query parameters
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQueryString(query);

        int sellerId = Integer.parseInt(params.getOrDefault("seller_id", String.valueOf(userId)));
        int restaurantId = Integer.parseInt(params.getOrDefault("restaurant_id", "0"));
        LocalDate startDate = LocalDate.parse(params.getOrDefault("start_date", LocalDate.now().minusDays(30).toString()));
        LocalDate endDate = LocalDate.parse(params.getOrDefault("end_date", LocalDate.now().toString()));

        System.out.println("=== ANALYTICS REQUEST ===");
        System.out.println("Seller ID: " + sellerId);
        System.out.println("Restaurant ID: " + restaurantId);
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        // Verify seller access (sellers can only see their own data, admins can see all)
        if ("SELLER".equals(role) && sellerId != userId) {
            sendJson(exchange, 403, Map.of("error", "Forbidden: can only access own analytics"));
            return;
        }

        // Get orders for the seller in the date range (all restaurants)
        List<Order> allOrders;
        if ("ADMIN".equals(role)) {
            // Admin: get all restaurants
            List<Restaurant> allRestaurants = restaurantDao.findAllAdmin();
            allOrders = new ArrayList<>();
            for (Restaurant r : allRestaurants) {
                allOrders.addAll(orderDao.getOrdersByVendor(r.getId()));
            }
            // Filter by date range
            allOrders = allOrders.stream()
                .filter(order -> {
                    LocalDate orderDate = order.getCreatedAt().toLocalDate();
                    return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        } else {
            allOrders = getOrdersForSeller(sellerId, startDate, endDate);
        }
        System.out.println("Found " + allOrders.size() + " orders for seller");
        
        // Filter orders by restaurant if specified
        List<Order> orders = allOrders;
        if (restaurantId > 0) {
            orders = allOrders.stream()
                .filter(order -> order.getVendorId() == restaurantId)
                .collect(Collectors.toList());
            System.out.println("After restaurant filtering: " + orders.size() + " orders");
        }
        
        // Calculate analytics
        Map<String, Object> analytics = calculateAnalytics(orders, startDate, endDate, restaurantId);
        
        System.out.println("=== END ANALYTICS REQUEST ===");
        sendJson(exchange, 200, analytics);
    }

    private List<Order> getOrdersForSeller(int sellerId, LocalDate startDate, LocalDate endDate) throws SQLException {
        // First, get all restaurants owned by this seller
        List<Restaurant> sellerRestaurants = restaurantDao.findByOwner(sellerId);
        System.out.println("Seller " + sellerId + " owns " + sellerRestaurants.size() + " restaurants:");
        for (Restaurant restaurant : sellerRestaurants) {
            System.out.println("  Restaurant " + restaurant.getId() + ": " + restaurant.getName());
        }
        
        // Get all orders for all restaurants owned by this seller
        List<Order> allOrders = new ArrayList<>();
        for (Restaurant restaurant : sellerRestaurants) {
            List<Order> restaurantOrders = orderDao.getOrdersByVendor(restaurant.getId());
            System.out.println("  Restaurant " + restaurant.getId() + " has " + restaurantOrders.size() + " orders");
            allOrders.addAll(restaurantOrders);
        }
        
        System.out.println("Total orders before date filtering: " + allOrders.size());
        
        // Filter by date range
        List<Order> filteredOrders = allOrders.stream()
            .filter(order -> {
                LocalDate orderDate = order.getCreatedAt().toLocalDate();
                return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
            
        System.out.println("Total orders after date filtering: " + filteredOrders.size());
        
        return filteredOrders;
    }

    private Map<String, Object> calculateAnalytics(List<Order> orders, LocalDate startDate, LocalDate endDate, int restaurantId) throws SQLException {
        Map<String, Object> analytics = new HashMap<>();

        System.out.println("=== ANALYTICS CALCULATION ===");
        System.out.println("Total orders found: " + orders.size());
        System.out.println("Date range: " + startDate + " to " + endDate);
        System.out.println("Report type: " + "daily"); // Removed report type parameter

        // Calculate summary statistics
        int totalOrders = orders.size();
        int totalRevenue = orders.stream().mapToInt(Order::getPayPrice).sum();
        int successfulOrders = (int) orders.stream().filter(o -> "delivered".equalsIgnoreCase(o.getStatus())).count();
        double successRate = totalOrders > 0 ? (double) successfulOrders / totalOrders * 100 : 0;
        double avgOrderValue = totalOrders > 0 ? (double) totalRevenue / totalOrders : 0;

        // Calculate net income (revenue - taxes - fees)
        int totalTaxes = orders.stream().mapToInt(Order::getTaxFee).sum();
        int totalFees = orders.stream().mapToInt(o -> o.getCourierFee() + o.getAdditionalFee()).sum();
        int netIncome = totalRevenue - totalTaxes - totalFees;

        System.out.println("Summary calculations:");
        System.out.println("  Total Revenue: " + totalRevenue);
        System.out.println("  Total Taxes: " + totalTaxes);
        System.out.println("  Total Fees: " + totalFees);
        System.out.println("  Net Income: " + netIncome);
        System.out.println("  Total Orders: " + totalOrders);
        System.out.println("  Successful Orders: " + successfulOrders);
        System.out.println("  Success Rate: " + successRate);
        System.out.println("  Avg Order Value: " + avgOrderValue);

        // Add summary to analytics
        analytics.put("summary", Map.of(
            "total_revenue", totalRevenue,
            "net_income", netIncome,
            "total_orders", totalOrders,
            "successful_orders", successfulOrders,
            "success_rate", Math.round(successRate * 100.0) / 100.0,
            "avg_order_value", Math.round(avgOrderValue * 100.0) / 100.0,
            "total_taxes", totalTaxes,
            "total_fees", totalFees
        ));

        // Calculate sales trends
        Map<String, Integer> salesTrends = calculateSalesTrends(orders, startDate, endDate, "daily"); // Default to daily
        analytics.put("sales_trends", salesTrends);

        // Calculate top selling foods
        List<Map<String, Object>> topFoods = calculateTopSellingFoods(orders, restaurantId);
        analytics.put("top_selling_foods", topFoods);

        // Calculate order status distribution
        Map<String, Integer> orderStatusDistribution = calculateOrderStatusDistribution(orders);
        analytics.put("order_status_distribution", orderStatusDistribution);

        // Get recent orders
        List<Map<String, Object>> recentOrders = getRecentOrders(orders);
        analytics.put("recent_orders", recentOrders);

        System.out.println("=== END ANALYTICS CALCULATION ===");
        return analytics;
    }

    private Map<String, Integer> calculateSalesTrends(List<Order> orders, LocalDate startDate, LocalDate endDate, String reportType) {
        Map<String, Integer> trends = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if ("daily".equalsIgnoreCase(reportType)) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                final LocalDate date = current;
                int dailyRevenue = orders.stream()
                    .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                    .mapToInt(Order::getPayPrice)
                    .sum();
                trends.put(current.format(formatter), dailyRevenue);
                current = current.plusDays(1);
            }
        } else if ("weekly".equalsIgnoreCase(reportType)) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                LocalDate weekEnd = current.plusDays(6);
                if (weekEnd.isAfter(endDate)) weekEnd = endDate;
                
                final LocalDate weekStart = current;
                final LocalDate weekEndFinal = weekEnd;
                int weeklyRevenue = orders.stream()
                    .filter(o -> {
                        LocalDate orderDate = o.getCreatedAt().toLocalDate();
                        return !orderDate.isBefore(weekStart) && !orderDate.isAfter(weekEndFinal);
                    })
                    .mapToInt(Order::getPayPrice)
                    .sum();
                trends.put(current.format(formatter) + " to " + weekEnd.format(formatter), weeklyRevenue);
                current = current.plusWeeks(1);
            }
        } else if ("monthly".equalsIgnoreCase(reportType)) {
            LocalDate current = startDate.withDayOfMonth(1);
            while (!current.isAfter(endDate)) {
                LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
                if (monthEnd.isAfter(endDate)) monthEnd = endDate;
                
                final LocalDate monthStart = current;
                final LocalDate monthEndFinal = monthEnd;
                int monthlyRevenue = orders.stream()
                    .filter(o -> {
                        LocalDate orderDate = o.getCreatedAt().toLocalDate();
                        return !orderDate.isBefore(monthStart) && !orderDate.isAfter(monthEndFinal);
                    })
                    .mapToInt(Order::getPayPrice)
                    .sum();
                trends.put(current.format(DateTimeFormatter.ofPattern("yyyy-MM")), monthlyRevenue);
                current = current.plusMonths(1);
            }
        }

        return trends;
    }

    private List<Map<String, Object>> calculateTopSellingFoods(List<Order> orders, int restaurantId) throws SQLException {
        Map<Integer, Integer> foodOrderCount = new HashMap<>(); // Count orders, not units
        Map<Integer, Integer> foodUnitSales = new HashMap<>(); // Keep track of units for revenue calculation
        Map<Integer, String> foodNames = new HashMap<>();

        System.out.println("=== TOP SELLING FOODS CALCULATION ===");
        System.out.println("Processing " + orders.size() + " orders for food analysis");
        System.out.println("Requested restaurant ID: " + restaurantId);

        // First, collect all unique food item IDs from all orders
        Set<Integer> allFoodItemIds = new HashSet<>();
        for (Order order : orders) {
            System.out.println("Processing order " + order.getId() + " with " + order.getItems().size() + " items");
            for (OrderItem item : order.getItems()) {
                allFoodItemIds.add(item.getItem_id());
                System.out.println("  Found food item ID: " + item.getItem_id());
            }
        }
        
        System.out.println("Total unique food items found in orders: " + allFoodItemIds.size());

        // Get names for all food items found in orders
        for (Integer itemId : allFoodItemIds) {
            try {
                FoodItem foodItem = foodItemDao.getFoodItemById(itemId);
                if (foodItem != null) {
                    foodNames.put(itemId, foodItem.getName());
                    foodOrderCount.put(itemId, 0); // Initialize with 0 orders
                    foodUnitSales.put(itemId, 0); // Initialize with 0 units
                    System.out.println("  Food item " + itemId + ": " + foodItem.getName());
                } else {
                    System.out.println("  Food item " + itemId + ": NOT FOUND in database");
                }
            } catch (Exception e) {
                System.out.println("  Error getting food item " + itemId + ": " + e.getMessage());
            }
        }

        // Count orders and units for each food item
        System.out.println("Counting orders and units for each food item:");
        for (Order order : orders) {
            System.out.println("  Order " + order.getId() + " - Restaurant: " + order.getVendorId() + " - Items: " + order.getItems().size());
            
            // Track which food items are in this order
            Set<Integer> itemsInThisOrder = new HashSet<>();
            
            for (OrderItem item : order.getItems()) {
                int itemId = item.getItem_id();
                int quantity = item.getQuantity();
                
                // Count units for revenue calculation
                foodUnitSales.merge(itemId, quantity, Integer::sum);
                
                // Track that this order contains this food item
                itemsInThisOrder.add(itemId);
                
                String foodName = foodNames.getOrDefault(itemId, "Unknown");
                System.out.println("    Item " + itemId + " (" + foodName + ") - Quantity: " + quantity);
            }
            
            // Count this order for each food item it contains
            for (Integer itemId : itemsInThisOrder) {
                foodOrderCount.merge(itemId, 1, Integer::sum);
                String foodName = foodNames.getOrDefault(itemId, "Unknown");
                System.out.println("    Order " + order.getId() + " contains " + foodName + " - Total orders so far: " + foodOrderCount.get(itemId));
            }
        }

        System.out.println("Food order count summary:");
        for (Map.Entry<Integer, Integer> entry : foodOrderCount.entrySet()) {
            String foodName = foodNames.getOrDefault(entry.getKey(), "Unknown");
            int units = foodUnitSales.getOrDefault(entry.getKey(), 0);
            System.out.println("  Item " + entry.getKey() + " (" + foodName + "): " + entry.getValue() + " orders, " + units + " units sold");
        }

        // Convert to list and sort by order count (not unit count)
        List<Map<String, Object>> topFoods = foodOrderCount.entrySet().stream()
            .map(entry -> {
                Map<String, Object> food = new HashMap<>();
                food.put("item_id", entry.getKey());
                food.put("name", foodNames.getOrDefault(entry.getKey(), "Unknown"));
                food.put("sales_count", entry.getValue()); // This now represents number of orders
                food.put("revenue", calculateFoodRevenue(orders, entry.getKey()));
                return food;
            })
            .sorted((a, b) -> Integer.compare((Integer) b.get("sales_count"), (Integer) a.get("sales_count")))
            .collect(Collectors.toList());

        System.out.println("Top foods result: " + topFoods.size() + " items");
        for (Map<String, Object> food : topFoods) {
            System.out.println("  " + food.get("name") + " - " + food.get("sales_count") + " orders - " + food.get("revenue") + " revenue");
        }
        System.out.println("=== END TOP SELLING FOODS CALCULATION ===");

        return topFoods;
    }

    private int calculateFoodRevenue(List<Order> orders, int itemId) {
        int totalRevenue = 0;
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getItem_id() == itemId) {
                        // Calculate proportional revenue for this item
                        double itemRatio = (double) item.getQuantity() / order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
                        totalRevenue += (int) (order.getPayPrice() * itemRatio);
                    }
                }
            }
        }
        return totalRevenue;
    }

    private Map<String, Integer> calculateOrderStatusDistribution(List<Order> orders) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (Order order : orders) {
            String status = order.getStatus().toLowerCase();
            distribution.put(status, distribution.getOrDefault(status, 0) + 1);
        }
        
        return distribution;
    }

    private List<Map<String, Object>> getRecentOrders(List<Order> orders) {
        return orders.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(10)
            .map(order -> {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", order.getId());
                orderInfo.put("status", order.getStatus());
                orderInfo.put("total", order.getPayPrice());
                orderInfo.put("created_at", order.getCreatedAt().toString());
                orderInfo.put("items_count", order.getItems() != null ? order.getItems().size() : 0);
                return orderInfo;
            })
            .collect(Collectors.toList());
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
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
}