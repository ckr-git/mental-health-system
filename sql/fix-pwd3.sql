USE mental_health;
UPDATE user SET password = '$2a$10$kb6yGrSk0hUrD8G6mwFHDeSIj4mn8VbtgsjjQpiP5RK21kWN7/zE6' WHERE username IN ('admin', 'patient001', 'patient002', 'doctor001');
