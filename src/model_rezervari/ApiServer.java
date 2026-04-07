package model_rezervari;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Embedded HTTP server (JDK built-in, zero external dependencies).
 * Serves the static web UI from src/model_rezervari/ui/ and exposes a REST API.
 *
 * Endpoints:
 *   GET  /                          → index.html
 *   GET  /api/tables?datetime=...   → JSON array of table statuses
 *   POST /api/reservation           → create a reservation
 *   GET  /api/reservations          → all reservations as JSON
 *   GET  /api/search?name=&from=&to= → filtered reservations
 */
public class ApiServer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Restaurant restaurant;
    private final List<Table> tables;
    private final int port;
    private HttpServer server;

    public ApiServer(Restaurant restaurant, List<Table> tables, int port) {
        this.restaurant = restaurant;
        this.tables     = tables;
        this.port       = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/tables",       this::handleTables);
        server.createContext("/api/reservation",  this::handleReservation);
        server.createContext("/api/reservations", this::handleGetReservations);
        server.createContext("/api/search",       this::handleSearch);
        server.createContext("/",                 this::handleStatic);

        server.setExecutor(null);   // default executor (new thread per request)
        server.start();
    }

    public void stop() { server.stop(0); }

    // ── Static file serving ──────────────────────────────────────────────────

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        byte[] data = null;

        // 1) Try classpath resource (works from JAR / IntelliJ output)
        String res = "/model_rezervari/ui" + path;
        try (InputStream is = getClass().getResourceAsStream(res)) {
            if (is != null) data = readAll(is);
        }

        // 2) Fallback: read from source tree (development mode)
        if (data == null) {
            File f = new File("src/model_rezervari/ui" + path);
            if (f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    data = readAll(is);
                }
            }
        }

        if (data == null) {
            byte[] msg = ("404 Not Found: " + path).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            ex.getResponseBody().write(msg);
            ex.getResponseBody().close();
            return;
        }

        ex.getResponseHeaders().add("Content-Type", contentType(path));
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        return "text/plain";
    }

    // ── GET /api/tables ──────────────────────────────────────────────────────

    private void handleTables(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;

        String query = ex.getRequestURI().getQuery();
        LocalDateTime dt = parseParam(query, "datetime");
        if (dt == null) dt = LocalDateTime.now();

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tables.size(); i++) {
            Table t         = tables.get(i);
            int remaining   = t.getRemainingSeats(dt);
            int cap         = t.getCapacity();

            String status;
            if (remaining == cap)                               status = "FREE";
            else if (remaining <= 0 || (cap > 2 && remaining <= 2)) status = "OCCUPIED";
            else                                                status = "PARTIAL";

            sb.append(i > 0 ? "," : "")
              .append("{\"id\":").append(t.getId())
              .append(",\"capacity\":").append(cap)
              .append(",\"location\":\"").append(t.getLocation())
              .append("\",\"seatsRemaining\":").append(remaining)
              .append(",\"status\":\"").append(status).append("\"}");
        }
        sb.append("]");
        json(ex, 200, sb.toString());
    }

    // ── POST /api/reservation ────────────────────────────────────────────────

    private void handleReservation(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;

        if ("DELETE".equals(ex.getRequestMethod())) {
            String query = ex.getRequestURI().getQuery();
            String idStr = param(query, "tableId");
            LocalDateTime dt = parseParam(query, "datetime");
            if (idStr != null && dt != null) {
                int tableId = Integer.parseInt(idStr);
                if (restaurant.cancelReservation(tableId, dt)) json(ex, 200, "{\"success\":true}");
                else json(ex, 404, "{\"success\":false,\"message\":\"Reservation not found.\"}");
            } else {
                json(ex, 400, err("Missing tableId or datetime"));
            }
            return;
        }

        if (!"POST".equals(ex.getRequestMethod())) { json(ex, 405, err("Method not allowed")); return; }

        byte[] rawBody = readAll(ex.getRequestBody());
        String body = new String(rawBody, StandardCharsets.UTF_8);
        try {
            String clientName  = field(body, "clientName");
            int    guests      = Integer.parseInt(field(body, "guests"));
            String dateTimeStr = field(body, "dateTime");
            String locationStr = field(body, "location");
            String specificStr = field(body, "specific");

            LocalDateTime dateTime   = LocalDateTime.parse(dateTimeStr, FMT);
            Location      location   = Location.valueOf(locationStr.toUpperCase());
            BookingType   bookingType = BookingType.valueOf(specificStr.toUpperCase());

            Integer tableId = null;
            try {
                String tIdStr = field(body, "tableId");
                if (!tIdStr.isEmpty() && !tIdStr.equals("null")) {
                    tableId = Integer.parseInt(tIdStr);
                }
            } catch (Exception ignore) {}

            boolean ok = restaurant.makeReservation(clientName, guests, dateTime, location, bookingType, tableId);
            if (ok) json(ex, 200, "{\"success\":true,\"message\":\"Reservation recorded successfully!\"}");
            else    json(ex, 409, "{\"success\":false,\"message\":\"No table available for selected criteria.\"}");

        } catch (Exception e) {
            json(ex, 400, err("Invalid data: " + esc(e.getMessage())));
        }
    }

    // ── GET /api/reservations ────────────────────────────────────────────────

    private void handleGetReservations(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        json(ex, 200, serializeList(
            tables.stream().flatMap(t -> t.reservations.stream()).collect(Collectors.toList())
        ));
    }

    // ── GET /api/search ──────────────────────────────────────────────────────

    private void handleSearch(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        String q    = ex.getRequestURI().getQuery();
        String name = param(q, "name");
        LocalDateTime from = parseParam(q, "from");
        LocalDateTime to   = parseParam(q, "to");

        if (name == null || from == null || to == null) {
            json(ex, 400, err("Missing parameters: name, from, to")); return;
        }
        json(ex, 200, serializeList(restaurant.findClientReservations(name, from, to)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String serializeList(List<Reservation> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Reservation r = list.get(i);
            sb.append(i > 0 ? "," : "")
              .append("{\"clientName\":\"").append(esc(r.getClientName()))
              .append("\",\"guests\":").append(r.getGuestCount())
              .append(",\"dateTime\":\"").append(r.getDateTime().format(FMT))
              .append("\",\"location\":\"").append(r.getTable().getLocation())
              .append("\",\"specific\":\"").append(r.getBookingType())
              .append("\",\"tableId\":").append(r.getTable().getId())
              .append(",\"capacity\":").append(r.getTable().getCapacity())
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Returns true if this was a CORS preflight that has been handled. */
    private boolean preflight(HttpExchange ex) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Access-Control-Allow-Origin",  "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(ex.getRequestMethod())) { json(ex, 200, "{}"); return true; }
        return false;
    }

    private void json(HttpExchange ex, int code, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private String err(String msg) { return "{\"success\":false,\"message\":\"" + esc(msg) + "\"}"; }
    private String esc(String s)   { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }

    /** Extract a string or number field from a naïve JSON body. */
    private String field(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) throw new IllegalArgumentException("Missing field: " + key);
        int s = idx + search.length();
        while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
        if (json.charAt(s) == '"') {
            int e = json.indexOf('"', s + 1);
            return json.substring(s + 1, e);
        }
        int e = s;
        while (e < json.length() && "0123456789.-".indexOf(json.charAt(e)) >= 0) e++;
        return json.substring(s, e);
    }

    private String param(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                try { return URLDecoder.decode(kv[1], "UTF-8"); }
                catch (Exception e) { return kv[1]; }
            }
        }
        return null;
    }

    private LocalDateTime parseParam(String query, String name) {
        String v = param(query, name);
        if (v == null) return null;
        try { return LocalDateTime.parse(v, FMT); } catch (DateTimeParseException e) { return null; }
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
}
