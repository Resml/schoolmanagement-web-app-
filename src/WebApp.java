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
        try {
            return DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (System.getenv("DB_USER") == null && (
                    msg.contains("password authentication failed") ||
                    msg.contains("role \"postgres\" does not exist") ||
                    (msg.contains("role") && msg.contains("does not exist"))
            )) {
                try {
                    String localUser = System.getProperty("user.name");
                    return DriverManager.getConnection(DB_URL, localUser, "");
                } catch (SQLException ignored) {
                }
            }
            throw e;
        }
    }

    static void ensureDatabaseExists() {
        if (!DB_URL.contains("school_management")) {
            return;
        }
        try (Connection testConn = connect()) {
            return;
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("does not exist") || msg.contains("database \"school_management\"")) {
                String maintenanceUrl = DB_URL.replace("/school_management", "/postgres");
                boolean created = false;
                try (Connection mConn = DriverManager.getConnection(maintenanceUrl, DB_USER, DB_PASSWORD);
                     Statement st = mConn.createStatement()) {
                    st.executeUpdate("CREATE DATABASE school_management");
                    created = true;
                } catch (Exception ignored) {
                }

                if (!created) {
                    try {
                        String localUser = System.getProperty("user.name");
                        try (Connection mConn = DriverManager.getConnection(maintenanceUrl, localUser, "");
                             Statement st = mConn.createStatement()) {
                            st.executeUpdate("CREATE DATABASE school_management");
                            created = true;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (created) {
                    System.out.println("Database 'school_management' verified/created.");
                }
            }
        }
    }


    // =====================================================
    // CREATE TABLES
    // =====================================================

    static void createTables() {

        ensureDatabaseExists();

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

        String lower = message.toLowerCase();
        StringBuilder hintHtml = new StringBuilder();

        if (lower.contains("refused") || (lower.contains("5432") && lower.contains("postmaster"))) {
            hintHtml.append("<div style='background:#fef2f2; border:1px solid #fecaca; border-radius:8px; padding:18px; margin:20px 0; text-align:left;'>")
                    .append("<h3 style='color:#991b1b; margin-top:0;'>⚠️ PostgreSQL Server is Not Running</h3>")
                    .append("<p style='color:#374151; margin-bottom:8px;'>The application tried to connect to <code>localhost:5432</code>, but no PostgreSQL server is accepting connections.</p>")
                    .append("<p style='color:#374151; margin-bottom:4px;'><strong>Fix (macOS Terminal):</strong></p>")
                    .append("<div style='background:#111827; color:#34d399; padding:10px 14px; border-radius:6px; font-family:monospace; margin-bottom:10px;'>brew services start postgresql@14</div>")
                    .append("<p style='color:#6b7280; font-size:13px; margin:0;'>Once started, click the Retry button below.</p>")
                    .append("</div>");
        } else if (lower.contains("database \"school_management\" does not exist") || (lower.contains("database") && lower.contains("does not exist"))) {
            hintHtml.append("<div style='background:#eff6ff; border:1px solid #bfdbfe; border-radius:8px; padding:18px; margin:20px 0; text-align:left;'>")
                    .append("<h3 style='color:#1e40af; margin-top:0;'>⚠️ Database Not Created Yet</h3>")
                    .append("<p style='color:#374151; margin-bottom:8px;'>The PostgreSQL database <code>school_management</code> does not exist yet.</p>")
                    .append("<p style='color:#374151; margin-bottom:4px;'><strong>Fix (Terminal):</strong></p>")
                    .append("<div style='background:#111827; color:#34d399; padding:10px 14px; border-radius:6px; font-family:monospace; margin-bottom:10px;'>createdb school_management</div>")
                    .append("<p style='color:#6b7280; font-size:13px; margin:0;'>Then click Retry to reload this page.</p>")
                    .append("</div>");
        } else if (lower.contains("password authentication failed") || (lower.contains("role") && lower.contains("does not exist"))) {
            hintHtml.append("<div style='background:#fefce8; border:1px solid #fef08a; border-radius:8px; padding:18px; margin:20px 0; text-align:left;'>")
                    .append("<h3 style='color:#854d0e; margin-top:0;'>⚠️ PostgreSQL Authentication Issue</h3>")
                    .append("<p style='color:#374151; margin-bottom:8px;'>PostgreSQL rejected user <code>" + escape(DB_USER) + "</code>.</p>")
                    .append("<p style='color:#374151; margin-bottom:4px;'><strong>Fix:</strong> Create the user in PostgreSQL or set the <code>DB_USER</code> and <code>DB_PASSWORD</code> environment variables:</p>")
                    .append("<div style='background:#111827; color:#34d399; padding:10px 14px; border-radius:6px; font-family:monospace; margin-bottom:10px;'>psql -d postgres -c \"CREATE USER " + escape(DB_USER) + " WITH SUPERUSER PASSWORD '" + escape(DB_PASSWORD) + "';\"</div>")
                    .append("</div>");
        }

        String html =
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<title>Error - School Management System</title>" +
                "<style>" +
                "body {" +
                "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;" +
                "background: #f4f6f8;" +
                "text-align: center;" +
                "padding: 60px 20px;" +
                "}" +
                ".error {" +
                "background: white;" +
                "max-width: 720px;" +
                "margin: auto;" +
                "padding: 35px 30px;" +
                "border-radius: 12px;" +
                "box-shadow: 0 4px 20px rgba(0,0,0,0.08);" +
                "}" +
                "h1 {" +
                "color: #dc2626;" +
                "margin-top: 0;" +
                "}" +
                ".raw-error {" +
                "background: #f8fafc;" +
                "border: 1px solid #e2e8f0;" +
                "padding: 12px;" +
                "border-radius: 6px;" +
                "color: #475569;" +
                "font-family: monospace;" +
                "font-size: 13px;" +
                "word-break: break-word;" +
                "margin: 15px 0;" +
                "text-align: left;" +
                "}" +
                ".btn {" +
                "display: inline-block;" +
                "margin: 8px;" +
                "padding: 10px 22px;" +
                "font-size: 14px;" +
                "font-weight: 600;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "cursor: pointer;" +
                "border: none;" +
                "}" +
                ".btn-retry {" +
                "background: #10b981;" +
                "color: white;" +
                "}" +
                ".btn-dashboard {" +
                "background: #2563eb;" +
                "color: white;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='error'>" +
                "<h1>Something went wrong</h1>" +
                "<div class='raw-error'>" + escape(message) + "</div>" +
                hintHtml.toString() +
                "<div style='margin-top: 25px;'>" +
                "<button class='btn btn-retry' onclick='window.location.reload()'>🔄 Retry</button>" +
                "<a class='btn btn-dashboard' href='/'>Dashboard</a>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        return html;
    }
}