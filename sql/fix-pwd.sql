USE mental_health;
UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae' WHERE username = 'admin';
UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae' WHERE username = 'patient001';
UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae' WHERE username = 'patient002';
UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae' WHERE username = 'doctor001';
SELECT username, LEFT(password, 30) as pwd_prefix FROM user WHERE username IN ('admin','patient001','patient002','doctor001');
