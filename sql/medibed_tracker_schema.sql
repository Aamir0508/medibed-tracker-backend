-- =====================================================================
-- MediBed Tracker - MySQL schema, seed data and sample queries
-- Team Pikachu | SE-IT | Mumbai University
-- Run in MySQL Workbench (File > Open SQL Script > Run) or:  mysql -u root -p < medibed_tracker_schema.sql
-- Requires MySQL 8.0.16+ (CHECK constraints are enforced from that version).
-- Section 1: DDL  |  Section 2: seed data  |  Section 3: DML / sample queries
-- =====================================================================

CREATE DATABASE IF NOT EXISTS medibed_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medibed_db;

-- Uncomment to rebuild from scratch (this deletes all data):
-- DROP TABLE IF EXISTS bed_snapshots, notifications, ambulance_requests, appointments,
--                      admin_users, doctors, hospital_facilities, hospitals;

-- =====================================================================
-- SECTION 1 : DDL  (one hospital -> many rows in every child table)
-- =====================================================================

CREATE TABLE hospitals (
    id              INT           PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(150)  NOT NULL,
    area            VARCHAR(80)   NOT NULL,
    address         VARCHAR(255)  NOT NULL,
    phone           VARCHAR(30)   NOT NULL,
    emergency_phone VARCHAR(30),
    type            ENUM('Government','Private') NOT NULL,
    rating          DECIMAL(2,1)  NOT NULL DEFAULT 0.0,
    preferred       BOOLEAN       NOT NULL DEFAULT FALSE,
    photo_url       VARCHAR(255),
    general_beds    INT           NOT NULL DEFAULT 0,
    icu_beds        INT           NOT NULL DEFAULT 0,
    oxygen_beds     INT           NOT NULL DEFAULT 0,
    nicu_beds       INT           NOT NULL DEFAULT 0,
    emergency_beds  INT           NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_hospital_beds CHECK (general_beds >= 0 AND icu_beds >= 0 AND oxygen_beds >= 0
                                        AND nicu_beds >= 0 AND emergency_beds >= 0),
    CONSTRAINT chk_hospital_rating CHECK (rating BETWEEN 0 AND 5),
    INDEX idx_hospitals_area (area),
    INDEX idx_hospitals_type (type)
);

CREATE TABLE hospital_facilities (
    hospital_id INT          NOT NULL,
    facility    VARCHAR(100) NOT NULL,
    CONSTRAINT fk_facility_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_facility_hospital (hospital_id)
);

CREATE TABLE doctors (
    id               INT          PRIMARY KEY AUTO_INCREMENT,
    hospital_id      INT          NOT NULL,
    name             VARCHAR(120) NOT NULL,
    specialization   VARCHAR(80)  NOT NULL,
    qualification    VARCHAR(150),
    languages        VARCHAR(120),
    bio              VARCHAR(300),
    department       VARCHAR(100) NOT NULL,
    room_number      VARCHAR(50),
    available_from   TIME         NOT NULL,
    available_to     TIME         NOT NULL,
    status           ENUM('AVAILABLE','BUSY','ON_LEAVE') NOT NULL DEFAULT 'AVAILABLE',
    queue_count      INT          NOT NULL DEFAULT 0,
    experience_years INT          NOT NULL DEFAULT 0,
    rating           DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    review_count     INT          NOT NULL DEFAULT 0,
    emergency_ready  BOOLEAN      NOT NULL DEFAULT FALSE,
    photo_url        VARCHAR(255),
    CONSTRAINT fk_doctor_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    CONSTRAINT chk_doctor_rating CHECK (rating BETWEEN 0 AND 5),
    CONSTRAINT chk_doctor_queue CHECK (queue_count >= 0),
    INDEX idx_doctors_hospital_status (hospital_id, status),
    INDEX idx_doctors_specialization (specialization)
);

CREATE TABLE admin_users (
    id            INT          PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(40)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,          -- BCrypt hash, never plain text
    role          ENUM('HOSPITAL_ADMIN','SUPER_ADMIN') NOT NULL,
    hospital_id   INT          NULL,              -- NULL for SUPER_ADMIN
    last_login    DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
);

CREATE TABLE appointments (
    id            INT          PRIMARY KEY AUTO_INCREMENT,
    patient_name  VARCHAR(100) NOT NULL,
    patient_phone VARCHAR(30)  NOT NULL,
    doctor_id     INT          NOT NULL,
    hospital_id   INT          NOT NULL,
    slot_time     DATETIME     NOT NULL,
    status        ENUM('BOOKED','RESCHEDULED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'BOOKED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_doctor   FOREIGN KEY (doctor_id)   REFERENCES doctors(id)   ON DELETE CASCADE,
    CONSTRAINT fk_appt_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_appt_doctor_slot (doctor_id, slot_time),
    INDEX idx_appt_hospital (hospital_id)
);

CREATE TABLE ambulance_requests (
    id              INT          PRIMARY KEY AUTO_INCREMENT,
    patient_name    VARCHAR(100) NOT NULL,
    contact_phone   VARCHAR(30),
    pickup_location VARCHAR(255) NOT NULL,
    hospital_id     INT          NOT NULL,
    status          ENUM('REQUESTED','DISPATCHED','EN_ROUTE','ARRIVED','CANCELLED') NOT NULL DEFAULT 'REQUESTED',
    eta_minutes     INT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at   DATETIME,
    CONSTRAINT fk_ambulance_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_ambulance_hospital (hospital_id)
);

CREATE TABLE notifications (
    id             INT          PRIMARY KEY AUTO_INCREMENT,
    recipient_type ENUM('STAFF','PATIENT','REQUESTER') NOT NULL,
    message        VARCHAR(255) NOT NULL,
    hospital_id    INT          NOT NULL,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_notification_hospital (hospital_id, is_read)
);

-- Supporting table (not one of the six core entities): one row is written every time a
-- hospital's bed counts change, so /api/analytics/hospital/{id}/occupancy can draw a trend.
CREATE TABLE bed_snapshots (
    id             INT      PRIMARY KEY AUTO_INCREMENT,
    hospital_id    INT      NOT NULL,
    general_beds   INT      NOT NULL,
    icu_beds       INT      NOT NULL,
    oxygen_beds    INT      NOT NULL,
    nicu_beds      INT      NOT NULL,
    emergency_beds INT      NOT NULL,
    recorded_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshot_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_snapshot_hospital_time (hospital_id, recorded_at)
);

-- ALTER example (DDL): add a column later without rebuilding the table
-- ALTER TABLE doctors ADD COLUMN emergency_ready BOOLEAN DEFAULT FALSE;   -- already part of CREATE above

-- =====================================================================
-- SECTION 2 : SEED DATA  (same data the Spring Boot app loads on first run)
-- =====================================================================

INSERT INTO hospitals (id, name, area, address, phone, emergency_phone, type, rating, preferred, photo_url, general_beds, icu_beds, oxygen_beds, nicu_beds, emergency_beds) VALUES
  (1, 'Kokilaben Dhirubhai Ambani Hospital', 'Andheri West', 'Rao Saheb Achutrao Patwardhan Marg, Four Bungalows, Andheri West, Mumbai 400053', '022-4269 6969', '022-4269-6000', 'Private', 4.7, TRUE, NULL, 22, 10, 18, 6, 5),
  (2, 'Lilavati Hospital & Research Centre', 'Bandra West', 'A-791, Bandra Reclamation, Bandra West, Mumbai 400050', '022-2643 8281', '022-2675-1500', 'Private', 4.6, TRUE, NULL, 18, 9, 14, 5, 4),
  (3, 'P. D. Hinduja Hospital & Medical Research Centre', 'Mahim', 'Veer Savarkar Marg, Mahim West, Mumbai 400016', '022-2445 2575', '022-2445-2222', 'Private', 4.6, TRUE, NULL, 20, 8, 12, 4, 3),
  (4, 'Jaslok Hospital & Research Centre', 'Pedder Road', '15, Dr. G. Deshmukh Marg, Pedder Road, Mumbai 400026', '022-6657 3333', '022-6657-3040', 'Private', 4.5, TRUE, NULL, 15, 7, 10, 3, 2),
  (5, 'Breach Candy Hospital Trust', 'Breach Candy', '60-A, Bhulabhai Desai Road, Breach Candy, Mumbai 400026', '022-2367 1888', '022-2367-2888', 'Private', 4.6, TRUE, NULL, 16, 6, 9, 3, 2),
  (6, 'Dr. Balabhai Nanavati Max Super Speciality Hospital', 'Vile Parle West', 'S.V. Road, Vile Parle West, Mumbai 400056', '022-2626 7500', '022-2626-7676', 'Private', 4.5, TRUE, NULL, 25, 12, 16, 8, 5),
  (7, 'Bombay Hospital & Medical Research Centre', 'Marine Lines', '12, New Marine Lines, Mumbai 400020', '022-2206 7676', '022-2206-7373', 'Private', 4.3, FALSE, NULL, 20, 9, 13, 4, 4),
  (8, 'Wockhardt Hospital, Mumbai Central', 'Mumbai Central', '1877, Dr. Anand Rao Nair Marg, Near Agripada Police Court, Mumbai Central, Mumbai 400011', '022-6178 4444', '022-6178-4000', 'Private', 4.3, FALSE, NULL, 19, 14, 15, 5, 4),
  (9, 'Fortis Hospital, Mulund', 'Mulund West', 'Mulund Goregaon Link Road, Mulund West, Mumbai 400078', '022-6754 0000', '022-6799-4000', 'Private', 4.4, TRUE, NULL, 17, 8, 11, 3, 3),
  (10, 'Global Hospital, Parel', 'Parel', '35, Dr. E Borges Road, Parel, Mumbai 400012', '022-6767 0101', '022-6767-6700', 'Private', 4.3, FALSE, NULL, 14, 7, 9, 2, 2),
  (11, 'Sir H. N. Reliance Foundation Hospital', 'Girgaon', 'Raja Rammohan Roy Road, Girgaon, Mumbai 400004', '022-6145 6789', '022-3667-7777', 'Private', 4.4, FALSE, NULL, 16, 8, 10, 3, 3),
  (12, 'Tata Memorial Hospital', 'Parel', 'Dr. E Borges Road, Parel, Mumbai 400012', '022-2417 7000', '022-2417-7300', 'Government', 4.8, TRUE, NULL, 12, 5, 8, 0, 2),
  (13, 'King Edward Memorial (KEM) Hospital', 'Parel', 'Acharya Donde Marg, Parel, Mumbai 400012', '022-2410 7000', '022-2410-7999', 'Government', 4.2, FALSE, NULL, 30, 10, 20, 8, 6),
  (14, 'Lokmanya Tilak Municipal General Hospital (Sion Hospital)', 'Sion West', 'Sion West, Mumbai 400022', '022-2407 6381', '022-2407-6300', 'Government', 4, FALSE, NULL, 28, 9, 18, 6, 5),
  (15, 'Sir J. J. Group of Hospitals', 'Nagpada', 'J.J. Marg, Nagpada, Mumbai 400008', '022-2373 5555', '022-2373-5000', 'Government', 4.1, FALSE, NULL, 32, 11, 22, 7, 6),
  (16, 'Dr. R. N. Cooper Municipal General Hospital', 'Vile Parle West', 'Juhu Road, Vile Parle West, Mumbai 400056', '022-2620 8585', '022-2620-8111', 'Government', 4, FALSE, NULL, 20, 6, 12, 4, 3),
  (17, 'Saifee Hospital', 'Marine Lines', '15/17, Maharshi Karve Road, Marine Lines, Mumbai 400004', '022-6757 0111', '022-6757-0000', 'Private', 4.5, TRUE, NULL, 15, 7, 9, 3, 2),
  (18, 'Holy Family Hospital', 'Bandra West', 'St. Andrew''s Road, Bandra West, Mumbai 400050', '022-5070 0700', '022-5069-9222', 'Private', 4.2, FALSE, NULL, 13, 5, 8, 3, 2),
  (19, 'Asian Heart Institute', 'Bandra Kurla Complex', 'G/N Block, Bandra Kurla Complex, Bandra East, Mumbai 400051', '022-6698 6666', '022-6698-6600', 'Private', 4.7, TRUE, NULL, 8, 10, 6, 0, 3),
  (20, 'Hiranandani Hospital, Powai', 'Powai', 'Hillside Avenue, Powai, Mumbai 400076', '022-2576 3300', '022-2576-3300', 'Private', 4.5, TRUE, NULL, 17, 8, 11, 4, 3),
  (21, 'Bhatia Hospital', 'Tardeo', 'Grant Road, Tardeo, Mumbai 400007', '022-2363 1133', '022-2363-0900', 'Private', 4.2, FALSE, NULL, 12, 5, 7, 2, 2),
  (22, 'Masina Hospital', 'Byculla', 'Sankli Street, Byculla, Mumbai 400027', '022-2372 6699', '022-2301-9000', 'Private', 4.1, FALSE, NULL, 14, 5, 8, 2, 2),
  (23, 'Seven Hills Hospital', 'Andheri East', 'Marol Maroshi Road, Andheri East, Mumbai 400059', '022-6676 0000', '022-6767-6799', 'Private', 4, FALSE, NULL, 21, 9, 13, 4, 4),
  (24, 'Surya Hospital', 'Santacruz West', 'Santacruz West, Mumbai 400054', '022-6666 7777', '022-6666-1200', 'Private', 4.2, FALSE, NULL, 10, 4, 6, 5, 2),
  (25, 'Criticare Asia Multi-Superspeciality Hospital', 'Andheri West', 'Four Bungalows, Andheri West, Mumbai 400053', '022-6291 0333', '022-2632-3200', 'Private', 4.1, FALSE, NULL, 11, 6, 8, 2, 3),
  (26, 'Rajawadi Hospital', 'Ghatkopar East', 'Rajawadi, Ghatkopar East, Mumbai 400077', '022-2501 4341', '022-2501-2000', 'Government', 3.8, FALSE, NULL, 22, 6, 14, 4, 3),
  (27, 'Topiwala National Medical College & B.Y.L. Nair Hospital', 'Mumbai Central', 'Dr. A. L. Nair Road, Mumbai Central, Mumbai 400008', '022-2307 5000', '022-2308-1400', 'Government', 3.9, FALSE, NULL, 26, 8, 17, 6, 4),
  (28, 'Bhagwati Municipal Hospital', 'Borivali West', 'S.V. Road, Borivali West, Mumbai 400092', '022-2892 4741', NULL, 'Government', 4.0, FALSE, NULL, 18, 4, 10, 3, 2);

INSERT INTO hospital_facilities (hospital_id, facility) VALUES
  (1, '24/7 Emergency & Trauma'),
  (1, 'Robotic Surgery'),
  (1, 'Organ Transplant'),
  (1, 'Oncology'),
  (1, 'Cardiology'),
  (2, '24/7 Emergency'),
  (2, 'Cardiac Care'),
  (2, 'Neurology'),
  (2, 'Oncology'),
  (2, 'Multi-specialty ICU'),
  (3, '24/7 Emergency'),
  (3, 'Nephrology'),
  (3, 'Gastroenterology'),
  (3, 'Home Care'),
  (3, 'Diagnostics'),
  (4, 'Super-specialty Care'),
  (4, 'Neurosurgery'),
  (4, 'Urology'),
  (4, 'IVF'),
  (4, 'Advanced Imaging'),
  (5, 'General Medicine'),
  (5, 'Surgery'),
  (5, 'Maternity'),
  (5, 'Diagnostics'),
  (6, '24/7 Emergency'),
  (6, 'Trauma Centre'),
  (6, 'Dialysis'),
  (6, 'NICU/PICU'),
  (6, 'Cath Lab'),
  (7, '24/7 Emergency'),
  (7, 'Multi-specialty'),
  (7, 'Diagnostics'),
  (7, 'Surgery'),
  (8, 'Large ICU capacity'),
  (8, 'Cardiac Sciences'),
  (8, 'Orthopedics'),
  (8, 'GPS-enabled Ambulances'),
  (9, '24/7 Emergency'),
  (9, 'Oncology'),
  (9, 'Orthopedics'),
  (9, 'Renal Care'),
  (10, 'Organ Transplant'),
  (10, 'Liver & Kidney Care'),
  (10, 'Critical Care'),
  (11, '24/7 Emergency'),
  (11, 'Cardiology'),
  (11, 'Oncology'),
  (11, 'Advanced Diagnostics'),
  (12, 'Cancer Care & Research'),
  (12, 'Oncology'),
  (12, 'Radiation Therapy'),
  (13, 'Teaching Hospital'),
  (13, 'Cardiology'),
  (13, 'Neurology'),
  (13, 'General Surgery'),
  (14, 'Teaching Hospital'),
  (14, 'Trauma Care'),
  (14, 'Pediatrics'),
  (14, 'Maternity'),
  (15, 'Teaching Hospital'),
  (15, 'Burns Unit'),
  (15, 'General Surgery'),
  (15, 'Ophthalmology'),
  (16, 'General Medicine'),
  (16, 'Maternity'),
  (16, 'Pediatrics'),
  (17, '24/7 Emergency'),
  (17, 'Minimal Access Surgery'),
  (17, 'Cardiology'),
  (18, 'Maternity'),
  (18, 'General Medicine'),
  (18, 'Pediatrics'),
  (19, 'Cardiac Surgery'),
  (19, 'Cath Lab'),
  (19, 'Cardiac ICU'),
  (20, '24/7 Emergency'),
  (20, 'Orthopedics'),
  (20, 'Maternity'),
  (20, 'Diagnostics'),
  (21, 'General Medicine'),
  (21, 'Surgery'),
  (21, 'Diagnostics'),
  (22, 'General Medicine'),
  (22, 'Maternity'),
  (22, 'Surgery'),
  (23, '24/7 Emergency'),
  (23, 'Multi-specialty'),
  (23, 'Trauma Care'),
  (24, 'Maternity & Neonatology'),
  (24, 'IVF'),
  (24, 'Pediatrics'),
  (25, 'Critical Care'),
  (25, 'Trauma'),
  (25, 'General Surgery'),
  (26, 'General Medicine'),
  (26, 'Maternity'),
  (26, 'Pediatrics'),
  (27, 'Teaching Hospital'),
  (27, 'General Surgery'),
  (27, 'Pediatrics'),
  (27, 'Orthopedics'),
  (28, 'General Medicine'),
  (28, 'Maternity'),
  (28, 'Emergency Care');

INSERT INTO doctors (hospital_id, name, specialization, qualification, languages, bio, department, room_number, available_from, available_to, status, queue_count, experience_years, rating, review_count, emergency_ready, photo_url) VALUES
  (1, 'Dr. Anjali Mehta', 'Emergency Medicine', 'MBBS, MD (Emergency Medicine)', 'English, Hindi, Marathi', 'Leads trauma response for critical incoming cases.', 'Emergency Ward', 'Ground Floor, Bay 2', '08:00:00', '20:00:00', 'AVAILABLE', 1, 9, 4.5, 212, TRUE, 'https://i.pravatar.cc/150?img=47'),
  (1, 'Dr. Rohan Kulkarni', 'Cardiology', 'MBBS, MD, DM (Cardiology)', 'English, Hindi, Marathi', 'Specialist in interventional cardiology and angioplasty.', 'Cardiac Care Unit', '3rd Floor, Room 312', '09:00:00', '17:00:00', 'BUSY', 4, 15, 4.8, 389, FALSE, 'https://i.pravatar.cc/150?img=12'),
  (1, 'Dr. Meera Kapadia', 'Neurosurgery', 'MBBS, MS, MCh (Neurosurgery)', 'English, Hindi, Gujarati', '20+ years in complex brain and spine surgery.', 'Neuro ICU', '5th Floor, Room 501', '09:00:00', '19:00:00', 'AVAILABLE', 0, 21, 4.9, 501, TRUE, 'https://i.pravatar.cc/150?img=32'),
  (13, 'Dr. Sunita Rao', 'General Surgery', 'MBBS, MS (General Surgery)', 'English, Hindi, Marathi', 'Handles emergency and elective surgical cases.', 'Surgical Ward', '2nd Floor, Room 208', '10:00:00', '18:00:00', 'AVAILABLE', 0, 6, 4.2, 98, TRUE, 'https://i.pravatar.cc/150?img=44'),
  (13, 'Dr. Priya Nair', 'Pediatrics', 'MBBS, MD (Pediatrics)', 'English, Hindi, Malayalam', 'Focused on newborn and child emergency care.', 'Pediatric Ward', '1st Floor, Room 110', '08:30:00', '16:30:00', 'AVAILABLE', 2, 11, 4.6, 266, TRUE, 'https://i.pravatar.cc/150?img=25'),
  (13, 'Dr. Ashok Deshmukh', 'Nephrology', 'MBBS, MD, DM (Nephrology)', 'English, Hindi, Marathi', 'Manages dialysis and kidney transplant patients.', 'Nephrology Unit', '3rd Floor, Room 305', '09:00:00', '17:00:00', 'BUSY', 5, 18, 4.5, 214, FALSE, 'https://i.pravatar.cc/150?img=53'),
  (2, 'Dr. Farhan Sheikh', 'Neurology', 'MBBS, MD, DM (Neurology)', 'English, Hindi, Urdu', 'Expert in stroke management and epilepsy care.', 'Neuro ICU', '4th Floor, Room 401', '09:00:00', '21:00:00', 'ON_LEAVE', 0, 20, 4.9, 445, FALSE, 'https://i.pravatar.cc/150?img=14'),
  (2, 'Dr. Vikram Desai', 'Orthopedics', 'MBBS, MS (Orthopedics)', 'English, Hindi, Marathi', 'Trauma and joint-replacement specialist.', 'Ortho Unit', '2nd Floor, Room 220', '09:00:00', '18:00:00', 'BUSY', 3, 13, 4.4, 176, TRUE, 'https://i.pravatar.cc/150?img=15'),
  (2, 'Dr. Kavita Iyer', 'Gynecology', 'MBBS, MD, DGO', 'English, Hindi, Tamil', 'High-risk pregnancy and emergency obstetric care.', 'Maternity Ward', '1st Floor, Room 115', '08:00:00', '20:00:00', 'AVAILABLE', 1, 14, 4.7, 298, TRUE, 'https://i.pravatar.cc/150?img=29'),
  (12, 'Dr. Arvind Rane', 'Oncology', 'MBBS, MD, DM (Oncology)', 'English, Hindi, Marathi', 'Specialist in cancer diagnosis and chemotherapy planning.', 'Oncology Ward', '6th Floor, Room 604', '09:00:00', '17:00:00', 'AVAILABLE', 2, 22, 4.9, 512, FALSE, 'https://i.pravatar.cc/150?img=52'),
  (12, 'Dr. Neha Bhatt', 'Radiology & Oncology', 'MBBS, MD (Radiotherapy)', 'English, Hindi, Gujarati', 'Leads radiation therapy planning for tumor patients.', 'Radiotherapy Unit', '2nd Floor, Room 210', '09:00:00', '18:00:00', 'AVAILABLE', 0, 16, 4.7, 203, FALSE, 'https://i.pravatar.cc/150?img=45'),
  (3, 'Dr. Imran Qureshi', 'Emergency Medicine', 'MBBS, MD (Emergency Medicine)', 'English, Hindi, Urdu', 'On-call for ambulance intake and triage.', 'Emergency Ward', 'Ground Floor, Bay 1', '00:00:00', '23:59:00', 'AVAILABLE', 0, 10, 4.6, 231, TRUE, 'https://i.pravatar.cc/150?img=51'),
  (3, 'Dr. Shalini Pillai', 'Endocrinology', 'MBBS, MD, DM (Endocrinology)', 'English, Hindi, Malayalam', 'Manages diabetes, thyroid and hormonal emergencies.', 'Endocrinology Unit', '3rd Floor, Room 318', '09:00:00', '17:00:00', 'BUSY', 6, 17, 4.5, 187, FALSE, 'https://i.pravatar.cc/150?img=41'),
  (6, 'Dr. Rajesh Trivedi', 'Cardiology', 'MBBS, MD, DM (Cardiology)', 'English, Hindi, Gujarati', 'Runs the 24x7 cardiac emergency response team.', 'Cardiac Care Unit', '4th Floor, Room 402', '08:00:00', '22:00:00', 'AVAILABLE', 1, 19, 4.8, 356, TRUE, 'https://i.pravatar.cc/150?img=60'),
  (6, 'Dr. Pooja Malhotra', 'Pulmonology', 'MBBS, MD (Pulmonary Medicine)', 'English, Hindi, Punjabi', 'Treats respiratory distress and ICU ventilator cases.', 'Respiratory ICU', '5th Floor, Room 512', '09:00:00', '19:00:00', 'AVAILABLE', 2, 12, 4.6, 174, TRUE, 'https://i.pravatar.cc/150?img=33'),
  (11, 'Dr. Siddharth Joshi', 'Gastroenterology', 'MBBS, MD, DM (Gastroenterology)', 'English, Hindi, Marathi', 'Handles liver, endoscopy and GI-bleed emergencies.', 'Gastro Unit', '3rd Floor, Room 330', '10:00:00', '18:00:00', 'BUSY', 3, 14, 4.4, 142, FALSE, 'https://i.pravatar.cc/150?img=68'),
  (11, 'Dr. Leena Fernandes', 'Dermatology', 'MBBS, MD (Dermatology)', 'English, Hindi, Konkani', 'Treats burns, infections and allergic reactions.', 'Dermatology OPD', '1st Floor, Room 105', '10:00:00', '16:00:00', 'AVAILABLE', 0, 8, 4.3, 96, FALSE, 'https://i.pravatar.cc/150?img=48'),
  (7, 'Dr. Manoj Pandey', 'General Medicine', 'MBBS, MD (Internal Medicine)', 'English, Hindi, Bhojpuri', 'First point of contact for adult emergency admissions.', 'General Medicine Ward', 'Ground Floor, Room 4', '08:00:00', '20:00:00', 'AVAILABLE', 1, 11, 4.5, 189, TRUE, 'https://i.pravatar.cc/150?img=59'),
  (7, 'Dr. Ritu Chawla', 'Anesthesiology', 'MBBS, MD (Anesthesiology)', 'English, Hindi, Punjabi', 'Coordinates emergency surgical anesthesia and pain management.', 'Operation Theatre Complex', '4th Floor, OT-2', '07:00:00', '19:00:00', 'AVAILABLE', 0, 15, 4.7, 210, TRUE, 'https://i.pravatar.cc/150?img=36'),
  (4, 'Dr. Aditi Shah', 'Cardiology', 'MBBS, MD, DM (Cardiology)', 'English, Hindi, Gujarati', 'Specializes in preventive cardiology and cardiac rehabilitation.', 'Cardiac Care Unit', '2nd Floor, Room 205', '09:00:00', '17:00:00', 'AVAILABLE', 1, 13, 4.6, 241, FALSE, 'https://i.pravatar.cc/150?img=20'),
  (4, 'Dr. Nikhil Bansal', 'Orthopedics', 'MBBS, MS (Orthopedics)', 'English, Hindi', 'Focuses on sports injuries and joint replacement surgery.', 'Ortho Unit', '3rd Floor, Room 310', '10:00:00', '18:00:00', 'BUSY', 2, 10, 4.3, 132, TRUE, 'https://i.pravatar.cc/150?img=11'),
  (5, 'Dr. Simran Kaur', 'Gynecology', 'MBBS, MD, DGO', 'English, Hindi, Punjabi', 'Handles high-risk deliveries and gynecological emergencies.', 'Maternity Ward', '1st Floor, Room 118', '08:00:00', '20:00:00', 'AVAILABLE', 0, 16, 4.8, 305, TRUE, 'https://i.pravatar.cc/150?img=31'),
  (18, 'Dr. Rakesh Menon', 'Nephrology', 'MBBS, MD, DM (Nephrology)', 'English, Hindi, Malayalam', 'Runs the dialysis unit and kidney transplant program.', 'Nephrology Unit', '2nd Floor, Room 214', '09:00:00', '17:00:00', 'AVAILABLE', 3, 18, 4.5, 198, FALSE, 'https://i.pravatar.cc/150?img=17'),
  (9, 'Dr. Tanvi Ghosh', 'Pediatrics', 'MBBS, MD (Pediatrics)', 'English, Hindi, Bengali', 'Specializes in neonatal intensive care.', 'Pediatric Ward', '1st Floor, Room 112', '08:30:00', '16:30:00', 'AVAILABLE', 1, 9, 4.6, 176, TRUE, 'https://i.pravatar.cc/150?img=24'),
  (8, 'Dr. Aamir Sheikh', 'General Surgery', 'MBBS, MS (General Surgery)', 'English, Hindi, Urdu', 'Performs laparoscopic and emergency abdominal surgeries.', 'Surgical Ward', '2nd Floor, Room 221', '09:00:00', '18:00:00', 'BUSY', 4, 14, 4.4, 167, TRUE, 'https://i.pravatar.cc/150?img=13'),
  (10, 'Dr. Neelam Verma', 'Oncology', 'MBBS, MD, DM (Oncology)', 'English, Hindi, Marathi', 'Leads chemotherapy planning for solid-tumor patients.', 'Oncology Ward', '5th Floor, Room 508', '09:00:00', '17:00:00', 'AVAILABLE', 2, 17, 4.7, 289, FALSE, 'https://i.pravatar.cc/150?img=26'),
  (17, 'Dr. Salim Contractor', 'ENT', 'MBBS, MS (ENT)', 'English, Hindi, Gujarati', 'Handles airway emergencies and sinus surgeries.', 'ENT OPD', '1st Floor, Room 103', '10:00:00', '18:00:00', 'AVAILABLE', 0, 12, 4.3, 121, TRUE, 'https://i.pravatar.cc/150?img=15'),
  (16, 'Dr. Bhavesh Patil', 'Emergency Medicine', 'MBBS, MD (Emergency Medicine)', 'English, Hindi, Marathi', 'First responder for ambulance intake and mass-casualty triage.', 'Emergency Ward', 'Ground Floor, Bay 3', '00:00:00', '23:59:00', 'AVAILABLE', 2, 8, 4.4, 154, TRUE, 'https://i.pravatar.cc/150?img=53'),
  (14, 'Dr. Snehal Gaikwad', 'General Medicine', 'MBBS, MD (Internal Medicine)', 'English, Hindi, Marathi', 'Manages adult emergency admissions and chronic illness care.', 'General Medicine Ward', 'Ground Floor, Room 6', '08:00:00', '20:00:00', 'BUSY', 5, 10, 4.2, 143, TRUE, 'https://i.pravatar.cc/150?img=39'),
  (27, 'Dr. Iqbal Ansari', 'Orthopedics', 'MBBS, MS (Orthopedics)', 'English, Hindi, Urdu', 'Specializes in trauma fractures and spinal fixation.', 'Ortho Unit', '3rd Floor, Room 322', '09:00:00', '17:00:00', 'AVAILABLE', 1, 13, 4.3, 139, TRUE, 'https://i.pravatar.cc/150?img=54'),
  (15, 'Dr. Chetan Yadav', 'Neurosurgery', 'MBBS, MS, MCh (Neurosurgery)', 'English, Hindi', 'Handles complex spine and brain tumor surgeries.', 'Neuro ICU', '4th Floor, Room 415', '09:00:00', '19:00:00', 'ON_LEAVE', 0, 19, 4.8, 267, FALSE, 'https://i.pravatar.cc/150?img=57'),
  (21, 'Dr. Ritika Sharma', 'Dermatology', 'MBBS, MD (Dermatology)', 'English, Hindi', 'Treats burns, severe allergic reactions and skin infections.', 'Dermatology OPD', '1st Floor, Room 108', '10:00:00', '16:00:00', 'AVAILABLE', 0, 7, 4.2, 89, FALSE, 'https://i.pravatar.cc/150?img=35'),
  (22, 'Dr. Faisal Merchant', 'Gastroenterology', 'MBBS, MD, DM (Gastroenterology)', 'English, Hindi, Urdu', 'Manages GI bleeds, liver disease and endoscopy cases.', 'Gastro Unit', '2nd Floor, Room 216', '09:00:00', '17:00:00', 'AVAILABLE', 1, 15, 4.5, 176, FALSE, 'https://i.pravatar.cc/150?img=16'),
  (19, 'Dr. Deepa Nadkarni', 'Cardiology', 'MBBS, MD, DM (Cardiology)', 'English, Hindi, Marathi', 'Leads the cath lab for emergency angioplasty procedures.', 'Cardiac Care Unit', '3rd Floor, Room 301', '08:00:00', '20:00:00', 'AVAILABLE', 2, 20, 4.9, 412, TRUE, 'https://i.pravatar.cc/150?img=28'),
  (25, 'Dr. Suresh Naik', 'Pulmonology', 'MBBS, MD (Pulmonary Medicine)', 'English, Hindi, Konkani', 'Manages ventilator support and respiratory emergencies.', 'Respiratory ICU', '4th Floor, Room 407', '09:00:00', '19:00:00', 'AVAILABLE', 1, 14, 4.5, 163, TRUE, 'https://i.pravatar.cc/150?img=61'),
  (24, 'Dr. Aisha Khan', 'Pediatrics', 'MBBS, MD (Pediatrics)', 'English, Hindi, Urdu', 'Focused on pediatric emergency and critical care.', 'Pediatric Ward', '1st Floor, Room 114', '08:30:00', '16:30:00', 'AVAILABLE', 0, 9, 4.5, 151, TRUE, 'https://i.pravatar.cc/150?img=23'),
  (23, 'Dr. Rohit Kamat', 'Orthopedics', 'MBBS, MS (Orthopedics)', 'English, Hindi, Marathi', 'Handles trauma cases and joint-replacement surgeries.', 'Ortho Unit', '2nd Floor, Room 225', '09:00:00', '18:00:00', 'BUSY', 3, 11, 4.3, 118, TRUE, 'https://i.pravatar.cc/150?img=62'),
  (26, 'Dr. Madhavi Joshi', 'Emergency Medicine', 'MBBS, MD (Emergency Medicine)', 'English, Hindi, Marathi', 'Coordinates round-the-clock emergency intake and triage.', 'Emergency Ward', 'Ground Floor, Bay 1', '00:00:00', '23:59:00', 'AVAILABLE', 1, 9, 4.3, 134, TRUE, 'https://i.pravatar.cc/150?img=43');

-- Starting point for the bed-trend analytics: one snapshot per hospital
INSERT INTO bed_snapshots (hospital_id, general_beds, icu_beds, oxygen_beds, nicu_beds, emergency_beds)
SELECT id, general_beds, icu_beds, oxygen_beds, nicu_beds, emergency_beds FROM hospitals;

-- NOTE: admin accounts are NOT seeded here. The Spring Boot app creates the first SUPER_ADMIN on
-- startup (username/password from application.properties) and stores a BCrypt hash.

-- =====================================================================
-- SECTION 3 : DML - the queries behind the API
-- Everything below runs inside one transaction that is ROLLED BACK at the end, so you can run
-- the whole script safely and still see every result. Change ROLLBACK to COMMIT to keep changes.
-- =====================================================================
START TRANSACTION;

-- INSERT: a hospital registers (POST /api/hospitals)
INSERT INTO hospitals (name, area, address, phone, type)
VALUES ('Demo Care Hospital', 'Andheri East', 'Sample Road, Andheri East, Mumbai 400069', '022-1234 5678', 'Private');

-- SELECT: hospitals in an area (GET /api/hospitals/area/{area})
SELECT id, name, type, general_beds, icu_beds FROM hospitals WHERE area = 'Parel';

-- SELECT: search by name (GET /api/hospitals/search?keyword=)
SELECT id, name, area FROM hospitals WHERE name LIKE '%Hospital%' ORDER BY name;

-- SELECT: who can see a patient right now at hospital 1? (GET /api/doctors/hospital/{id}/available-now)
SELECT name, specialization, room_number
FROM doctors
WHERE hospital_id = 1
  AND status = 'AVAILABLE'
  AND CURTIME() BETWEEN available_from AND available_to;

-- SELECT: top-rated doctors at a hospital (GET /api/doctors/hospital/{id}/top-rated)
SELECT name, specialization, rating, review_count
FROM doctors WHERE hospital_id = 1
ORDER BY rating DESC, review_count DESC LIMIT 3;

-- SELECT (JOIN): doctors with their hospital
SELECT d.name AS doctor, d.specialization, h.name AS hospital, h.area
FROM doctors d JOIN hospitals h ON h.id = d.hospital_id
ORDER BY h.name, d.name;

-- SELECT: hospitals that still have an ICU bed, most beds first (bed-type filter on the website)
SELECT name, area, icu_beds FROM hospitals WHERE icu_beds > 0 ORDER BY icu_beds DESC;

-- SELECT: total available beds per hospital type
SELECT type, COUNT(*) AS hospitals,
       SUM(general_beds + icu_beds + oxygen_beds + nicu_beds + emergency_beds) AS total_beds
FROM hospitals GROUP BY type;

-- UPDATE: a hospital pushes a live bed-count update (PATCH /api/hospitals/{id}/beds)
UPDATE hospitals SET icu_beds = icu_beds - 1 WHERE id = 1 AND icu_beds > 0;

-- UPDATE: toggle a doctor's status (PATCH /api/doctors/{id}/status)
UPDATE doctors SET status = 'BUSY', queue_count = queue_count + 1 WHERE id = 1;

-- INSERT + SELECT: book an appointment and list a doctor's schedule
INSERT INTO appointments (patient_name, patient_phone, doctor_id, hospital_id, slot_time)
VALUES ('Test Patient', '9876543210', 1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY));
SET @appt_id = LAST_INSERT_ID();
SELECT id, patient_name, slot_time, status FROM appointments WHERE doctor_id = 1 ORDER BY slot_time;

-- UPDATE: reschedule, then cancel (PUT .../reschedule, DELETE /api/appointments/{id})
UPDATE appointments SET slot_time = DATE_ADD(slot_time, INTERVAL 1 HOUR), status = 'RESCHEDULED' WHERE id = @appt_id;
UPDATE appointments SET status = 'CANCELLED' WHERE id = @appt_id;

-- DELETE: clear old, already-read notifications (the app also does this nightly)
DELETE FROM notifications WHERE is_read = TRUE AND created_at < NOW() - INTERVAL 30 DAY;

-- DELETE: remove the demo hospital again - ON DELETE CASCADE also removes its doctors, appointments etc.
DELETE FROM hospitals WHERE name = 'Demo Care Hospital';

ROLLBACK;   -- use COMMIT instead if you want to keep the changes above
