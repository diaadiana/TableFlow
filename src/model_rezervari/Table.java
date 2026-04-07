package model_rezervari;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Represents a restaurant table with ID, capacity, and location.
// Manages associated reservations and checks availability based on rules.

public class Table {

    private int id;
    private int capacity;
    private Location location;
    /* default */ List<Reservation> reservations = new ArrayList<>();

    public Table(int id, int capacity, Location location) {
        this.id = id;
        this.capacity = capacity;
        this.location = location;
    }

    public boolean isAvailable(LocalDateTime dateTime, int guestCount, String clientName) {
        int occupied = reservations.stream()
                .filter(r -> r.getDateTime().equals(dateTime))
                .mapToInt(Reservation::getGuestCount)
                .sum();

        boolean otherClient = reservations.stream()
                .anyMatch(r -> r.getDateTime().equals(dateTime) &&
                        !r.getClientName().equalsIgnoreCase(clientName));

        return !otherClient && (occupied + guestCount <= capacity);
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public int getRemainingSeats(LocalDateTime dateTime) {
        int occupied = reservations.stream()
                .filter(r -> r.getDateTime().equals(dateTime))
                .mapToInt(Reservation::getGuestCount)
                .sum();
        return capacity - occupied;
    }

    public Location getLocation() {
        return location;
    }

    public int getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "Table " + id + " (" + location + ", " + capacity + " seats)";
    }
}
