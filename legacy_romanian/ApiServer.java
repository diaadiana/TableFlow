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
    private final List<AranjareMese> mese;
    private final int port;
    private HttpServer server;

    public ApiServer(Restaurant restaurant, List<AranjareMese> mese, int port) {
        this.restaurant = restaurant;
        this.mese       = mese;
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
            if (is != null) data = is.readAllBytes();
        }

        // 2) Fallback: read from source tree (development mode)
        if (data == null) {
            File f = new File("src/model_rezervari/ui" + path);
            if (f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    data = is.readAllBytes();
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
        for (int i = 0; i < mese.size(); i++) {
            AranjareMese m  = mese.get(i);
            int ramase      = m.getLocuriRamase(dt);
            int cap         = m.getCapacitate();

            String status;
            if (ramase == cap)                              status = "FREE";
            else if (ramase <= 0 || (cap > 2 && ramase <= 2)) status = "OCCUPIED";
            else                                            status = "PARTIAL";

            sb.append(i > 0 ? "," : "")
              .append("{\"id\":").append(m.getId())
              .append(",\"capacity\":").append(cap)
              .append(",\"location\":\"").append(m.getAmplasare())
              .append("\",\"locuriRamase\":").append(ramase)
              .append(",\"status\":\"").append(status).append("\"}");
        }
        sb.append("]");
        json(ex, 200, sb.toString());
    }

    // ── POST /api/reservation ────────────────────────────────────────────────

    private void handleReservation(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (!"POST".equals(ex.getRequestMethod())) { json(ex, 405, err("Method not allowed")); return; }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            String numeClient   = field(body, "numeClient");
            int    nrPersoane   = Integer.parseInt(field(body, "nrPersoane"));
            String dataOraStr   = field(body, "dataOra");
            String amplasareStr = field(body, "amplasare");
            String specificStr  = field(body, "specific");

            LocalDateTime      dataOra   = LocalDateTime.parse(dataOraStr, FMT);
            Amplasare          amplasare = Amplasare.valueOf(amplasareStr.toUpperCase());
            SpecificulRezervarii specific = SpecificulRezervarii.valueOf(specificStr.toUpperCase());

            boolean ok = restaurant.faRezervare(numeClient, nrPersoane, dataOra, amplasare, specific);
            if (ok) json(ex, 200, "{\"success\":true,\"message\":\"Rezervare înregistrată cu succes!\"}");
            else    json(ex, 409, "{\"success\":false,\"message\":\"Nicio masă disponibilă pentru criteriile selectate.\"}");

        } catch (Exception e) {
            json(ex, 400, err("Date invalide: " + esc(e.getMessage())));
        }
    }

    // ── GET /api/reservations ────────────────────────────────────────────────

    private void handleGetReservations(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        json(ex, 200, serializeList(
            mese.stream().flatMap(m -> m.rezervari.stream()).collect(Collectors.toList())
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
            json(ex, 400, err("Parametri lipsă: name, from, to")); return;
        }
        json(ex, 200, serializeList(restaurant.cautaRezervariClient(name, from, to)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String serializeList(List<Rezervare> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Rezervare r = list.get(i);
            sb.append(i > 0 ? "," : "")
              .append("{\"numeClient\":\"").append(esc(r.getNumeClient()))
              .append("\",\"nrPersoane\":").append(r.getNrPersoane())
              .append(",\"dataOra\":\"").append(r.getDataOra().format(FMT))
              .append("\",\"amplasare\":\"").append(r.getMasa().getAmplasare())
              .append("\",\"specific\":\"").append(r.getSpecific())
              .append("\",\"masaId\":").append(r.getMasa().getId())
              .append(",\"capacitate\":").append(r.getMasa().getCapacitate())
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Returns true if this was a CORS preflight that has been handled. */
    private boolean preflight(HttpExchange ex) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Access-Control-Allow-Origin",  "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
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
                try { return URLDecoder.decode(kv[1], StandardCharsets.UTF_8); }
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
}
