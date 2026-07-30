
USE ecommerce;

-- Disable FK checks temporarily for cleanup
SET FOREIGN_KEY_CHECKS = 0;

-- Delete all synthetic duplicate/variant products
DELETE FROM cart_items WHERE product_id IN (SELECT product_id FROM products WHERE name LIKE '%(%');
DELETE FROM order_items WHERE product_id IN (SELECT product_id FROM products WHERE name LIKE '%(%');
DELETE FROM reviews WHERE product_id IN (SELECT product_id FROM products WHERE name LIKE '%(%');
DELETE FROM wishlist WHERE product_id IN (SELECT product_id FROM products WHERE name LIKE '%(%');

DELETE FROM products WHERE name LIKE '%(%';

SET FOREIGN_KEY_CHECKS = 1;
