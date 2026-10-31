INSERT INTO users (id, email, mobile_number, full_name, password, role, status, email_verified, mobile_verified, enabled)
VALUES
  (1, 'admin@smarthealthcare.local', '9000000000', 'System Admin', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'ADMIN', 'APPROVED', true, true, true)
ON DUPLICATE KEY UPDATE email = email;
