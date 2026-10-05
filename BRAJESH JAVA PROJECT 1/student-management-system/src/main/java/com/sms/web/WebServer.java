package com.sms.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sms.exception.DataAccessException;
import com.sms.exception.DuplicateStudentException;
import com.sms.exception.ValidationException;
import com.sms.model.Student;
import com.sms.service.StudentService;
import com.sms.util.Constants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server providing a REST API and serving the modern web UI.
 * Backed by {@link com.sun.net.httpserver.HttpServer} with zero external server bloat.
 */
public final class WebServer {

  private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);
  private final StudentService service;
  private final int port;
  private HttpServer server;
  private final Gson gson;

  public WebServer(StudentService service, int port) {
    this.service = service;
    this.port = port;
    this.gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
          @Override
          public void write(JsonWriter out, LocalDateTime val) throws IOException {
            if (val == null) {
              out.nullValue();
            } else {
              out.value(val.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
          }
          @Override
          public LocalDateTime read(JsonReader in) throws IOException {
            String s = in.nextString();
            return s != null ? LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
          }
        })
        .setPrettyPrinting()
        .create();
  }

  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

    // API Handlers
    server.createContext("/api/students", new StudentsHandler());
    server.createContext("/api/stats", new StatsHandler());
    server.createContext("/api/export", new ExportHandler());
    server.createContext("/api/health", new HealthHandler());

    // Static Web Assets
    server.createContext("/", new StaticResourceHandler());

    server.start();
    LOG.info("SMS Web Server started on http://localhost:{}/", port);
  }

  public void stop() {
    if (server != null) {
      server.stop(1);
      LOG.info("SMS Web Server stopped.");
    }
  }

  public int getPort() {
    return port;
  }

  // ----------------------------------------------------------------- API Handlers

  private class StudentsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      addCorsHeaders(exchange);
      String method = exchange.getRequestMethod().toUpperCase();

      if ("OPTIONS".equals(method)) {
        exchange.sendResponseHeaders(204, -1);
        return;
      }

      String path = exchange.getRequestURI().getPath(); // e.g. /api/students or /api/students/CS2021001
      String id = extractId(path, "/api/students");

      try {
        switch (method) {
          case "GET" -> {
            if (id != null && !id.isBlank()) {
              handleGetById(exchange, id);
            } else {
              handleGetAllOrSearch(exchange);
            }
          }
          case "POST" -> handleCreate(exchange);
          case "PUT" -> {
            if (id != null && !id.isBlank()) {
              handleUpdate(exchange, id);
            } else {
              sendJson(exchange, 400, Map.of("error", "Student ID is required in URL path"));
            }
          }
          case "DELETE" -> {
            if (id != null && !id.isBlank()) {
              handleDelete(exchange, id);
            } else {
              sendJson(exchange, 400, Map.of("error", "Student ID is required in URL path"));
            }
          }
          default -> sendJson(exchange, 405, Map.of("error", "Method not allowed"));
        }
      } catch (ValidationException ve) {
        sendJson(exchange, 400, Map.of("error", "Validation failed", "fields", ve.getFieldErrors()));
      } catch (DuplicateStudentException de) {
        sendJson(exchange, 409, Map.of(
            "error", "Duplicate value",
            "field", de.getConflictField(),
            "value", de.getConflictValue(),
            "message", de.getMessage()
        ));
      } catch (DataAccessException dae) {
        LOG.error("Database error in API", dae);
        sendJson(exchange, 500, Map.of("error", "Database error: " + dae.getMessage()));
      } catch (Exception ex) {
        LOG.error("Unexpected error in API", ex);
        sendJson(exchange, 500, Map.of("error", "Server error: " + ex.getMessage()));
      }
    }

    private void handleGetAllOrSearch(HttpExchange exchange) throws IOException {
      Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
      String q = query.get("search");
      String branch = query.get("branch");
      String yearStr = query.get("year");

      int year = 0;
      if (yearStr != null && !yearStr.isBlank()) {
        try {
          year = Integer.parseInt(yearStr.trim());
        } catch (NumberFormatException ignored) {}
      }

      List<Student> students;
      boolean hasSearch = (q != null && !q.isBlank()) ||
                          (branch != null && !branch.isBlank() && !"ALL".equalsIgnoreCase(branch)) ||
                          (year > 0);

      if (hasSearch) {
        String filterBranch = (branch == null || "ALL".equalsIgnoreCase(branch)) ? null : branch;
        students = service.search(q, filterBranch, year);
      } else {
        students = service.findAll();
      }

      sendJson(exchange, 200, students);
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
      Optional<Student> student = service.findById(id);
      if (student.isPresent()) {
        sendJson(exchange, 200, student.get());
      } else {
        sendJson(exchange, 404, Map.of("error", "Student not found with ID: " + id));
      }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
      String body = readBody(exchange);
      JsonObject json = JsonParser.parseString(body).getAsJsonObject();

      String studentId = getStr(json, "studentId");
      String name = getStr(json, "name");
      String branch = getStr(json, "branch");
      int year = getInt(json, "year", 1);
      String phone = getStr(json, "phone");
      String marksText = getStr(json, "marks");

      service.addStudent(studentId, name, branch, year, phone, marksText);
      sendJson(exchange, 201, Map.of(
          "success", true,
          "message", "Student created successfully",
          "studentId", studentId
      ));
    }

    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
      String body = readBody(exchange);
      JsonObject json = JsonParser.parseString(body).getAsJsonObject();

      String name = getStr(json, "name");
      String branch = getStr(json, "branch");
      int year = getInt(json, "year", 1);
      String phone = getStr(json, "phone");
      String marksText = getStr(json, "marks");

      service.updateStudent(id, name, branch, year, phone, marksText);
      sendJson(exchange, 200, Map.of(
          "success", true,
          "message", "Student updated successfully",
          "studentId", id
      ));
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
      service.deleteStudent(id);
      sendJson(exchange, 200, Map.of(
          "success", true,
          "message", "Student deleted successfully",
          "studentId", id
      ));
    }
  }

  private class StatsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      addCorsHeaders(exchange);
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        return;
      }

      List<Student> all = service.findAll();
      int total = all.size();
      double avg = service.averageMarks(all);
      Optional<Student> top = service.topScorer(all);

      Map<String, Integer> branchCounts = new LinkedHashMap<>();
      for (String b : Constants.BRANCHES) {
        branchCounts.put(b, 0);
      }
      int passed = 0;
      int gradeA = 0, gradeB = 0, gradeC = 0, gradeD = 0, gradeF = 0;

      for (Student s : all) {
        branchCounts.put(s.getBranch(), branchCounts.getOrDefault(s.getBranch(), 0) + 1);
        double m = s.getMarks().doubleValue();
        if (m >= 40.0) passed++;
        if (m >= 80.0) gradeA++;
        else if (m >= 65.0) gradeB++;
        else if (m >= 50.0) gradeC++;
        else if (m >= 40.0) gradeD++;
        else gradeF++;
      }

      Map<String, Object> stats = new LinkedHashMap<>();
      stats.put("totalStudents", total);
      stats.put("averageMarks", Math.round(avg * 100.0) / 100.0);
      stats.put("passRate", total > 0 ? Math.round(((double) passed / total) * 1000.0) / 10.0 : 0.0);
      stats.put("topScorer", top.orElse(null));
      stats.put("branchCounts", branchCounts);
      stats.put("grades", Map.of(
          "A (>=80%)", gradeA,
          "B (65-79%)", gradeB,
          "C (50-64%)", gradeC,
          "D (40-49%)", gradeD,
          "F (<40%)", gradeF
      ));

      sendJson(exchange, 200, stats);
    }
  }

  private class ExportHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      addCorsHeaders(exchange);
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        return;
      }

      List<Student> all = service.findAll();
      File tempFile = File.createTempFile("sms-export-", ".csv");
      try {
        service.exportToCsv(all, tempFile);
        byte[] bytes = Files.readAllBytes(tempFile.toPath());

        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"students.csv\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(bytes);
        }
      } finally {
        tempFile.delete();
      }
    }
  }

  private class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      addCorsHeaders(exchange);
      sendJson(exchange, 200, Map.of(
          "status", "UP",
          "database", "Connected",
          "port", port,
          "timestamp", LocalDateTime.now().toString()
      ));
    }
  }

  // ----------------------------------------------------------- Static Resource Handler

  private class StaticResourceHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      if (path == null || path.equals("/") || path.isBlank()) {
        path = "/index.html";
      }

      // Sanitize
      path = path.replace("\\", "/");
      if (path.contains("..")) {
        exchange.sendResponseHeaders(403, -1);
        return;
      }

      byte[] content = loadResource(path);
      if (content == null) {
        String notFound = "<!DOCTYPE html><html><body><h1>404 Not Found</h1><p>The requested file " + path + " was not found.</p></body></html>";
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(404, notFound.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(notFound.getBytes(StandardCharsets.UTF_8));
        }
        return;
      }

      String contentType = guessContentType(path);
      exchange.getResponseHeaders().set("Content-Type", contentType);
      exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
      exchange.sendResponseHeaders(200, content.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(content);
      }
    }

    private byte[] loadResource(String path) {
      // 1. Try classpath /web/...
      String resourcePath = "/web" + (path.startsWith("/") ? path : "/" + path);
      try (InputStream in = WebServer.class.getResourceAsStream(resourcePath)) {
        if (in != null) {
          return in.readAllBytes();
        }
      } catch (IOException ignored) {}

      // 2. Try file system relative to project root src/main/resources/web/...
      Path localPath = Paths.get("src", "main", "resources", "web", path.startsWith("/") ? path.substring(1) : path);
      if (Files.exists(localPath)) {
        try {
          return Files.readAllBytes(localPath);
        } catch (IOException ignored) {}
      }

      return null;
    }

    private String guessContentType(String path) {
      if (path.endsWith(".html")) return "text/html; charset=UTF-8";
      if (path.endsWith(".css")) return "text/css; charset=UTF-8";
      if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
      if (path.endsWith(".json")) return "application/json; charset=UTF-8";
      if (path.endsWith(".svg")) return "image/svg+xml";
      if (path.endsWith(".png")) return "image/png";
      if (path.endsWith(".ico")) return "image/x-icon";
      return "text/plain; charset=UTF-8";
    }
  }

  // ----------------------------------------------------------------- Helpers

  private void addCorsHeaders(HttpExchange exchange) {
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  }

  private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
    byte[] jsonBytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(statusCode, jsonBytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(jsonBytes);
    }
  }

  private String readBody(HttpExchange exchange) throws IOException {
    try (InputStream in = exchange.getRequestBody()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String extractId(String path, String prefix) {
    if (path.startsWith(prefix)) {
      String sub = path.substring(prefix.length());
      if (sub.startsWith("/")) sub = sub.substring(1);
      return sub.trim();
    }
    return null;
  }

  private Map<String, String> parseQuery(String query) {
    Map<String, String> map = new HashMap<>();
    if (query == null || query.isBlank()) return map;
    for (String pair : query.split("&")) {
      int idx = pair.indexOf("=");
      if (idx > 0) {
        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
        String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
        map.put(key, val);
      }
    }
    return map;
  }

  private String getStr(JsonObject obj, String key) {
    return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
  }

  private int getInt(JsonObject obj, String key, int def) {
    try {
      return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
    } catch (Exception e) {
      return def;
    }
  }
}
