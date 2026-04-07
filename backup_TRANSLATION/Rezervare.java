package model_rezervari;

import java.time.LocalDateTime;

public class Rezervare {

    private String numeClient;
    private int nrPersoane;
    private LocalDateTime dataOra;
    private SpecificulRezervarii specific;
    private AranjareMese masa;

    public Rezervare(String numeClient, int nrPersoane, LocalDateTime dataOra,
                     SpecificulRezervarii specific, AranjareMese masa) {
        this.numeClient = numeClient;
        this.nrPersoane = nrPersoane;
        this.dataOra    = dataOra;
        this.specific   = specific;
        this.masa       = masa;
    }

    public int getNrPersoane()          { return nrPersoane; }
    public LocalDateTime getDataOra()   { return dataOra; }
    public String getNumeClient()       { return numeClient; }
    public AranjareMese getMasa()       { return masa; }
    public SpecificulRezervarii getSpecific() { return specific; } // ← ADĂUGAT

    @Override
    public String toString() {
        return "Rezervare pentru " + numeClient + " la masa " + masa.getId() +
                " (" + masa.getAmplasare() + ") pentru " + nrPersoane +
                " persoane, la " + dataOra + ", specific: " + specific;
    }

}