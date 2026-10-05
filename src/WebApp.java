import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class WebApp {

    // =====================================================
    // DATABASE DETAILS & CONFIGURATION
    // =====================================================

    static final String DB_URL = getDatabaseUrl();

    static final String DB_USER =
            System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";

    static final String DB_PASSWORD =
            System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "Digital@5588";

    static final int PORT = getPort();

    static String getDatabaseUrl() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.isEmpty()) {
            return dbUrl;
        }
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            if (databaseUrl.startsWith("postgres://")) {
                return "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
            } else if (databaseUrl.startsWith("postgresql://")) {
                return "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
            } else if (!databaseUrl.startsWith("jdbc:")) {
                return "jdbc:" + databaseUrl;
            }
            return databaseUrl;
        }
        return "jdbc:postgresql://localhost:5432/school_management";
    }

    static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }


    // =====================================================
    // MAIN
    // =====================================================

    public static void main(String[] args) throws Exception {

        createTables();

        int port = PORT;
        HttpServer server = null;
        for (int i = 0; i < 50; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (java.net.BindException e) {
                System.out.println("⚠️  Port " + port + " is currently in use. Trying port " + (port + 1) + "...");
                port++;
            }
        }

        if (server == null) {
            throw new RuntimeException("Could not bind HttpServer to an available port.");
        }

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        server.createContext("/", WebApp::dashboard);
        server.createContext("/students", WebApp::students);
        server.createContext("/teachers", WebApp::teachers);
        server.createContext("/subjects", WebApp::subjects);

        server.start();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("     🏫 SCHOOL MANAGEMENT SYSTEM IS RUNNING");
        System.out.println("     Local URL: http://localhost:" + port);
        System.out.println("=================================================");
        System.out.println();
    }


    // =====================================================
    // DATABASE CONNECTION
    // =====================================================

    static Connection connect() throws SQLException {

        return DriverManager.getConnection(
                DB_URL,
                DB_USER,
                DB_PASSWORD
        );
    }


    // =====================================================
    // CREATE TABLES
    // =====================================================

    static void createTables() {

        try (
                Connection conn = connect();
                Statement st = conn.createStatement()
        ) {

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS students (" +
                    "student_id INTEGER PRIMARY KEY, " +
                    "student_name VARCHAR(100) NOT NULL, " +
                    "age INTEGER, " +
                    "class VARCHAR(50), " +
                    "phone VARCHAR(20)" +
                    ")"
            );


            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS teachers (" +
                    "teacher_id INTEGER PRIMARY KEY, " +
                    "teacher_name VARCHAR(100) NOT NULL, " +
                    "subject VARCHAR(100), " +
                    "age INTEGER, " +
                    "phone VARCHAR(20)" +
                    ")"
            );


            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS subjects (" +
                    "subject_id INTEGER PRIMARY KEY, " +
                    "subject_name VARCHAR(100) NOT NULL, " +
                    "teacher_name VARCHAR(100), " +
                    "class VARCHAR(50)" +
                    ")"
            );


            System.out.println(
                    "Database tables checked successfully."
            );

        } catch (Exception e) {

            System.out.println(
                    "DATABASE ERROR:"
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // DASHBOARD
    // =====================================================

    static void dashboard(
            HttpExchange exchange) throws IOException {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<title>School Management System</title>" +

                "<style>" +

                "body {" +
                "margin: 0;" +
                "font-family: Arial, sans-serif;" +
                "background: #f4f6f8;" +
                "}" +

                ".header {" +
                "background: #1f2937;" +
                "color: white;" +
                "padding: 35px;" +
                "text-align: center;" +
                "}" +

                ".header h1 {" +
                "margin: 0;" +
                "}" +

                ".container {" +
                "width: 90%;" +
                "max-width: 1100px;" +
                "margin: 50px auto;" +
                "}" +

                ".cards {" +
                "display: flex;" +
                "justify-content: center;" +
                "gap: 25px;" +
                "flex-wrap: wrap;" +
                "}" +

                ".card {" +
                "background: white;" +
                "width: 280px;" +
                "padding: 30px;" +
                "text-align: center;" +
                "border-radius: 15px;" +
                "box-shadow: 0 4px 15px #ccc;" +
                "}" +

                ".card h2 {" +
                "margin-top: 0;" +
                "}" +

                ".card p {" +
                "color: #666;" +
                "min-height: 40px;" +
                "}" +

                ".btn {" +
                "display: inline-block;" +
                "padding: 13px 22px;" +
                "background: #2563eb;" +
                "color: white;" +
                "text-decoration: none;" +
                "border-radius: 7px;" +
                "margin-top: 15px;" +
                "}" +

                ".btn:hover {" +
                "background: #1d4ed8;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='header'>" +

                "<h1>🏫 SCHOOL MANAGEMENT SYSTEM</h1>" +

                "<p>Java + PostgreSQL Web Application</p>" +

                "</div>" +

                "<div class='container'>" +

                "<div class='cards'>" +

                "<div class='card'>" +

                "<h2>👨‍🎓 Students</h2>" +

                "<p>Add, view, update and delete student records.</p>" +

                "<a class='btn' href='/students'>" +
                "Student Management" +
                "</a>" +

                "</div>" +


                "<div class='card'>" +

                "<h2>👩‍🏫 Teachers</h2>" +

                "<p>Add, view, update and delete teacher records.</p>" +

                "<a class='btn' href='/teachers'>" +
                "Teacher Management" +
                "</a>" +

                "</div>" +


                "<div class='card'>" +

                "<h2>📚 Subjects</h2>" +

                "<p>Add, view, update and delete subject records.</p>" +

                "<a class='btn' href='/subjects'>" +
                "Subject Management" +
                "</a>" +

                "</div>" +

                "</div>" +

                "</div>" +

                "</body>" +
                "</html>";


        send(exchange, html);
    }


    // =====================================================
    // STUDENTS
    // =====================================================

    static void students(
            HttpExchange exchange) throws IOException {

        try {

            // ---------------- POST ----------------

            if (exchange.getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                Map<String, String> data =
                        readForm(exchange);

                String action =
                        data.get("action");


                // ADD STUDENT

                if ("add".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "INSERT INTO students " +
                                            "(student_id, student_name, age, class, phone) " +
                                            "VALUES (?, ?, ?, ?, ?)"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("student_id")
                                )
                        );

                        ps.setString(
                                2,
                                data.get("student_name")
                        );

                        ps.setInt(
                                3,
                                Integer.parseInt(
                                        data.get("age")
                                )
                        );

                        ps.setString(
                                4,
                                data.get("class")
                        );

                        ps.setString(
                                5,
                                data.get("phone")
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/students"
                    );

                    return;
                }


                // UPDATE STUDENT

                if ("update".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "UPDATE students " +
                                            "SET student_name=?, " +
                                            "age=?, " +
                                            "class=?, " +
                                            "phone=? " +
                                            "WHERE student_id=?"
                                    )
                    ) {

                        ps.setString(
                                1,
                                data.get("student_name")
                        );

                        ps.setInt(
                                2,
                                Integer.parseInt(
                                        data.get("age")
                                )
                        );

                        ps.setString(
                                3,
                                data.get("class")
                        );

                        ps.setString(
                                4,
                                data.get("phone")
                        );

                        ps.setInt(
                                5,
                                Integer.parseInt(
                                        data.get("student_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/students"
                    );

                    return;
                }


                // DELETE STUDENT

                if ("delete".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "DELETE FROM students " +
                                            "WHERE student_id=?"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("student_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/students"
                    );

                    return;
                }
            }


            // =================================================
            // DISPLAY STUDENTS
            // =================================================

            StringBuilder rows =
                    new StringBuilder();


            try (
                    Connection conn = connect();
                    Statement st =
                            conn.createStatement();

                    ResultSet rs =
                            st.executeQuery(
                                    "SELECT student_id, " +
                                    "student_name, age, class, phone " +
                                    "FROM students " +
                                    "ORDER BY student_id"
                            )
            ) {

                while (rs.next()) {

                    String id =
                            rs.getString("student_id");

                    String name =
                            rs.getString("student_name");

                    String age =
                            rs.getString("age");

                    String studentClass =
                            rs.getString("class");

                    String phone =
                            rs.getString("phone");


                    rows.append("<tr>");

                    rows.append("<td>")
                            .append(escape(id))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(name))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(age))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(studentClass))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(phone))
                            .append("</td>");

                    rows.append("<td>");

                    rows.append(
                            "<form method='post'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='action' " +
                            "value='delete'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='student_id' " +
                            "value='"
                    );

                    rows.append(
                            escape(id)
                    );

                    rows.append("'>");

                    rows.append(
                            "<button type='submit' " +
                            "onclick=\"return confirm('Delete this student?')\">" +
                            "Delete" +
                            "</button>"
                    );

                    rows.append("</form>");

                    rows.append("</td>");

                    rows.append("</tr>");
                }
            }


            send(
                    exchange,
                    studentPage(
                            rows.toString()
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();

            send(
                    exchange,
                    errorPage(e)
            );
        }
    }


    // =====================================================
    // STUDENT PAGE
    // =====================================================

    static String studentPage(
            String rows) {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +

                "<head>" +

                "<title>Student Management</title>" +

                "<style>" +

                "body {" +
                "font-family: Arial;" +
                "background: #f4f6f8;" +
                "margin: 0;" +
                "}" +

                ".container {" +
                "width: 95%;" +
                "max-width: 1200px;" +
                "margin: 30px auto;" +
                "}" +

                ".back {" +
                "display: inline-block;" +
                "background: #555;" +
                "color: white;" +
                "padding: 10px 18px;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "}" +

                ".box {" +
                "background: white;" +
                "padding: 25px;" +
                "margin: 20px 0;" +
                "border-radius: 12px;" +
                "box-shadow: 0 3px 12px #ddd;" +
                "}" +

                "input {" +
                "padding: 11px;" +
                "margin: 5px;" +
                "border: 1px solid #ccc;" +
                "border-radius: 5px;" +
                "}" +

                "button {" +
                "padding: 11px 18px;" +
                "background: #2563eb;" +
                "color: white;" +
                "border: none;" +
                "border-radius: 5px;" +
                "cursor: pointer;" +
                "}" +

                "table {" +
                "width: 100%;" +
                "border-collapse: collapse;" +
                "}" +

                "th, td {" +
                "padding: 12px;" +
                "border: 1px solid #ddd;" +
                "text-align: center;" +
                "}" +

                "th {" +
                "background: #1f2937;" +
                "color: white;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='container'>" +

                "<a class='back' href='/'>" +
                "← Dashboard" +
                "</a>" +

                "<h1>👨‍🎓 Student Management</h1>" +


                "<div class='box'>" +

                "<h2>Add Student</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='add'>" +

                "<input type='number' " +
                "name='student_id' " +
                "placeholder='Student ID' required>" +

                "<input type='text' " +
                "name='student_name' " +
                "placeholder='Student Name' required>" +

                "<input type='number' " +
                "name='age' " +
                "placeholder='Age' required>" +

                "<input type='text' " +
                "name='class' " +
                "placeholder='Class' required>" +

                "<input type='text' " +
                "name='phone' " +
                "placeholder='Phone' required>" +

                "<button type='submit'>" +
                "Add Student" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>Update Student</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='update'>" +

                "<input type='number' " +
                "name='student_id' " +
                "placeholder='Student ID' required>" +

                "<input type='text' " +
                "name='student_name' " +
                "placeholder='New Name' required>" +

                "<input type='number' " +
                "name='age' " +
                "placeholder='New Age' required>" +

                "<input type='text' " +
                "name='class' " +
                "placeholder='New Class' required>" +

                "<input type='text' " +
                "name='phone' " +
                "placeholder='New Phone' required>" +

                "<button type='submit'>" +
                "Update Student" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>All Students</h2>" +

                "<table>" +

                "<tr>" +
                "<th>ID</th>" +
                "<th>Name</th>" +
                "<th>Age</th>" +
                "<th>Class</th>" +
                "<th>Phone</th>" +
                "<th>Action</th>" +
                "</tr>" +

                rows +

                "</table>" +

                "</div>" +

                "</div>" +

                "</body>" +

                "</html>";


        return html;
    }


    // =====================================================
    // TEACHERS
    // =====================================================

    static void teachers(
            HttpExchange exchange) throws IOException {

        try {

            if (exchange.getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                Map<String, String> data =
                        readForm(exchange);

                String action =
                        data.get("action");


                // ADD TEACHER

                if ("add".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "INSERT INTO teachers " +
                                            "(teacher_id, teacher_name, subject, age, phone) " +
                                            "VALUES (?, ?, ?, ?, ?)"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("teacher_id")
                                )
                        );

                        ps.setString(
                                2,
                                data.get("teacher_name")
                        );

                        ps.setString(
                                3,
                                data.get("subject")
                        );

                        ps.setInt(
                                4,
                                Integer.parseInt(
                                        data.get("age")
                                )
                        );

                        ps.setString(
                                5,
                                data.get("phone")
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/teachers"
                    );

                    return;
                }


                // UPDATE TEACHER

                if ("update".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "UPDATE teachers " +
                                            "SET teacher_name=?, " +
                                            "subject=?, " +
                                            "age=?, " +
                                            "phone=? " +
                                            "WHERE teacher_id=?"
                                    )
                    ) {

                        ps.setString(
                                1,
                                data.get("teacher_name")
                        );

                        ps.setString(
                                2,
                                data.get("subject")
                        );

                        ps.setInt(
                                3,
                                Integer.parseInt(
                                        data.get("age")
                                )
                        );

                        ps.setString(
                                4,
                                data.get("phone")
                        );

                        ps.setInt(
                                5,
                                Integer.parseInt(
                                        data.get("teacher_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/teachers"
                    );

                    return;
                }


                // DELETE TEACHER

                if ("delete".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "DELETE FROM teachers " +
                                            "WHERE teacher_id=?"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("teacher_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/teachers"
                    );

                    return;
                }
            }


            StringBuilder rows =
                    new StringBuilder();


            try (
                    Connection conn = connect();
                    Statement st =
                            conn.createStatement();

                    ResultSet rs =
                            st.executeQuery(
                                    "SELECT teacher_id, " +
                                    "teacher_name, subject, age, phone " +
                                    "FROM teachers " +
                                    "ORDER BY teacher_id"
                            )
            ) {

                while (rs.next()) {

                    String id =
                            rs.getString("teacher_id");

                    String name =
                            rs.getString("teacher_name");

                    String subject =
                            rs.getString("subject");

                    String age =
                            rs.getString("age");

                    String phone =
                            rs.getString("phone");


                    rows.append("<tr>");

                    rows.append("<td>")
                            .append(escape(id))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(name))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(subject))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(age))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(phone))
                            .append("</td>");

                    rows.append("<td>");

                    rows.append(
                            "<form method='post'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='action' " +
                            "value='delete'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='teacher_id' " +
                            "value='"
                    );

                    rows.append(
                            escape(id)
                    );

                    rows.append("'>");

                    rows.append(
                            "<button type='submit' " +
                            "onclick=\"return confirm('Delete this teacher?')\">" +
                            "Delete" +
                            "</button>"
                    );

                    rows.append("</form>");

                    rows.append("</td>");

                    rows.append("</tr>");
                }
            }


            send(
                    exchange,
                    teacherPage(
                            rows.toString()
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();

            send(
                    exchange,
                    errorPage(e)
            );
        }
    }


    // =====================================================
    // TEACHER PAGE
    // =====================================================

    static String teacherPage(
            String rows) {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +

                "<head>" +

                "<title>Teacher Management</title>" +

                "<style>" +

                "body {" +
                "font-family: Arial;" +
                "background: #f4f6f8;" +
                "margin: 0;" +
                "}" +

                ".container {" +
                "width: 95%;" +
                "max-width: 1200px;" +
                "margin: 30px auto;" +
                "}" +

                ".back {" +
                "display: inline-block;" +
                "background: #555;" +
                "color: white;" +
                "padding: 10px 18px;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "}" +

                ".box {" +
                "background: white;" +
                "padding: 25px;" +
                "margin: 20px 0;" +
                "border-radius: 12px;" +
                "box-shadow: 0 3px 12px #ddd;" +
                "}" +

                "input {" +
                "padding: 11px;" +
                "margin: 5px;" +
                "border: 1px solid #ccc;" +
                "border-radius: 5px;" +
                "}" +

                "button {" +
                "padding: 11px 18px;" +
                "background: #2563eb;" +
                "color: white;" +
                "border: none;" +
                "border-radius: 5px;" +
                "cursor: pointer;" +
                "}" +

                "table {" +
                "width: 100%;" +
                "border-collapse: collapse;" +
                "}" +

                "th, td {" +
                "padding: 12px;" +
                "border: 1px solid #ddd;" +
                "text-align: center;" +
                "}" +

                "th {" +
                "background: #1f2937;" +
                "color: white;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='container'>" +

                "<a class='back' href='/'>" +
                "← Dashboard" +
                "</a>" +

                "<h1>👩‍🏫 Teacher Management</h1>" +


                "<div class='box'>" +

                "<h2>Add Teacher</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='add'>" +

                "<input type='number' " +
                "name='teacher_id' " +
                "placeholder='Teacher ID' required>" +

                "<input type='text' " +
                "name='teacher_name' " +
                "placeholder='Teacher Name' required>" +

                "<input type='text' " +
                "name='subject' " +
                "placeholder='Subject' required>" +

                "<input type='number' " +
                "name='age' " +
                "placeholder='Age' required>" +

                "<input type='text' " +
                "name='phone' " +
                "placeholder='Phone' required>" +

                "<button type='submit'>" +
                "Add Teacher" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>Update Teacher</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='update'>" +

                "<input type='number' " +
                "name='teacher_id' " +
                "placeholder='Teacher ID' required>" +

                "<input type='text' " +
                "name='teacher_name' " +
                "placeholder='New Name' required>" +

                "<input type='text' " +
                "name='subject' " +
                "placeholder='New Subject' required>" +

                "<input type='number' " +
                "name='age' " +
                "placeholder='New Age' required>" +

                "<input type='text' " +
                "name='phone' " +
                "placeholder='New Phone' required>" +

                "<button type='submit'>" +
                "Update Teacher" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>All Teachers</h2>" +

                "<table>" +

                "<tr>" +
                "<th>ID</th>" +
                "<th>Name</th>" +
                "<th>Subject</th>" +
                "<th>Age</th>" +
                "<th>Phone</th>" +
                "<th>Action</th>" +
                "</tr>" +

                rows +

                "</table>" +

                "</div>" +

                "</div>" +

                "</body>" +

                "</html>";


        return html;
    }


    // =====================================================
    // SUBJECTS
    // =====================================================

    static void subjects(
            HttpExchange exchange) throws IOException {

        try {

            if (exchange.getRequestMethod()
                    .equalsIgnoreCase("POST")) {

                Map<String, String> data =
                        readForm(exchange);

                String action =
                        data.get("action");


                // ADD SUBJECT

                if ("add".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "INSERT INTO subjects " +
                                            "(subject_id, subject_name, teacher_name, class) " +
                                            "VALUES (?, ?, ?, ?)"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("subject_id")
                                )
                        );

                        ps.setString(
                                2,
                                data.get("subject_name")
                        );

                        ps.setString(
                                3,
                                data.get("teacher_name")
                        );

                        ps.setString(
                                4,
                                data.get("class")
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/subjects"
                    );

                    return;
                }


                // UPDATE SUBJECT

                if ("update".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "UPDATE subjects " +
                                            "SET subject_name=?, " +
                                            "teacher_name=?, " +
                                            "class=? " +
                                            "WHERE subject_id=?"
                                    )
                    ) {

                        ps.setString(
                                1,
                                data.get("subject_name")
                        );

                        ps.setString(
                                2,
                                data.get("teacher_name")
                        );

                        ps.setString(
                                3,
                                data.get("class")
                        );

                        ps.setInt(
                                4,
                                Integer.parseInt(
                                        data.get("subject_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/subjects"
                    );

                    return;
                }


                // DELETE SUBJECT

                if ("delete".equals(action)) {

                    try (
                            Connection conn = connect();
                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "DELETE FROM subjects " +
                                            "WHERE subject_id=?"
                                    )
                    ) {

                        ps.setInt(
                                1,
                                Integer.parseInt(
                                        data.get("subject_id")
                                )
                        );

                        ps.executeUpdate();
                    }

                    redirect(
                            exchange,
                            "/subjects"
                    );

                    return;
                }
            }


            StringBuilder rows =
                    new StringBuilder();


            try (
                    Connection conn = connect();
                    Statement st =
                            conn.createStatement();

                    ResultSet rs =
                            st.executeQuery(
                                    "SELECT subject_id, " +
                                    "subject_name, teacher_name, class " +
                                    "FROM subjects " +
                                    "ORDER BY subject_id"
                            )
            ) {

                while (rs.next()) {

                    String id =
                            rs.getString("subject_id");

                    String name =
                            rs.getString("subject_name");

                    String teacher =
                            rs.getString("teacher_name");

                    String studentClass =
                            rs.getString("class");


                    rows.append("<tr>");

                    rows.append("<td>")
                            .append(escape(id))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(name))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(teacher))
                            .append("</td>");

                    rows.append("<td>")
                            .append(escape(studentClass))
                            .append("</td>");

                    rows.append("<td>");

                    rows.append(
                            "<form method='post'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='action' " +
                            "value='delete'>"
                    );

                    rows.append(
                            "<input type='hidden' " +
                            "name='subject_id' " +
                            "value='"
                    );

                    rows.append(
                            escape(id)
                    );

                    rows.append("'>");

                    rows.append(
                            "<button type='submit' " +
                            "onclick=\"return confirm('Delete this subject?')\">" +
                            "Delete" +
                            "</button>"
                    );

                    rows.append("</form>");

                    rows.append("</td>");

                    rows.append("</tr>");
                }
            }


            send(
                    exchange,
                    subjectPage(
                            rows.toString()
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();

            send(
                    exchange,
                    errorPage(e)
            );
        }
    }


    // =====================================================
    // SUBJECT PAGE
    // =====================================================

    static String subjectPage(
            String rows) {

        String html =
                "<!DOCTYPE html>" +
                "<html>" +

                "<head>" +

                "<title>Subject Management</title>" +

                "<style>" +

                "body {" +
                "font-family: Arial;" +
                "background: #f4f6f8;" +
                "margin: 0;" +
                "}" +

                ".container {" +
                "width: 95%;" +
                "max-width: 1200px;" +
                "margin: 30px auto;" +
                "}" +

                ".back {" +
                "display: inline-block;" +
                "background: #555;" +
                "color: white;" +
                "padding: 10px 18px;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "}" +

                ".box {" +
                "background: white;" +
                "padding: 25px;" +
                "margin: 20px 0;" +
                "border-radius: 12px;" +
                "box-shadow: 0 3px 12px #ddd;" +
                "}" +

                "input {" +
                "padding: 11px;" +
                "margin: 5px;" +
                "border: 1px solid #ccc;" +
                "border-radius: 5px;" +
                "}" +

                "button {" +
                "padding: 11px 18px;" +
                "background: #2563eb;" +
                "color: white;" +
                "border: none;" +
                "border-radius: 5px;" +
                "cursor: pointer;" +
                "}" +

                "table {" +
                "width: 100%;" +
                "border-collapse: collapse;" +
                "}" +

                "th, td {" +
                "padding: 12px;" +
                "border: 1px solid #ddd;" +
                "text-align: center;" +
                "}" +

                "th {" +
                "background: #1f2937;" +
                "color: white;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='container'>" +

                "<a class='back' href='/'>" +
                "← Dashboard" +
                "</a>" +

                "<h1>📚 Subject Management</h1>" +


                "<div class='box'>" +

                "<h2>Add Subject</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='add'>" +

                "<input type='number' " +
                "name='subject_id' " +
                "placeholder='Subject ID' required>" +

                "<input type='text' " +
                "name='subject_name' " +
                "placeholder='Subject Name' required>" +

                "<input type='text' " +
                "name='teacher_name' " +
                "placeholder='Teacher Name' required>" +

                "<input type='text' " +
                "name='class' " +
                "placeholder='Class' required>" +

                "<button type='submit'>" +
                "Add Subject" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>Update Subject</h2>" +

                "<form method='post'>" +

                "<input type='hidden' " +
                "name='action' value='update'>" +

                "<input type='number' " +
                "name='subject_id' " +
                "placeholder='Subject ID' required>" +

                "<input type='text' " +
                "name='subject_name' " +
                "placeholder='New Subject Name' required>" +

                "<input type='text' " +
                "name='teacher_name' " +
                "placeholder='New Teacher Name' required>" +

                "<input type='text' " +
                "name='class' " +
                "placeholder='New Class' required>" +

                "<button type='submit'>" +
                "Update Subject" +
                "</button>" +

                "</form>" +

                "</div>" +


                "<div class='box'>" +

                "<h2>All Subjects</h2>" +

                "<table>" +

                "<tr>" +
                "<th>ID</th>" +
                "<th>Subject</th>" +
                "<th>Teacher</th>" +
                "<th>Class</th>" +
                "<th>Action</th>" +
                "</tr>" +

                rows +

                "</table>" +

                "</div>" +

                "</div>" +

                "</body>" +

                "</html>";


        return html;
    }


    // =====================================================
    // READ FORM DATA
    // =====================================================

    static Map<String, String> readForm(
            HttpExchange exchange)
            throws IOException {

        String body =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        Map<String, String> data =
                new HashMap<>();


        if (body.isEmpty()) {
            return data;
        }


        String[] pairs =
                body.split("&");


        for (String pair : pairs) {

            String[] parts =
                    pair.split("=", 2);


            String key =
                    URLDecoder.decode(
                            parts[0],
                            StandardCharsets.UTF_8
                    );


            String value = "";


            if (parts.length > 1) {

                value =
                        URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );
            }


            data.put(
                    key,
                    value
            );
        }


        return data;
    }


    // =====================================================
    // SEND HTML
    // =====================================================

    static void send(
            HttpExchange exchange,
            String html)
            throws IOException {

        byte[] data =
                html.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                200,
                data.length
        );


        try (
                OutputStream out =
                        exchange.getResponseBody()
        ) {

            out.write(data);
        }
    }


    // =====================================================
    // REDIRECT
    // =====================================================

    static void redirect(
            HttpExchange exchange,
            String location)
            throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Location",
                        location
                );


        exchange.sendResponseHeaders(
                303,
                -1
        );


        exchange.close();
    }


    // =====================================================
    // HTML ESCAPE
    // =====================================================

    static String escape(
            String text) {

        if (text == null) {
            return "";
        }


        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =====================================================
    // ERROR PAGE
    // =====================================================

    static String errorPage(
            Exception e) {

        String message =
                e.getMessage();


        if (message == null) {

            message =
                    e.getClass()
                            .getSimpleName();
        }


        String html =
                "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<title>Error</title>" +

                "<style>" +

                "body {" +
                "font-family: Arial;" +
                "background: #f4f6f8;" +
                "text-align: center;" +
                "padding: 60px;" +
                "}" +

                ".error {" +
                "background: white;" +
                "max-width: 800px;" +
                "margin: auto;" +
                "padding: 30px;" +
                "border-radius: 12px;" +
                "box-shadow: 0 3px 12px #ddd;" +
                "}" +

                "h1 {" +
                "color: #dc2626;" +
                "}" +

                ".back {" +
                "display: inline-block;" +
                "margin-top: 20px;" +
                "padding: 10px 20px;" +
                "background: #2563eb;" +
                "color: white;" +
                "text-decoration: none;" +
                "border-radius: 5px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='error'>" +

                "<h1>Something went wrong</h1>" +

                "<p>" +
                escape(message) +
                "</p>" +

                "<a class='back' href='/'>" +
                "Back to Dashboard" +
                "</a>" +

                "</div>" +

                "</body>" +

                "</html>";


        return html;
    }
}