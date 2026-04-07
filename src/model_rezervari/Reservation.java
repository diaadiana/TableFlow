package model_rezervari;

import java.time.LocalDateTime;

public class Reservation {

    private String clientName;
    private int guestCount;
    private LocalDateTime dateTime;
    private BookingType bookingType;
    private Table table;

    public Reservation(String clientName, int guestCount, LocalDateTime dateTime,
                       BookingType bookingType, Table table) {
        this.clientName  = clientName;
        this.guestCount  = guestCount;
        this.dateTime    = dateTime;
        this.bookingType = bookingType;
        this.table       = table;
    }

    public int getGuestCount()          { return guestCount; }
    public LocalDateTime getDateTime()  { return dateTime; }
    public String getClientName()       { return clientName; }
    public Table getTable()             { return table; }
    public BookingType getBookingType() { return bookingType; }

    @Override
    public String toString() {
        return "Reservation for " + clientName + " at table " + table.getId() +
                " (" + table.getLocation() + ") for " + guestCount +
                " guests, at " + dateTime + ", type: " + bookingType;
    }
}