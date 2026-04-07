package model_rezervari;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

 //reprezintă o masă din restaurant, cu ID, număr de locuri și amplasare.
 //Gestionează rezervările asociate și verifică disponibilitatea în funcție de reguli.

public class AranjareMese {

    private int id;
    private int nrLocuri;
    private Amplasare amplasare;
    /* default */ List<Rezervare> rezervari = new ArrayList<>();

    public AranjareMese(int id, int nrLocuri, Amplasare amplasare) {
        this.id = id;
        this.nrLocuri = nrLocuri;
        this.amplasare = amplasare;
    }

    public boolean esteDisponibila(LocalDateTime dataOra, int nrPersoane, String numeClient) {
        int ocupate = rezervari.stream()
                .filter(r -> r.getDataOra().equals(dataOra))
                .mapToInt(Rezervare::getNrPersoane)
                .sum();

        boolean altClient = rezervari.stream()
                .anyMatch(r -> r.getDataOra().equals(dataOra) &&
                        !r.getNumeClient().equalsIgnoreCase(numeClient));

        return !altClient && (ocupate + nrPersoane <= nrLocuri);
    }

    public void adaugaRezervare(Rezervare rezervare) {
        rezervari.add(rezervare);
    }

    public int getLocuriRamase(LocalDateTime dataOra) {
        int ocupate = rezervari.stream()
                .filter(r -> r.getDataOra().equals(dataOra))
                .mapToInt(Rezervare::getNrPersoane)
                .sum();
        return nrLocuri - ocupate;
    }

    public Amplasare getAmplasare() {
        return amplasare;
    }
    public int getId()             {
        return id;
    }
    public int getCapacitate() {
        return nrLocuri;
    }

    @Override
    public String toString() {
        return "Masa " + id + " (" + amplasare + ", " + nrLocuri + " locuri)";
    }
}
