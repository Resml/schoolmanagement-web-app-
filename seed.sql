-- Seed sample data for School Management System

INSERT INTO students (student_id, student_name, age, class, phone) VALUES
(101, 'Aarav Sharma', 16, 'Grade 10-A', '9876543210'),
(102, 'Diya Patel', 15, 'Grade 9-B', '9876543211'),
(103, 'Rohan Mehta', 17, 'Grade 11-A', '9876543212'),
(104, 'Ananya Iyer', 16, 'Grade 10-B', '9876543213'),
(105, 'Kabir Verma', 17, 'Grade 11-A', '9876543214'),
(106, 'Ishita Sen', 15, 'Grade 9-A', '9876543215')
ON CONFLICT (student_id) DO UPDATE 
SET student_name = EXCLUDED.student_name,
    age = EXCLUDED.age,
    class = EXCLUDED.class,
    phone = EXCLUDED.phone;

INSERT INTO teachers (teacher_id, teacher_name, subject, age, phone) VALUES
(201, 'Dr. Rajesh Khanna', 'Physics', 45, '9822011223'),
(202, 'Priya Nair', 'Mathematics', 38, '9822011224'),
(203, 'Vikram Malhotra', 'Computer Science', 34, '9822011225'),
(204, 'Sunita Deshmukh', 'Chemistry', 42, '9822011226'),
(205, 'Amitabh Joshi', 'English', 50, '9822011227')
ON CONFLICT (teacher_id) DO UPDATE 
SET teacher_name = EXCLUDED.teacher_name,
    subject = EXCLUDED.subject,
    age = EXCLUDED.age,
    phone = EXCLUDED.phone;

INSERT INTO subjects (subject_id, subject_name, teacher_name, class) VALUES
(301, 'Physics', 'Dr. Rajesh Khanna', 'Grade 11-A'),
(302, 'Mathematics', 'Priya Nair', 'Grade 10-A'),
(303, 'Computer Science', 'Vikram Malhotra', 'Grade 11-A'),
(304, 'Chemistry', 'Sunita Deshmukh', 'Grade 10-B'),
(305, 'English Literature', 'Amitabh Joshi', 'Grade 9-A')
ON CONFLICT (subject_id) DO UPDATE 
SET subject_name = EXCLUDED.subject_name,
    teacher_name = EXCLUDED.teacher_name,
    class = EXCLUDED.class;
