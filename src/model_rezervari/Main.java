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

        List<AranjareMese> mese = new ArrayList<>();
        mese.add(new AranjareMese(1, 3, Amplasare.INDOOR));
        mese.add(new AranjareMese(2, 2, Amplasare.INDOOR));
        mese.add(new AranjareMese(3, 4, Amplasare.INDOOR));
        mese.add(new AranjareMese(4, 4, Amplasare.INDOOR));
        mese.add(new AranjareMese(5, 4, Amplasare.OUTDOOR));
        mese.add(new AranjareMese(6, 5, Amplasare.OUTDOOR));
        mese.add(new AranjareMese(7, 6, Amplasare.OUTDOOR));

        Restaurant restaurant = new Restaurant(new ArrayList<>(mese));

        // ── Attempt to load saved reservations ──────────────
        Database.load(mese);

        boolean hasData = mese.stream().anyMatch(m -> !m.rezervari.isEmpty());
        if (!hasData) {
            System.out.println("[Database] No existing data found. Seeding defaults...");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime slot = now.withMinute(0).withSecond(0).withNano(0);
            
            restaurant.faRezervare("Alexandru Popescu", 4, slot, Amplasare.OUTDOOR, SpecificulRezervarii.BIRTHDAY, null);
            restaurant.faRezervare("Maria Ionescu", 2, slot, Amplasare.INDOOR, SpecificulRezervarii.FAMILY, null);
            restaurant.faRezervare("Andrei Dumitrescu", 3, slot, Amplasare.INDOOR, SpecificulRezervarii.FRIENDS, null);
            restaurant.faRezervare("Elena Gheorghe", 1, slot, Amplasare.OUTDOOR, SpecificulRezervarii.MEETING, null);
        }

        // ── Start server ────────────────────────────────────
        ApiServer server = new ApiServer(restaurant, mese, 8080);
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
