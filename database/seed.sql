USE smarthealthcare;

INSERT INTO users (id, full_name, email, mobile_number, password, role, status, email_verified, mobile_verified, enabled) VALUES
(1, 'System Admin', 'admin@smarthealthcare.local', '9000000000', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'ADMIN', 'APPROVED', true, true, true),
(2, 'Aarav Sharma', 'aarav@example.com', '9876543210', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'PATIENT', 'APPROVED', true, true, true),
(3, 'Dr Mira Patel', 'mira.doctor@example.com', '9876543211', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'DOCTOR', 'APPROVED', true, true, true),
(4, 'CityCare Labs', 'labs@example.com', '9876543212', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'LABORATORY', 'APPROVED', true, true, true),
(5, 'HealthPlus Pharmacy', 'pharmacy@example.com', '9876543213', '$2a$10$u/BasvFVJtrBG0v4mIztqOnHWETb4a9Ji1Sc3gmOhfjhBUyuhME0q', 'PHARMACY', 'APPROVED', true, true, true)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO patients (id, user_id, age, gender, allergy_details, near_dear_contact_number, disease_details)
VALUES (1, 2, 32, 'Male', 'No', '9876500000', 'Asthma')
ON DUPLICATE KEY UPDATE disease_details = VALUES(disease_details);

INSERT INTO doctors (id, user_id, age, experience, speciality, address, nmc_id)
VALUES (1, 3, 40, 12, 'Cardiology', 'Mumbai', 'NMC-2026-101')
ON DUPLICATE KEY UPDATE speciality = VALUES(speciality);

INSERT INTO laboratories (id, user_id, lab_name, address)
VALUES (1, 4, 'CityCare Labs', 'Mumbai')
ON DUPLICATE KEY UPDATE lab_name = VALUES(lab_name);

INSERT INTO pharmacies (id, user_id, license_number, address)
VALUES (1, 5, 'PHARMA-LIC-500', 'Mumbai')
ON DUPLICATE KEY UPDATE license_number = VALUES(license_number);

INSERT INTO appointment_slots (doctor_id, slot_start, slot_end, booked)
VALUES (1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 30 MINUTE, false);

INSERT INTO lab_tests (laboratory_id, test_name, price, instructions)
VALUES
(1, 'Complete Blood Count', 450.00, 'Morning sample preferred'),
(1, 'Fasting Blood Sugar', 250.00, 'Fasting required');

INSERT INTO medicines (pharmacy_id, name, price, stock)
VALUES
(1, 'Paracetamol 500mg', 35.00, 200),
(1, 'Salbutamol Inhaler', 280.00, 40);

INSERT INTO smartwatch_data (patient_id, systolic_bp, diastolic_bp, pulse, heart_rate, oxygen_level, sleep_hours)
VALUES (1, 118, 76, 78, 78, 98, 7.2);
