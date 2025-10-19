use e_commerce_db;
-- 쿠폰 발급 시스템 테스트용 데이터 초기화 SQL
-- 사용법: docker exec server-kotlin-mysql-1 mysql -u application -papplication e_commerce_db < test-data.sql

-- 기존 테스트 데이터 정리
DELETE FROM user_coupon WHERE user_id IN (1, 2, 3);
DELETE FROM coupon WHERE coupon_id IN (1, 2, 3);
DELETE FROM user WHERE user_id IN (1, 2, 3);

-- 쿠폰 데이터 삽입
INSERT INTO coupon (coupon_id, coupon_status, description, discount_amount, stock) VALUES 
(1, 'OPENED', '테스트쿠폰 1', 1000, 10),
(2, 'OPENED', '테스트쿠폰 2', 1000, 10),
(3, 'OPENED', '테스트쿠폰 3', 1000, 10);

-- 유저 데이터 삽입
INSERT INTO user (created_at, updated_at, user_id, name) VALUES 
(1760845267, 1760845267, 1, '테스트 유저 1'),
(1760845267, 1760845267, 2, '테스트 유저 2'),
(1760845267, 1760845267, 3, '테스트 유저 3');

-- 데이터 확인 쿼리
SELECT '쿠폰 데이터' as table_name, COUNT(*) as count FROM coupon WHERE coupon_id IN (1, 2, 3)
UNION ALL
SELECT '유저 데이터' as table_name, COUNT(*) as count FROM user WHERE user_id IN (1, 2, 3)
UNION ALL
SELECT '발급된 쿠폰' as table_name, COUNT(*) as count FROM user_coupon WHERE user_id IN (1, 2, 3);
