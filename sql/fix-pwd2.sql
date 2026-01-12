USE mental_health;
UPDATE user SET password = '$2a$10$w0O.ffalDpgJ.Vl8eKTQXODrp9bKk5AljKMmO5ubvPS1g/uWH6WMi' WHERE username IN ('admin', 'patient001', 'patient002', 'doctor001');
