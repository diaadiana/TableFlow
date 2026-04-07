package model_rezervari;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TableFlow — Restaurant Management
 *
 * Builds the restaurant model, seeds sample reservations,
 * starts the embedded HTTP server and opens the dashboard.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        List<Table> tables = new ArrayList<>();
        tables.add(new Table(1, 3, Location.INDOOR));
        tables.add(new Table(2, 2, Location.INDOOR));
        tables.add(new Table(3, 4, Location.INDOOR));
        tables.add(new Table(4, 4, Location.INDOOR));
        tables.add(new Table(5, 4, Location.OUTDOOR));
        tables.add(new Table(6, 5, Location.OUTDOOR));
        tables.add(new Table(7, 6, Location.OUTDOOR));

        Restaurant restaurant = new Restaurant(new ArrayList<>(tables));

        // ── Attempt to load saved reservations ──────────────
        Database.load(tables);

        boolean hasData = tables.stream().anyMatch(t -> !t.reservations.isEmpty());
        if (!hasData) {
            System.out.println("[Database] No existing data found. Seeding defaults...");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime slot = now.withMinute(0).withSecond(0).withNano(0);

            restaurant.makeReservation("Alexandru Popescu", 4, slot, Location.OUTDOOR, BookingType.BIRTHDAY, null);
            restaurant.makeReservation("Maria Ionescu", 2, slot, Location.INDOOR, BookingType.FAMILY, null);
            restaurant.makeReservation("Andrei Dumitrescu", 3, slot, Location.INDOOR, BookingType.FRIENDS, null);
            restaurant.makeReservation("Elena Gheorghe", 1, slot, Location.OUTDOOR, BookingType.MEETING, null);
        }

        // ── Start server ────────────────────────────────────
        ApiServer server = new ApiServer(restaurant, tables, 8080);
        server.start();

        System.out.println("TableFlow — http://localhost:8080");

        Thread.sleep(400);
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        }

        Thread.currentThread().join();
    }
}
