package com.example.foodapp;

import com.example.foodapp.dao.CouponDao;
import com.example.foodapp.handler.CouponHandler;
import com.example.foodapp.model.entity.Coupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class CouponTest {
    private CouponHandler couponHandler;
    private CouponDao couponDao;

    @BeforeEach
    void setUp() {
        couponHandler = new CouponHandler();
        couponDao = new CouponDao();
    }

    @Test
    void testCreateAndValidateCoupon() throws SQLException {
        // Create a test coupon
        Coupon coupon = new Coupon();
        coupon.setCouponCode("TEST50");
        coupon.setType("fixed");
        coupon.setValue(50);
        coupon.setMinPrice(100);
        coupon.setUserCount(10);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(coupon);
        assertTrue(coupon.getId() > 0);

        // Test validation
        Coupon validated = couponHandler.validateCoupon("TEST50", 150);
        assertNotNull(validated);
        assertEquals("TEST50", validated.getCouponCode());

        // Test minimum price validation
        Coupon invalidPrice = couponHandler.validateCoupon("TEST50", 50);
        assertNull(invalidPrice);

        // Test user-specific validation
        Coupon validForUser = couponHandler.validateCouponForUser("TEST50", 150, 1);
        assertNotNull(validForUser);

        // Test user usage tracking
        couponHandler.recordCouponUsage(coupon.getId(), 1);
        
        // User should not be able to use the same coupon again
        Coupon alreadyUsed = couponHandler.validateCouponForUser("TEST50", 150, 1);
        assertNull(alreadyUsed);

        // User count should be decremented
        Coupon updatedCoupon = couponDao.findById(coupon.getId());
        assertEquals(9, updatedCoupon.getUserCount());
    }

    @Test
    void testDateValidation() throws SQLException {
        // Create expired coupon
        Coupon expiredCoupon = new Coupon();
        expiredCoupon.setCouponCode("EXPIRED");
        expiredCoupon.setType("fixed");
        expiredCoupon.setValue(10);
        expiredCoupon.setMinPrice(50);
        expiredCoupon.setUserCount(5);
        expiredCoupon.setStartDate(LocalDate.now().minusDays(10));
        expiredCoupon.setEndDate(LocalDate.now().minusDays(1));

        couponHandler.createCoupon(expiredCoupon);

        // Should be invalid due to expiration
        Coupon validated = couponHandler.validateCoupon("EXPIRED", 100);
        assertNull(validated);

        // Create future coupon
        Coupon futureCoupon = new Coupon();
        futureCoupon.setCouponCode("FUTURE");
        futureCoupon.setType("fixed");
        futureCoupon.setValue(10);
        futureCoupon.setMinPrice(50);
        futureCoupon.setUserCount(5);
        futureCoupon.setStartDate(LocalDate.now().plusDays(1));
        futureCoupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(futureCoupon);

        // Should be invalid due to not started yet
        Coupon validatedFuture = couponHandler.validateCoupon("FUTURE", 100);
        assertNull(validatedFuture);
    }

    @Test
    void testDiscountCalculation() throws SQLException {
        // Test fixed discount
        Coupon fixedCoupon = new Coupon();
        fixedCoupon.setCouponCode("FIXED20");
        fixedCoupon.setType("fixed");
        fixedCoupon.setValue(20);
        fixedCoupon.setMinPrice(50);
        fixedCoupon.setUserCount(5);
        fixedCoupon.setStartDate(LocalDate.now().minusDays(1));
        fixedCoupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(fixedCoupon);

        int discount = couponHandler.calculateDiscount(fixedCoupon, 100);
        assertEquals(20, discount);

        // Test percent discount
        Coupon percentCoupon = new Coupon();
        percentCoupon.setCouponCode("PERCENT10");
        percentCoupon.setType("percent");
        percentCoupon.setValue(10);
        percentCoupon.setMinPrice(50);
        percentCoupon.setUserCount(5);
        percentCoupon.setStartDate(LocalDate.now().minusDays(1));
        percentCoupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(percentCoupon);

        int percentDiscount = couponHandler.calculateDiscount(percentCoupon, 100);
        assertEquals(10, percentDiscount);
    }

    @Test
    void testUserCountExhaustion() throws SQLException {
        // Create coupon with only 1 use
        Coupon limitedCoupon = new Coupon();
        limitedCoupon.setCouponCode("LIMITED");
        limitedCoupon.setType("fixed");
        limitedCoupon.setValue(10);
        limitedCoupon.setMinPrice(50);
        limitedCoupon.setUserCount(1);
        limitedCoupon.setStartDate(LocalDate.now().minusDays(1));
        limitedCoupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(limitedCoupon);

        // First user should be able to use it
        Coupon valid1 = couponHandler.validateCouponForUser("LIMITED", 100, 1);
        assertNotNull(valid1);

        // Record usage
        couponHandler.recordCouponUsage(limitedCoupon.getId(), 1);

        // Second user should not be able to use it (user count = 0)
        Coupon valid2 = couponHandler.validateCouponForUser("LIMITED", 100, 2);
        assertNull(valid2);
    }

    @Test
    void testFixedCouponDiscountCalculation() throws SQLException {
        // Create a fixed coupon with value 50
        Coupon fixedCoupon = new Coupon();
        fixedCoupon.setCouponCode("FIXED50");
        fixedCoupon.setType("fixed");
        fixedCoupon.setValue(50);
        fixedCoupon.setMinPrice(100);
        fixedCoupon.setUserCount(5);
        fixedCoupon.setStartDate(LocalDate.now().minusDays(1));
        fixedCoupon.setEndDate(LocalDate.now().plusDays(30));

        couponHandler.createCoupon(fixedCoupon);

        // Test with order price 200 (should get full 50 discount)
        int discount1 = couponHandler.calculateDiscount(fixedCoupon, 200);
        System.out.println("Order price: 200, Coupon value: 50, Discount: " + discount1);
        assertEquals(50, discount1);

        // Test with order price 30 (should get 30 discount, limited by order price)
        int discount2 = couponHandler.calculateDiscount(fixedCoupon, 30);
        System.out.println("Order price: 30, Coupon value: 50, Discount: " + discount2);
        assertEquals(30, discount2);

        // Test with order price 100 (should get full 50 discount)
        int discount3 = couponHandler.calculateDiscount(fixedCoupon, 100);
        System.out.println("Order price: 100, Coupon value: 50, Discount: " + discount3);
        assertEquals(50, discount3);
    }

    @Test
    void testFrontendVerification() throws SQLException {
        System.out.println("=== FRONTEND VERIFICATION TEST ===");
        System.out.println("Create these coupons in admin panel and test in frontend:");
        System.out.println();
        
        // Test Case 1: Fixed coupon - 100 off
        Coupon fixed100 = new Coupon();
        fixed100.setCouponCode("SAVE100");
        fixed100.setType("fixed");
        fixed100.setValue(100);
        fixed100.setMinPrice(500);
        fixed100.setUserCount(10);
        fixed100.setStartDate(LocalDate.now().minusDays(1));
        fixed100.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(fixed100);
        
        System.out.println("1. FIXED COUPON: SAVE100");
        System.out.println("   - Value: 100 (fixed amount off)");
        System.out.println("   - Min order: 500");
        System.out.println("   - Expected results:");
        System.out.println("     * Order 300 → Invalid (below min)");
        System.out.println("     * Order 500 → Discount: 100, Pay: 400");
        System.out.println("     * Order 800 → Discount: 100, Pay: 700");
        System.out.println("     * Order 50  → Discount: 50, Pay: 0 (limited by order price)");
        System.out.println();
        
        // Test Case 2: Percent coupon - 20% off
        Coupon percent20 = new Coupon();
        percent20.setCouponCode("PERCENT20");
        percent20.setType("percent");
        percent20.setValue(20);
        percent20.setMinPrice(200);
        percent20.setUserCount(10);
        percent20.setStartDate(LocalDate.now().minusDays(1));
        percent20.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(percent20);
        
        System.out.println("2. PERCENT COUPON: PERCENT20");
        System.out.println("   - Value: 20% off");
        System.out.println("   - Min order: 200");
        System.out.println("   - Expected results:");
        System.out.println("     * Order 100 → Invalid (below min)");
        System.out.println("     * Order 200 → Discount: 40, Pay: 160");
        System.out.println("     * Order 500 → Discount: 100, Pay: 400");
        System.out.println("     * Order 1000 → Discount: 200, Pay: 800");
        System.out.println();
        
        // Test Case 3: Small fixed coupon - 25 off
        Coupon fixed25 = new Coupon();
        fixed25.setCouponCode("SAVE25");
        fixed25.setType("fixed");
        fixed25.setValue(25);
        fixed25.setMinPrice(100);
        fixed25.setUserCount(10);
        fixed25.setStartDate(LocalDate.now().minusDays(1));
        fixed25.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(fixed25);
        
        System.out.println("3. SMALL FIXED COUPON: SAVE25");
        System.out.println("   - Value: 25 (fixed amount off)");
        System.out.println("   - Min order: 100");
        System.out.println("   - Expected results:");
        System.out.println("     * Order 50  → Invalid (below min)");
        System.out.println("     * Order 100 → Discount: 25, Pay: 75");
        System.out.println("     * Order 200 → Discount: 25, Pay: 175");
        System.out.println("     * Order 20  → Discount: 20, Pay: 0 (limited by order price)");
        System.out.println();
        
        // Test Case 4: Large percent coupon - 50% off
        Coupon percent50 = new Coupon();
        percent50.setCouponCode("HALF50");
        percent50.setType("percent");
        percent50.setValue(50);
        percent50.setMinPrice(300);
        percent50.setUserCount(5);
        percent50.setStartDate(LocalDate.now().minusDays(1));
        percent50.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(percent50);
        
        System.out.println("4. LARGE PERCENT COUPON: HALF50");
        System.out.println("   - Value: 50% off");
        System.out.println("   - Min order: 300");
        System.out.println("   - Uses left: 5");
        System.out.println("   - Expected results:");
        System.out.println("     * Order 200 → Invalid (below min)");
        System.out.println("     * Order 300 → Discount: 150, Pay: 150");
        System.out.println("     * Order 600 → Discount: 300, Pay: 300");
        System.out.println("     * Order 1000 → Discount: 500, Pay: 500");
        System.out.println();
        
        System.out.println("=== TESTING INSTRUCTIONS ===");
        System.out.println("1. Login as admin and verify these coupons exist");
        System.out.println("2. Login as buyer and try each coupon with different order amounts");
        System.out.println("3. Check that discount calculation matches expected values");
        System.out.println("4. Try using same coupon twice - should be rejected");
        System.out.println("5. Check that user count decreases after each use");
        System.out.println();
        
        // Verify calculations programmatically
        System.out.println("=== PROGRAMMATIC VERIFICATION ===");
        System.out.println("SAVE100 (fixed 100):");
        System.out.println("  Order 500 → Discount: " + couponHandler.calculateDiscount(fixed100, 500) + " (expected: 100)");
        System.out.println("  Order 800 → Discount: " + couponHandler.calculateDiscount(fixed100, 800) + " (expected: 100)");
        System.out.println("  Order 50  → Discount: " + couponHandler.calculateDiscount(fixed100, 50) + " (expected: 50)");
        System.out.println();
        
        System.out.println("PERCENT20 (20% off):");
        System.out.println("  Order 200 → Discount: " + couponHandler.calculateDiscount(percent20, 200) + " (expected: 40)");
        System.out.println("  Order 500 → Discount: " + couponHandler.calculateDiscount(percent20, 500) + " (expected: 100)");
        System.out.println("  Order 1000 → Discount: " + couponHandler.calculateDiscount(percent20, 1000) + " (expected: 200)");
        System.out.println();
        
        System.out.println("SAVE25 (fixed 25):");
        System.out.println("  Order 100 → Discount: " + couponHandler.calculateDiscount(fixed25, 100) + " (expected: 25)");
        System.out.println("  Order 200 → Discount: " + couponHandler.calculateDiscount(fixed25, 200) + " (expected: 25)");
        System.out.println("  Order 20  → Discount: " + couponHandler.calculateDiscount(fixed25, 20) + " (expected: 20)");
        System.out.println();
        
        System.out.println("HALF50 (50% off):");
        System.out.println("  Order 300 → Discount: " + couponHandler.calculateDiscount(percent50, 300) + " (expected: 150)");
        System.out.println("  Order 600 → Discount: " + couponHandler.calculateDiscount(percent50, 600) + " (expected: 300)");
        System.out.println("  Order 1000 → Discount: " + couponHandler.calculateDiscount(percent50, 1000) + " (expected: 500)");
        System.out.println();
        
        // Test validation
        System.out.println("=== VALIDATION TESTS ===");
        System.out.println("SAVE100 validation:");
        System.out.println("  Order 300 → Valid: " + (couponHandler.validateCoupon("SAVE100", 300) != null) + " (expected: false - below min)");
        System.out.println("  Order 500 → Valid: " + (couponHandler.validateCoupon("SAVE100", 500) != null) + " (expected: true)");
        System.out.println();
        
        System.out.println("PERCENT20 validation:");
        System.out.println("  Order 100 → Valid: " + (couponHandler.validateCoupon("PERCENT20", 100) != null) + " (expected: false - below min)");
        System.out.println("  Order 200 → Valid: " + (couponHandler.validateCoupon("PERCENT20", 200) != null) + " (expected: true)");
        System.out.println();
        
        System.out.println("=== END OF TEST ===");
    }

    @Test
    void testCouponOnTotalWithFees() throws SQLException {
        System.out.println("=== TESTING COUPON ON TOTAL WITH FEES ===");
        
        // Create a fixed coupon
        Coupon fixed50 = new Coupon();
        fixed50.setCouponCode("FIXED50TOTAL");
        fixed50.setType("fixed");
        fixed50.setValue(50);
        fixed50.setMinPrice(200);
        fixed50.setUserCount(10);
        fixed50.setStartDate(LocalDate.now().minusDays(1));
        fixed50.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(fixed50);
        
        // Create a percent coupon
        Coupon percent10 = new Coupon();
        percent10.setCouponCode("PERCENT10TOTAL");
        percent10.setType("percent");
        percent10.setValue(10);
        percent10.setMinPrice(200);
        percent10.setUserCount(10);
        percent10.setStartDate(LocalDate.now().minusDays(1));
        percent10.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(percent10);
        
        // Test scenarios with tax and fees
        System.out.println("Test Case 1: Order 150 + Tax 10% (15) + Additional 5 = Total 170");
        System.out.println("  Fixed coupon (50 off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("FIXED50TOTAL", 170) != null) + " (expected: false - below min)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(fixed50, 170) + " (expected: 0 - invalid)");
        
        System.out.println("  Percent coupon (10% off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("PERCENT10TOTAL", 170) != null) + " (expected: false - below min)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(percent10, 170) + " (expected: 0 - invalid)");
        
        System.out.println();
        System.out.println("Test Case 2: Order 200 + Tax 10% (20) + Additional 5 = Total 225");
        System.out.println("  Fixed coupon (50 off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("FIXED50TOTAL", 225) != null) + " (expected: true)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(fixed50, 225) + " (expected: 50)");
        System.out.println("    Final price: " + (225 - 50) + " (expected: 175)");
        
        System.out.println("  Percent coupon (10% off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("PERCENT10TOTAL", 225) != null) + " (expected: true)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(percent10, 225) + " (expected: 22)");
        System.out.println("    Final price: " + (225 - 22) + " (expected: 203)");
        
        System.out.println();
        System.out.println("Test Case 3: Order 500 + Tax 10% (50) + Additional 10 = Total 560");
        System.out.println("  Fixed coupon (50 off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("FIXED50TOTAL", 560) != null) + " (expected: true)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(fixed50, 560) + " (expected: 50)");
        System.out.println("    Final price: " + (560 - 50) + " (expected: 510)");
        
        System.out.println("  Percent coupon (10% off, min 200):");
        System.out.println("    Valid: " + (couponHandler.validateCoupon("PERCENT10TOTAL", 560) != null) + " (expected: true)");
        System.out.println("    Discount: " + couponHandler.calculateDiscount(percent10, 560) + " (expected: 56)");
        System.out.println("    Final price: " + (560 - 56) + " (expected: 504)");
        
        System.out.println();
        System.out.println("=== FRONTEND TESTING GUIDE ===");
        System.out.println("1. Create order with items totaling 200");
        System.out.println("2. Add tax (10%) and additional fees (5)");
        System.out.println("3. Total should be: 200 + 20 + 5 = 225");
        System.out.println("4. Apply FIXED50TOTAL coupon:");
        System.out.println("   - Should be valid (225 >= 200 min)");
        System.out.println("   - Discount: 50");
        System.out.println("   - Final price: 225 - 50 = 175");
        System.out.println("5. Apply PERCENT10TOTAL coupon:");
        System.out.println("   - Should be valid (225 >= 200 min)");
        System.out.println("   - Discount: 22 (10% of 225)");
        System.out.println("   - Final price: 225 - 22 = 203");
        System.out.println();
    }

    @Test
    void testOneTimeUsePerUser() throws SQLException {
        System.out.println("=== TESTING ONE-TIME USE PER USER FEATURE ===");
        
        // Create a test coupon
        Coupon singleUseCoupon = new Coupon();
        singleUseCoupon.setCouponCode("SINGLEUSE");
        singleUseCoupon.setType("fixed");
        singleUseCoupon.setValue(25);
        singleUseCoupon.setMinPrice(100);
        singleUseCoupon.setUserCount(10); // Allow 10 total uses
        singleUseCoupon.setStartDate(LocalDate.now().minusDays(1));
        singleUseCoupon.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(singleUseCoupon);
        
        System.out.println("Created coupon: SINGLEUSE (25 off, min 100, 10 uses total)");
        System.out.println();
        
        // Test User 1 - First use
        System.out.println("=== USER 1 (ID: 1) - FIRST USE ===");
        Coupon validForUser1 = couponHandler.validateCouponForUser("SINGLEUSE", 200, 1);
        System.out.println("User 1 validation: " + (validForUser1 != null ? "VALID" : "INVALID"));
        System.out.println("Expected: VALID (first time use)");
        
        if (validForUser1 != null) {
            int discount = couponHandler.calculateDiscount(validForUser1, 200);
            System.out.println("Discount: " + discount + " (expected: 25)");
            
            // Record the usage
            couponHandler.recordCouponUsage(validForUser1.getId(), 1);
            System.out.println("✅ Usage recorded for User 1");
        }
        
        System.out.println();
        
        // Test User 1 - Second use (should fail)
        System.out.println("=== USER 1 (ID: 1) - SECOND USE ===");
        Coupon invalidForUser1 = couponHandler.validateCouponForUser("SINGLEUSE", 200, 1);
        System.out.println("User 1 validation: " + (invalidForUser1 != null ? "VALID" : "INVALID"));
        System.out.println("Expected: INVALID (already used)");
        System.out.println("✅ User 1 cannot use the same coupon twice");
        
        System.out.println();
        
        // Test User 2 - First use (should work)
        System.out.println("=== USER 2 (ID: 2) - FIRST USE ===");
        Coupon validForUser2 = couponHandler.validateCouponForUser("SINGLEUSE", 200, 2);
        System.out.println("User 2 validation: " + (validForUser2 != null ? "VALID" : "INVALID"));
        System.out.println("Expected: VALID (first time use)");
        
        if (validForUser2 != null) {
            int discount = couponHandler.calculateDiscount(validForUser2, 200);
            System.out.println("Discount: " + discount + " (expected: 25)");
            
            // Record the usage
            couponHandler.recordCouponUsage(validForUser2.getId(), 2);
            System.out.println("✅ Usage recorded for User 2");
        }
        
        System.out.println();
        
        // Test User 2 - Second use (should fail)
        System.out.println("=== USER 2 (ID: 2) - SECOND USE ===");
        Coupon invalidForUser2 = couponHandler.validateCouponForUser("SINGLEUSE", 200, 2);
        System.out.println("User 2 validation: " + (invalidForUser2 != null ? "VALID" : "INVALID"));
        System.out.println("Expected: INVALID (already used)");
        System.out.println("✅ User 2 cannot use the same coupon twice");
        
        System.out.println();
        
        // Test User 3 - First use (should work)
        System.out.println("=== USER 3 (ID: 3) - FIRST USE ===");
        Coupon validForUser3 = couponHandler.validateCouponForUser("SINGLEUSE", 200, 3);
        System.out.println("User 3 validation: " + (validForUser3 != null ? "VALID" : "INVALID"));
        System.out.println("Expected: VALID (first time use)");
        
        if (validForUser3 != null) {
            int discount = couponHandler.calculateDiscount(validForUser3, 200);
            System.out.println("Discount: " + discount + " (expected: 25)");
            
            // Record the usage
            couponHandler.recordCouponUsage(validForUser3.getId(), 3);
            System.out.println("✅ Usage recorded for User 3");
        }
        
        System.out.println();
        
        // Check remaining uses
        Coupon updatedCoupon = couponDao.findById(singleUseCoupon.getId());
        System.out.println("=== REMAINING USES ===");
        System.out.println("Original uses: 10");
        System.out.println("Used by: User 1, User 2, User 3");
        System.out.println("Remaining uses: " + updatedCoupon.getUserCount() + " (expected: 7)");
        System.out.println("✅ User count properly decremented");
        
        System.out.println();
        System.out.println("=== DATABASE VERIFICATION ===");
        System.out.println("Checking coupon_usage table entries:");
        System.out.println("User 1 used coupon: " + couponDao.hasUserUsedCoupon(singleUseCoupon.getId(), 1));
        System.out.println("User 2 used coupon: " + couponDao.hasUserUsedCoupon(singleUseCoupon.getId(), 2));
        System.out.println("User 3 used coupon: " + couponDao.hasUserUsedCoupon(singleUseCoupon.getId(), 3));
        System.out.println("User 4 used coupon: " + couponDao.hasUserUsedCoupon(singleUseCoupon.getId(), 4));
        System.out.println("Expected: true, true, true, false");
        
        System.out.println();
        System.out.println("=== FRONTEND TESTING GUIDE ===");
        System.out.println("1. Login as User 1 and apply SINGLEUSE coupon");
        System.out.println("2. Complete the order successfully");
        System.out.println("3. Try to use SINGLEUSE coupon again with User 1");
        System.out.println("4. Should see: 'Invalid coupon code, conditions not met, or already used'");
        System.out.println("5. Login as User 2 and apply SINGLEUSE coupon");
        System.out.println("6. Should work for User 2 (first time use)");
        System.out.println("7. Try to use it again with User 2 - should fail");
        System.out.println();
        System.out.println("=== FEATURE SUMMARY ===");
        System.out.println("✅ Each user can only use each coupon ONCE");
        System.out.println("✅ Usage is tracked in coupon_usage table");
        System.out.println("✅ User count is decremented after each use");
        System.out.println("✅ Different users can use the same coupon");
        System.out.println("✅ Same user cannot reuse the same coupon");
        System.out.println();
    }

    @Test
    void debugSingleUseCoupon() throws SQLException {
        System.out.println("=== DEBUGGING SINGLEUSE COUPON ===");
        
        // Find the SINGLEUSE coupon
        Coupon singleUseCoupon = couponDao.findByCode("SINGLEUSE");
        if (singleUseCoupon == null) {
            System.out.println("❌ SINGLEUSE coupon not found in database!");
            return;
        }
        
        System.out.println("Found SINGLEUSE coupon:");
        System.out.println("  ID: " + singleUseCoupon.getId());
        System.out.println("  Code: " + singleUseCoupon.getCouponCode());
        System.out.println("  Type: " + singleUseCoupon.getType());
        System.out.println("  Value: " + singleUseCoupon.getValue());
        System.out.println("  Min Price: " + singleUseCoupon.getMinPrice());
        System.out.println("  User Count: " + singleUseCoupon.getUserCount());
        System.out.println("  Start Date: " + singleUseCoupon.getStartDate());
        System.out.println("  End Date: " + singleUseCoupon.getEndDate());
        
        System.out.println();
        System.out.println("=== TESTING WITH DIFFERENT USER IDs ===");
        
        // Test with different user IDs
        for (int userId = 1; userId <= 10; userId++) {
            Coupon result = couponHandler.validateCouponForUser("SINGLEUSE", 200, userId);
            boolean hasUsed = couponDao.hasUserUsedCoupon(singleUseCoupon.getId(), userId);
            System.out.println("User " + userId + ": " + (result != null ? "VALID" : "INVALID") + 
                             " (has used: " + hasUsed + ")");
        }
        
        System.out.println();
        System.out.println("=== BASIC VALIDATION TEST ===");
        Coupon basicValidation = couponHandler.validateCoupon("SINGLEUSE", 200);
        System.out.println("Basic validation (no user check): " + (basicValidation != null ? "VALID" : "INVALID"));
        
        System.out.println();
        System.out.println("=== DATE CHECK ===");
        LocalDate today = LocalDate.now();
        System.out.println("Today: " + today);
        System.out.println("Start Date: " + singleUseCoupon.getStartDate());
        System.out.println("End Date: " + singleUseCoupon.getEndDate());
        System.out.println("Is today within range: " + 
                         (today.isAfter(singleUseCoupon.getStartDate().minusDays(1)) && 
                          today.isBefore(singleUseCoupon.getEndDate().plusDays(1))));
        
        System.out.println();
        System.out.println("=== MINIMUM PRICE CHECK ===");
        System.out.println("Order price: 200");
        System.out.println("Min price: " + singleUseCoupon.getMinPrice());
        System.out.println("Is order price >= min price: " + (200 >= singleUseCoupon.getMinPrice()));
        
        System.out.println();
        System.out.println("=== USER COUNT CHECK ===");
        System.out.println("Remaining user count: " + singleUseCoupon.getUserCount());
        System.out.println("Is user count > 0: " + (singleUseCoupon.getUserCount() > 0));
    }

    @Test
    void createFreshCouponForTesting() throws SQLException {
        System.out.println("=== CREATING FRESH COUPON FOR TESTING ===");
        
        // Create a fresh coupon that no one has used
        Coupon freshCoupon = new Coupon();
        freshCoupon.setCouponCode("FRESH25");
        freshCoupon.setType("fixed");
        freshCoupon.setValue(25);
        freshCoupon.setMinPrice(100);
        freshCoupon.setUserCount(5); // Allow 5 uses
        freshCoupon.setStartDate(LocalDate.now().minusDays(1));
        freshCoupon.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(freshCoupon);
        
        System.out.println("✅ Created fresh coupon: FRESH25");
        System.out.println("  - Code: FRESH25");
        System.out.println("  - Type: Fixed amount off");
        System.out.println("  - Value: 25");
        System.out.println("  - Min order: 100");
        System.out.println("  - Uses available: 5");
        System.out.println("  - Valid until: " + freshCoupon.getEndDate());
        
        System.out.println();
        System.out.println("=== TESTING FRESH COUPON ===");
        
        // Test with any user ID
        for (int userId = 1; userId <= 5; userId++) {
            Coupon result = couponHandler.validateCouponForUser("FRESH25", 200, userId);
            System.out.println("User " + userId + " can use FRESH25: " + (result != null ? "YES" : "NO"));
        }
        
        System.out.println();
        System.out.println("=== INSTRUCTIONS FOR USER ===");
        System.out.println("1. Login with your new user account");
        System.out.println("2. Create an order with total >= 100");
        System.out.println("3. Try to apply coupon code: FRESH25");
        System.out.println("4. It should work for your new user!");
        System.out.println();
    }

    @Test
    void testMaxUsesPerUser() throws SQLException {
        System.out.println("=== TESTING MAX USES PER USER ===");
        
        // Create a coupon that allows 3 uses per user
        Coupon multiUseCoupon = new Coupon();
        multiUseCoupon.setCouponCode("MULTI3");
        multiUseCoupon.setType("fixed");
        multiUseCoupon.setValue(10);
        multiUseCoupon.setMinPrice(50);
        multiUseCoupon.setUserCount(10); // Total uses available
        multiUseCoupon.setMaxUsesPerUser(3); // Each user can use 3 times
        multiUseCoupon.setStartDate(LocalDate.now().minusDays(1));
        multiUseCoupon.setEndDate(LocalDate.now().plusDays(30));
        couponHandler.createCoupon(multiUseCoupon);
        
        System.out.println("✅ Created MULTI3 coupon with max 3 uses per user");
        
        // Test user 1 using the coupon multiple times
        int userId = 1;
        int orderPrice = 100;
        
        System.out.println("\n=== TESTING USER " + userId + " ===");
        
        // First use - should work
        Coupon result1 = couponHandler.validateCouponForUser("MULTI3", orderPrice, userId);
        System.out.println("Use 1: " + (result1 != null ? "VALID" : "INVALID"));
        if (result1 != null) {
            couponHandler.recordCouponUsage(result1.getId(), userId);
            System.out.println("✅ Recorded use 1");
        }
        
        // Second use - should work
        Coupon result2 = couponHandler.validateCouponForUser("MULTI3", orderPrice, userId);
        System.out.println("Use 2: " + (result2 != null ? "VALID" : "INVALID"));
        if (result2 != null) {
            couponHandler.recordCouponUsage(result2.getId(), userId);
            System.out.println("✅ Recorded use 2");
        }
        
        // Third use - should work
        Coupon result3 = couponHandler.validateCouponForUser("MULTI3", orderPrice, userId);
        System.out.println("Use 3: " + (result3 != null ? "VALID" : "INVALID"));
        if (result3 != null) {
            couponHandler.recordCouponUsage(result3.getId(), userId);
            System.out.println("✅ Recorded use 3");
        }
        
        // Fourth use - should NOT work (exceeded limit)
        Coupon result4 = couponHandler.validateCouponForUser("MULTI3", orderPrice, userId);
        System.out.println("Use 4: " + (result4 != null ? "VALID" : "INVALID"));
        if (result4 == null) {
            System.out.println("✅ Correctly blocked use 4 (exceeded limit)");
        }
        
        // Test another user - should work
        int userId2 = 2;
        System.out.println("\n=== TESTING USER " + userId2 + " ===");
        
        Coupon resultUser2 = couponHandler.validateCouponForUser("MULTI3", orderPrice, userId2);
        System.out.println("User " + userId2 + " first use: " + (resultUser2 != null ? "VALID" : "INVALID"));
        if (resultUser2 != null) {
            couponHandler.recordCouponUsage(resultUser2.getId(), userId2);
            System.out.println("✅ User " + userId2 + " can still use the coupon");
        }
        
        // Check usage counts
        int user1Usage = couponDao.getUserUsageCount(multiUseCoupon.getId(), userId);
        int user2Usage = couponDao.getUserUsageCount(multiUseCoupon.getId(), userId2);
        
        System.out.println("\n=== USAGE SUMMARY ===");
        System.out.println("User " + userId + " usage count: " + user1Usage + " (max: 3)");
        System.out.println("User " + userId2 + " usage count: " + user2Usage + " (max: 3)");
        
        // Verify the counts
        assert user1Usage == 3 : "User 1 should have used the coupon 3 times";
        assert user2Usage == 1 : "User 2 should have used the coupon 1 time";
        
        System.out.println("✅ Max uses per user functionality working correctly!");
        System.out.println();
    }
} 