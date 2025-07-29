# 🍕 Food Delivery Backend System

A comprehensive Java-based food delivery backend system with JavaFX frontend, featuring user authentication, restaurant management, order processing, payment handling, and real-time delivery tracking.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [User Roles & Permissions](#user-roles--permissions)
- [Key Features Explained](#key-features-explained)
- [Usage Guide](#usage-guide)
- [Development](#development)

## 🎯 Overview

This is a full-stack food delivery application that connects customers, restaurants, and couriers. The system supports multiple user roles, real-time order tracking, payment processing, and comprehensive restaurant management.

### Core Functionality
- **Multi-role User System**: Buyers, Sellers (Restaurant Owners), Couriers, and Admins
- **Restaurant Management**: Menu creation, inventory management, order processing
- **Order System**: Real-time order tracking, status updates, delivery management
- **Payment Processing**: Multiple payment methods including wallet system
- **Analytics**: Sales reports, order analytics, performance metrics
- **Search & Discovery**: Advanced restaurant and food item search

## 🏗️ Architecture

### Backend Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   JavaFX UI     │    │   HTTP Server   │    │   Database      │
│   (Frontend)    │◄──►│   (Backend)     │◄──►│   (MySQL)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Key Components
- **Controllers**: Handle UI interactions and business logic
- **Handlers**: Process HTTP requests and manage API endpoints
- **DAOs**: Data Access Objects for database operations
- **Services**: Business logic and authentication services
- **Models**: Entity classes representing database tables

## ✨ Features

### 🔐 Authentication & Authorization
- JWT-based authentication
- Role-based access control
- Secure password hashing
- Session management

### 🏪 Restaurant Management
- Restaurant registration and profile management
- Menu creation with categories
- Food item management with images
- Inventory tracking
- Pricing and discount management

### 🛒 Order System
- Shopping cart functionality
- Real-time order status tracking
- Order history and analytics
- Delivery management
- Rating and review system

### 💳 Payment System
- Multiple payment methods
- Digital wallet system
- Coupon and discount management
- Transaction history
- Payment verification

### 📊 Analytics & Reporting
- Sales analytics for restaurants
- Order performance metrics
- Revenue tracking
- Customer behavior analysis

### 🔍 Search & Discovery
- Restaurant search by location
- Food item search
- Filtering and sorting options
- Favorites management

## 🛠️ Technology Stack

### Backend
- **Java 17+**: Core programming language
- **JavaFX**: Desktop application framework
- **MySQL**: Relational database
- **JDBC**: Database connectivity
- **JWT**: Authentication tokens
- **Maven**: Build and dependency management

### Frontend
- **JavaFX**: Rich desktop UI
- **FXML**: UI layout definition
- **CSS**: Styling and theming

### Development Tools
- **IntelliJ IDEA**: IDE
- **Maven**: Build tool
- **Git**: Version control

## 📁 Project Structure

```
foodbackend/
├── src/
│   ├── main/
│   │   ├── java/com/example/foodapp/
│   │   │   ├── controller/          # UI Controllers
│   │   │   ├── handler/             # HTTP Request Handlers
│   │   │   ├── dao/                 # Data Access Objects
│   │   │   ├── model/entity/        # Database Entities
│   │   │   ├── service/             # Business Logic Services
│   │   │   ├── security/            # Authentication & Security
│   │   │   ├── util/                # Utility Classes
│   │   │   ├── App.java             # Main Application Entry
│   │   │   └── LoginApp.java        # Login Application
│   │   └── resources/
│   │       ├── fxml/                # UI Layout Files
│   │       ├── css/                 # Stylesheets
│   │       ├── pictures/            # Images & Assets
│   │       └── application.properties
│   └── test/                        # Test Files
├── pom.xml                          # Maven Configuration
└── README.md                        # This File
```

## 🚀 Installation & Setup

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher
- IntelliJ IDEA (recommended)

### Database Setup
1. Create a MySQL database:
```sql
CREATE DATABASE food_delivery;
```

2. Update `src/main/resources/application.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/food_delivery
db.username=your_username
db.password=your_password
```

### Application Setup
1. Clone the repository:
```bash
git clone <repository-url>
cd foodbackend
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.example.foodapp.App"
```

## 🗄️ Database Schema

### Core Tables
- **users**: User accounts and profiles
- **restaurants**: Restaurant information
- **menus**: Restaurant menu categories
- **food_items**: Individual food items
- **orders**: Order records
- **order_items**: Order line items
- **payments**: Payment transactions
- **ratings**: User ratings and reviews
- **addresses**: User delivery addresses
- **coupons**: Discount coupons
- **extra_expenses**: Additional fees

### Key Relationships
- Users can have multiple addresses
- Restaurants have multiple menus
- Menus contain multiple food items
- Orders contain multiple order items
- Users can rate restaurants and food items

## 🌐 API Endpoints

### Authentication
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/logout` - User logout

### Restaurants
- `GET /restaurants` - List all restaurants
- `GET /restaurants/{id}` - Get restaurant details
- `POST /restaurants` - Create restaurant (Seller only)
- `PUT /restaurants/{id}` - Update restaurant (Owner only)

### Orders
- `GET /orders` - Get user orders
- `POST /orders` - Create new order
- `PUT /orders/{id}/status` - Update order status
- `GET /orders/{id}` - Get order details

### Payments
- `POST /payment/online` - Process online payment
- `GET /wallet/balance` - Get wallet balance
- `POST /wallet/topup` - Add funds to wallet

### Search
- `GET /search/restaurants` - Search restaurants
- `GET /search/food` - Search food items

## 👥 User Roles & Permissions

### 🛒 Buyer
- Browse restaurants and menus
- Add items to cart
- Place orders
- Track order status
- Rate restaurants and food
- Manage delivery addresses
- Use wallet and coupons

### 🏪 Seller (Restaurant Owner)
- Manage restaurant profile
- Create and edit menus
- Add/remove food items
- View and process orders
- Access sales analytics
- Manage inventory

### 🚚 Courier
- View assigned deliveries
- Update delivery status
- Access delivery history
- Manage availability

### 👨‍💼 Admin
- Manage all users
- Monitor system activity
- Access global analytics
- Manage coupons and promotions
- System configuration

## 🔑 Key Features Explained

### Shopping Cart System
The cart system maintains state across different screens and calculates totals including:
- Base item prices
- Discount prices
- Tax calculations
- Additional fees
- Extra expenses

### Payment Processing
Supports multiple payment methods:
- **Online Payment**: Credit/debit card processing
- **Wallet Payment**: Digital wallet with balance
- **Coupon System**: Discount codes and promotions

### Real-time Order Tracking
Order status flow:
1. **Pending** - Order placed, waiting for restaurant confirmation
2. **Confirmed** - Restaurant accepted the order
3. **Preparing** - Food is being prepared
4. **Ready** - Food is ready for pickup
5. **In Transit** - Courier picked up the order
6. **Delivered** - Order completed

### Search & Discovery
Advanced search capabilities:
- Restaurant search by name, cuisine, location
- Food item search with filters
- Price range filtering
- Rating-based sorting
- Distance-based recommendations

## 📖 Usage Guide

### For Buyers
1. **Registration**: Create account with phone and password
2. **Browse**: Search and browse restaurants
3. **Order**: Add items to cart and place order
4. **Track**: Monitor order status in real-time
5. **Pay**: Complete payment using preferred method
6. **Rate**: Provide feedback after delivery

### For Sellers
1. **Setup**: Register restaurant and complete profile
2. **Menu Management**: Create menus and add food items
3. **Order Processing**: Accept and manage incoming orders
4. **Analytics**: Monitor sales and performance metrics
5. **Inventory**: Track stock levels and availability

### For Couriers
1. **Registration**: Sign up as delivery partner
2. **Orders**: View assigned delivery orders
3. **Updates**: Update delivery status in real-time
4. **History**: Access delivery history and earnings

## 🛠️ Development

### Adding New Features
1. Create entity classes in `model/entity/`
2. Add DAO methods in appropriate DAO class
3. Create handler methods for API endpoints
4. Add UI controllers and FXML files
5. Update navigation in `LoginApp.java`

### Code Style Guidelines
- Use meaningful variable and method names
- Add comments for complex logic
- Follow Java naming conventions
- Maintain consistent indentation
- Use proper exception handling

### Testing
- Unit tests for business logic
- Integration tests for API endpoints
- UI tests for critical user flows
- Database migration tests

### Deployment
1. Build the application: `mvn clean package`
2. Set up production database
3. Configure application properties
4. Deploy JAR file to server
5. Start application with proper JVM settings

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Make changes and test thoroughly
4. Submit pull request with detailed description
5. Ensure all tests pass

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**Built with ❤️ using Java, JavaFX, and MySQL** 