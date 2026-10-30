CREATE DATABASE IF NOT EXISTS smarthealthcare;
USE smarthealthcare;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) UNIQUE,
  mobile_number VARCHAR(20) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('PATIENT','DOCTOR','ADMIN','PHARMACY','LABORATORY') NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED','BLOCKED') DEFAULT 'PENDING',
  email_verified BOOLEAN DEFAULT FALSE,
  mobile_verified BOOLEAN DEFAULT FALSE,
  enabled BOOLEAN DEFAULT TRUE,
  INDEX idx_users_role (role),
  INDEX idx_users_status (status)
);

CREATE TABLE patients (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT NOT NULL UNIQUE,
  age INT,
  gender VARCHAR(40),
  allergy_details VARCHAR(500) DEFAULT 'No',
  near_dear_contact_number VARCHAR(20),
  disease_details VARCHAR(500) DEFAULT 'No',
  CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE doctors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT NOT NULL UNIQUE,
  age INT,
  experience INT,
  speciality VARCHAR(120),
  address VARCHAR(255),
  nmc_id VARCHAR(80) UNIQUE,
  CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_doctor_search (speciality, address)
);

CREATE TABLE admin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  department VARCHAR(80),
  CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE laboratories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT NOT NULL UNIQUE,
  lab_name VARCHAR(160),
  address VARCHAR(255),
  CONSTRAINT fk_labs_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_labs_address (address)
);

CREATE TABLE pharmacies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT NOT NULL UNIQUE,
  license_number VARCHAR(100),
  address VARCHAR(255),
  CONSTRAINT fk_pharmacies_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE appointment_slots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  doctor_id BIGINT NOT NULL,
  slot_start DATETIME NOT NULL,
  slot_end DATETIME NOT NULL,
  booked BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  UNIQUE KEY uk_doctor_slot (doctor_id, slot_start),
  INDEX idx_slot_available (doctor_id, booked)
);

CREATE TABLE appointments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  slot_id BIGINT UNIQUE,
  mode VARCHAR(40),
  reason VARCHAR(500),
  status ENUM('PENDING','BOOKED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'BOOKED',
  meeting_link VARCHAR(255),
  CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_appt_slot FOREIGN KEY (slot_id) REFERENCES appointment_slots(id)
);

CREATE TABLE lab_tests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  laboratory_id BIGINT NOT NULL,
  test_name VARCHAR(140),
  price DECIMAL(10,2),
  instructions VARCHAR(500),
  CONSTRAINT fk_tests_lab FOREIGN KEY (laboratory_id) REFERENCES laboratories(id)
);

CREATE TABLE lab_bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  test_id BIGINT NOT NULL,
  doctor_id BIGINT,
  sample_collector VARCHAR(120),
  status ENUM('PENDING','BOOKED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'PENDING',
  report_path VARCHAR(255),
  CONSTRAINT fk_labbooking_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_labbooking_test FOREIGN KEY (test_id) REFERENCES lab_tests(id),
  CONSTRAINT fk_labbooking_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE prescriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT,
  file_path VARCHAR(255),
  notes VARCHAR(1000),
  CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_rx_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT,
  laboratory_id BIGINT,
  report_type VARCHAR(100),
  file_path VARCHAR(255),
  CONSTRAINT fk_reports_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_reports_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_reports_lab FOREIGN KEY (laboratory_id) REFERENCES laboratories(id)
);

CREATE TABLE medicines (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  pharmacy_id BIGINT NOT NULL,
  name VARCHAR(160),
  price DECIMAL(10,2),
  stock INT DEFAULT 0,
  image_path VARCHAR(255),
  CONSTRAINT fk_medicines_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(id)
);

CREATE TABLE medicine_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  pharmacy_id BIGINT,
  prescription_id BIGINT,
  total_amount DECIMAL(10,2),
  delivery_status ENUM('PRESCRIPTION_REVIEW','ORDER_PLACED','PACKED','OUT_FOR_DELIVERY','DELIVERED') DEFAULT 'PRESCRIPTION_REVIEW',
  CONSTRAINT fk_orders_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_orders_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(id),
  CONSTRAINT fk_orders_rx FOREIGN KEY (prescription_id) REFERENCES prescriptions(id)
);

CREATE TABLE smartwatch_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  systolic_bp INT,
  diastolic_bp INT,
  pulse INT,
  heart_rate INT,
  oxygen_level INT,
  sleep_hours DECIMAL(4,2),
  recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watch_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  INDEX idx_watch_patient_time (patient_id, recorded_at)
);

CREATE TABLE emergency_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT,
  alert_type VARCHAR(80),
  message VARCHAR(1000),
  sms_sent BOOLEAN DEFAULT FALSE,
  doctor_notified BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_alert_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_alert_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE chatbot_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  patient_id BIGINT NOT NULL,
  prompt VARCHAR(2000),
  response VARCHAR(4000),
  CONSTRAINT fk_chat_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT,
  reference_type VARCHAR(80),
  reference_id BIGINT,
  amount DECIMAL(10,2),
  status VARCHAR(40),
  CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id BIGINT,
  title VARCHAR(160),
  message VARCHAR(800),
  read_flag BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);
