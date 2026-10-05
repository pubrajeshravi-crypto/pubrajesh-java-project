-- =============================================================
-- Student Management System — Seed Data (25 realistic records)
-- Run after schema.sql.
-- =============================================================

USE sms_db;

INSERT INTO students (student_id, name, branch, year, phone, marks) VALUES
  ('CS2021001', 'Aarav Kumar',        'CSE',  2, '9876543210', 82.50),
  ('CS2021002', 'Priya Sharma',       'CSE',  3, '9876543211', 97.50),
  ('CS2021003', 'Rohit Verma',        'CSE',  1, '9876543212', 65.00),
  ('CS2021004', 'Sneha Patel',        'CSE',  4, '9876543213', 88.75),
  ('CS2021005', 'Kiran Reddy',        'CSE',  2, '9876543214', 73.25),
  ('EC2021001', 'Ananya Singh',       'ECE',  3, '9876543215', 91.00),
  ('EC2021002', 'Vikram Nair',        'ECE',  1, '9876543216', 55.50),
  ('EC2021003', 'Meera Iyer',         'ECE',  4, '9876543217', 78.00),
  ('EC2021004', 'Arjun Mehta',        'ECE',  2, '9876543218', 84.25),
  ('EC2021005', 'Divya Krishnan',     'ECE',  3, '9876543219', 69.75),
  ('ME2021001', 'Suresh Yadav',       'ME',   4, '9876543220', 62.00),
  ('ME2021002', 'Pooja Joshi',        'ME',   2, '9876543221', 77.50),
  ('ME2021003', 'Nikhil Gupta',       'ME',   1, '9876543222', 58.25),
  ('ME2021004', 'Lakshmi Pillai',     'ME',   3, '9876543223', 93.00),
  ('ME2021005', 'Rahul Tiwari',       'ME',   4, '9876543224', 46.75),
  ('CE2021001', 'Anjali Mishra',      'CE',   2, '9876543225', 80.00),
  ('CE2021002', 'Deepak Chauhan',     'CE',   3, '9876543226', 71.50),
  ('CE2021003', 'Kavya Nambiar',      'CE',   1, '9876543227', 89.25),
  ('CE2021004', 'Sameer Khan',        'CE',   4, '9876543228', 54.00),
  ('EE2021001', 'Ritu Agarwal',       'EE',   2, '9876543229', 76.75),
  ('EE2021002', 'Harish Bhatia',      'EE',   3, '9876543230', 67.50),
  ('EE2021003', 'Nandini Menon',      'EE',   1, '9876543231', 95.00),
  ('IT2021001', 'Tarun Saxena',       'IT',   4, '9876543232', 83.25),
  ('IT2021002', 'Simran Dhillon',     'IT',   2, '9876543233', 74.00),
  ('IT2021003', 'Aditya Banerjee',    'IT',   3, '9876543234', 61.50);
