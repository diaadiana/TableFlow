package model_rezervari;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Manages tables and reservations.
// Allows creating reservations, viewing all reservations,
// and searching for a client's reservations within a time interval.

public class Restaurant {

    private List<Table> tables;

    public Restaurant(List<Table> tables) {
        this.tables = tables;
    }

    public boolean makeReservation(String clientName,
                                   int guestCount,
                                   LocalDateTime dateTime,
                                   Location location,
                                   BookingType bookingType,
                                   Integer requestedTableId) {

        java.util.function.Predicate<Table> isOptimized = t -> {
            int cap = t.getCapacity();
            if (guestCount == 1 && cap > 2) return false;
            if (guestCount == 3 && cap > 4) return false;
            // Standard rule: don't leave more than 1 seat empty if possible
            if (cap - guestCount > 1) return false;
            return true;
        };

        Optional<Table> targetTable = Optional.empty();

        // 1. Try to find a PERFECTLY OPTIMIZED table in the requested location
        // Priority to the one clicked if it's optimized and available
        if (requestedTableId != null) {
            targetTable = tables.stream()
                    .filter(t -> t.getId() == (int) requestedTableId && t.getLocation() == location &&
                            isOptimized.test(t) && t.isAvailable(dateTime, guestCount, clientName) &&
                            t.getRemainingSeats(dateTime) >= guestCount)
                    .findFirst();
        }

        // 2. If not found, look for ANY other optimized table in that location
        if (!targetTable.isPresent()) {
            targetTable = tables.stream()
                    .filter(t -> t.getLocation() == location &&
                            isOptimized.test(t) && t.isAvailable(dateTime, guestCount, clientName) &&
                            t.getRemainingSeats(dateTime) >= guestCount)
                    .findFirst();
        }

        // 3. Last Resort: If no *optimized* table found, take ANY available table that fits the guests
        // to avoid throwing an error to the user.
        if (!targetTable.isPresent()) {
            // Try clicked table first (if it fits capacity)
            if (requestedTableId != null) {
                targetTable = tables.stream()
                        .filter(t -> t.getId() == (int) requestedTableId && t.getLocation() == location &&
                                t.isAvailable(dateTime, guestCount, clientName) &&
                                t.getRemainingSeats(dateTime) >= guestCount)
                        .findFirst();
            }
            // If still nothing, take literally any table that fits the guests
            if (!targetTable.isPresent()) {
                targetTable = tables.stream()
                        .filter(t -> t.getLocation() == location &&
                                t.isAvailable(dateTime, guestCount, clientName) &&
                                t.getRemainingSeats(dateTime) >= guestCount)
                        .findFirst();
            }
        }

        if (targetTable.isPresent()) {
            Table table = targetTable.get();
            Reservation reservation = new Reservation(clientName, guestCount, dateTime, bookingType, table);
            table.addReservation(reservation);
            Database.save(tables);

            int remaining = table.getRemainingSeats(dateTime);
            if (remaining <= 2 && remaining > 0) {
                System.out.println("[Info] Table " + table.getId() + " partially filled. Remaining: " + remaining);
            }
            return true;

        } else {
            throw new IllegalArgumentException("No tables available for " + guestCount + " guests in the " + location + " zone.");
        }
    }

    public boolean cancelReservation(int tableId, LocalDateTime dateTime) {
        Optional<Table> tableOpt = tables.stream().filter(t -> t.getId() == tableId).findFirst();
        if (tableOpt.isPresent()) {
            Table table = tableOpt.get();
            boolean removed = table.reservations.removeIf(r -> r.getDateTime().equals(dateTime));
            if (removed) {
                Database.save(tables);
                return true;
            }
        }
        return false;
    }

    public List<String> listReservations() {
        List<String> list = new ArrayList<>();
        for (Table t : tables) {
            list.addAll(
                    t.reservations.stream()
                            .map(Reservation::toString)
                            .collect(Collectors.toList())
            );
        }
        return list;
    }

    public List<Reservation> findClientReservations(String clientName,
                                                     LocalDateTime from,
                                                     LocalDateTime to) {
        return tables.stream()
                .flatMap(t -> t.reservations.stream())
                .filter(r -> r.getClientName().equalsIgnoreCase(clientName))
                .filter(r -> !r.getDateTime().isBefore(from) && !r.getDateTime().isAfter(to))
                .collect(Collectors.toList());
    }
}
