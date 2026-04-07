package model_rezervari;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bella Vista — Restaurant Management
 *
 * Builds the restaurant model, seeds sample reservations,
 * starts the embedded HTTP server and opens the dashboard.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        List<AranjareMese> mese = new ArrayList<>();
        mese.add(new AranjareMese(1, 3, Amplasare.INTERIOR));
        mese.add(new AranjareMese(2, 2, Amplasare.INTERIOR));
        mese.add(new AranjareMese(3, 4, Amplasare.INTERIOR));
        mese.add(new AranjareMese(4, 4, Amplasare.INTERIOR));
        mese.add(new AranjareMese(5, 4, Amplasare.EXTERIOR));
        mese.add(new AranjareMese(6, 5, Amplasare.EXTERIOR));
        mese.add(new AranjareMese(7, 6, Amplasare.EXTERIOR));

        Restaurant restaurant = new Restaurant(new ArrayList<>(mese));

        // ── Seed sample reservations for today ──────────────
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slot = now.withMinute(0).withSecond(0).withNano(0);

        restaurant.faRezervare("Alexandru Popescu", 4, slot,
                Amplasare.EXTERIOR, SpecificulRezervarii.ZI_DE_NASTERE);

        restaurant.faRezervare("Maria Ionescu", 2, slot,
                Amplasare.INTERIOR, SpecificulRezervarii.FAMILIE);

        restaurant.faRezervare("Andrei Dumitrescu", 3, slot,
                Amplasare.INTERIOR, SpecificulRezervarii.PRIETENI);

        restaurant.faRezervare("Elena Gheorghe", 1, slot,
                Amplasare.EXTERIOR, SpecificulRezervarii.INTALNIRE);

        // ── Start server ────────────────────────────────────
        ApiServer server = new ApiServer(restaurant, mese, 8080);
        server.start();

        System.out.println("Bella Vista — http://localhost:8080");

        Thread.sleep(400);
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        }

        Thread.currentThread().join();
    }
}
