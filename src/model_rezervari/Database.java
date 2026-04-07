package model_rezervari;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Database {
    private static final String FILE_NAME = "reservations.json";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void save(List<AranjareMese> mese) {
        StringBuilder sb = new StringBuilder("[\n");
        boolean first = true;
        for (AranjareMese m : mese) {
            for (Rezervare r : m.rezervari) {
                if (!first) sb.append(",\n");
                sb.append("  ").append(serialize(r));
                first = false;
            }
        }
        sb.append("\n]");
        
        try {
            Files.write(Paths.get(FILE_NAME), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Error saving DB: " + e.getMessage());
        }
    }

    public static void load(List<AranjareMese> mese) {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;

        try {
            String json = new String(Files.readAllBytes(Paths.get(FILE_NAME)), StandardCharsets.UTF_8);
            int idx = json.indexOf('{');
            while (idx >= 0) {
                int end = json.indexOf('}', idx);
                if (end < 0) break;
                String obj = json.substring(idx, end + 1);
                
                String clientName = field(obj, "clientName");
                int guests = Integer.parseInt(field(obj, "guests"));
                SpecificulRezervarii specific = SpecificulRezervarii.valueOf(field(obj, "specific"));
                LocalDateTime dt = LocalDateTime.parse(field(obj, "dateTime"), FMT);
                int masaId = Integer.parseInt(field(obj, "masaId"));

                AranjareMese targetMasa = null;
                for (AranjareMese m : mese) {
                    if (m.getId() == masaId) { targetMasa = m; break; }
                }
                
                if (targetMasa != null) {
                    targetMasa.adaugaRezervare(new Rezervare(clientName, guests, dt, specific, targetMasa));
                }
                
                idx = json.indexOf('{', end);
            }
            System.out.println("[Database] Loaded reservations successfully.");
        } catch (Exception e) {
            System.err.println("Error loading DB: " + e.getMessage());
        }
    }

    private static String serialize(Rezervare r) {
        return String.format(
            "{\"clientName\":\"%s\",\"guests\":%d,\"location\":\"%s\",\"specific\":\"%s\",\"dateTime\":\"%s\",\"masaId\":%d}",
            esc(r.getNumeClient()), r.getNrPersoane(), r.getMasa().getAmplasare(), r.getSpecific(),
            r.getDataOra().format(FMT), r.getMasa().getId()
        );
    }

    private static String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }

    private static String field(String json, String key) {
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
}
