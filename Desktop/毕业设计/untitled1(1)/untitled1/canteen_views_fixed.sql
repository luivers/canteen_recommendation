/*
 修复后的视图SQL
 
 修复内容：
 1. category_sales: d.category 不存在 → 关联 categories 表，使用 c.name
 2. hourly_sales / monthly_sales_summary / sales_trend / user_consumption_behavior: 
    o.create_time 不存在于 orders 表 → 改用 order_items.create_time
 3. window_sales: o.window_id 不存在于 orders 表 → 通过 order_items 关联 windows
*/

SET NAMES utf8mb4;

-- ----------------------------
-- View structure for category_sales
-- 修复: d.category → 关联 categories 表使用 c.name
-- ----------------------------
DROP VIEW IF EXISTS `category_sales`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `category_sales` AS
SELECT
  `c`.`name` AS `category`,
  count(`oi`.`dish_id`) AS `total_sold`,
  sum(`oi`.`subtotal`) AS `total_revenue`,
  count(DISTINCT `oi`.`dish_id`) AS `unique_dishes`
FROM `dishes` `d`
  LEFT JOIN `categories` `c` ON (`d`.`category_id` = `c`.`id`)
  LEFT JOIN `order_items` `oi` ON (`d`.`id` = `oi`.`dish_id`)
  LEFT JOIN `orders` `o` ON (`oi`.`order_id` = `o`.`id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY `c`.`name`
ORDER BY `total_revenue` DESC;

-- ----------------------------
-- View structure for hourly_sales
-- 修复: o.create_time → oi.create_time (orders 表无 create_time 列)
-- ----------------------------
DROP VIEW IF EXISTS `hourly_sales`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `hourly_sales` AS
SELECT
  HOUR(`oi`.`create_time`) AS `hour_of_day`,
  count(`o`.`id`) AS `order_count`,
  sum(`o`.`total_amount`) AS `total_revenue`,
  avg(`o`.`total_amount`) AS `avg_order_value`
FROM `orders` `o`
  LEFT JOIN `order_items` `oi` ON (`o`.`id` = `oi`.`order_id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY HOUR(`oi`.`create_time`)
ORDER BY `hour_of_day`;

-- ----------------------------
-- View structure for monthly_sales_summary
-- 修复: o.create_time → oi.create_time
-- ----------------------------
DROP VIEW IF EXISTS `monthly_sales_summary`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `monthly_sales_summary` AS
SELECT
  YEAR(`oi`.`create_time`) AS `year`,
  MONTH(`oi`.`create_time`) AS `month`,
  count(DISTINCT `o`.`id`) AS `order_count`,
  sum(DISTINCT `o`.`total_amount`) AS `total_revenue`,
  count(DISTINCT `o`.`user_id`) AS `active_users`,
  count(DISTINCT `oi`.`window_id`) AS `active_windows`,
  count(DISTINCT `oi`.`dish_id`) AS `dishes_sold`
FROM `orders` `o`
  LEFT JOIN `order_items` `oi` ON (`o`.`id` = `oi`.`order_id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY YEAR(`oi`.`create_time`), MONTH(`oi`.`create_time`)
ORDER BY `year` DESC, `month` DESC;

-- ----------------------------
-- View structure for review_analysis (无需修复)
-- ----------------------------
DROP VIEW IF EXISTS `review_analysis`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `review_analysis` AS
SELECT
  floor(`r`.`overall_rating`) AS `rating`,
  count(`r`.`id`) AS `review_count`,
  round(((count(`r`.`id`) * 100.0) / (SELECT count(0) FROM `reviews`)), 2) AS `percentage`
FROM `reviews` `r`
GROUP BY floor(`r`.`overall_rating`)
ORDER BY `rating` DESC;

-- ----------------------------
-- View structure for sales_trend
-- 修复: o.create_time → oi.create_time
-- ----------------------------
DROP VIEW IF EXISTS `sales_trend`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `sales_trend` AS
SELECT
  cast(`oi`.`create_time` AS DATE) AS `sale_date`,
  count(DISTINCT `o`.`id`) AS `order_count`,
  sum(DISTINCT `o`.`total_amount`) AS `total_revenue`,
  count(DISTINCT `o`.`user_id`) AS `active_users`,
  count(DISTINCT `oi`.`window_id`) AS `active_windows`
FROM `orders` `o`
  LEFT JOIN `order_items` `oi` ON (`o`.`id` = `oi`.`order_id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY cast(`oi`.`create_time` AS DATE)
ORDER BY `sale_date` DESC;

-- ----------------------------
-- View structure for top_selling_dishes (无需修复)
-- ----------------------------
DROP VIEW IF EXISTS `top_selling_dishes`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `top_selling_dishes` AS
SELECT
  `d`.`id` AS `dish_id`,
  `d`.`name` AS `dish_name`,
  `d`.`price` AS `unit_price`,
  count(`oi`.`dish_id`) AS `total_sold`,
  sum(`oi`.`quantity`) AS `total_quantity`,
  sum(`oi`.`subtotal`) AS `total_revenue`,
  `d`.`average_rating` AS `avg_rating`,
  `d`.`rating_count` AS `rating_count`
FROM `dishes` `d`
  LEFT JOIN `order_items` `oi` ON (`d`.`id` = `oi`.`dish_id`)
  LEFT JOIN `orders` `o` ON (`oi`.`order_id` = `o`.`id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY `d`.`id`, `d`.`name`, `d`.`price`, `d`.`average_rating`, `d`.`rating_count`
ORDER BY `total_sold` DESC, `total_revenue` DESC;

-- ----------------------------
-- View structure for user_consumption_behavior
-- 修复: o.create_time → oi.create_time
-- ----------------------------
DROP VIEW IF EXISTS `user_consumption_behavior`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `user_consumption_behavior` AS
SELECT
  `u`.`id` AS `user_id`,
  `u`.`username` AS `username`,
  count(DISTINCT `o`.`id`) AS `order_count`,
  sum(DISTINCT `o`.`total_amount`) AS `total_spent`,
  avg(DISTINCT `o`.`total_amount`) AS `avg_order_value`,
  max(`oi`.`create_time`) AS `last_order_time`
FROM `users` `u`
  LEFT JOIN `orders` `o` ON (`u`.`id` = `o`.`user_id`)
  LEFT JOIN `order_items` `oi` ON (`o`.`id` = `oi`.`order_id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY `u`.`id`, `u`.`username`
ORDER BY `total_spent` DESC;

-- ----------------------------
-- View structure for window_sales
-- 修复: o.window_id 不存在 → 通过 order_items 关联 windows
-- ----------------------------
DROP VIEW IF EXISTS `window_sales`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `window_sales` AS
SELECT
  `w`.`id` AS `window_id`,
  `w`.`name` AS `window_name`,
  `w`.`location` AS `window_location`,
  count(DISTINCT `oi`.`order_id`) AS `order_count`,
  sum(`oi`.`subtotal`) AS `total_revenue`,
  avg(`oi`.`subtotal`) AS `avg_order_value`,
  count(DISTINCT `o`.`user_id`) AS `unique_customers`
FROM `windows` `w`
  LEFT JOIN `order_items` `oi` ON (`w`.`id` = `oi`.`window_id`)
  LEFT JOIN `orders` `o` ON (`oi`.`order_id` = `o`.`id`)
WHERE `o`.`status` = 'COMPLETED'
GROUP BY `w`.`id`, `w`.`name`, `w`.`location`
ORDER BY `total_revenue` DESC, `order_count` DESC;
