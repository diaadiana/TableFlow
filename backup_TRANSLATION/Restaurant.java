package model_rezervari;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//gestionează mesele și rezervările.
//Permite efectuarea rezervărilor, afișarea tuturor rezervărilor,
//și căutarea rezervărilor unui client într-un interval de timp.

public class Restaurant {

    private List<AranjareMese> mese;

    public Restaurant(List<AranjareMese> mese) {
        this.mese = mese;
    }

    public boolean faRezervare(String numeClient,
                               int nrPersoane,
                               LocalDateTime dataOra,
                               Amplasare amplasare,
                               SpecificulRezervarii specific) {

        Optional<AranjareMese> masaOpt = mese.stream()
                .filter(m -> m.getAmplasare() == amplasare &&
                        m.esteDisponibila(dataOra, nrPersoane, numeClient))
                .findFirst();

        if (masaOpt.isPresent()) {
            AranjareMese masa = masaOpt.get();

            if (masa.getLocuriRamase(dataOra) < nrPersoane) {
                System.out.println("[Eroare] Nu sunt suficiente locuri libere la masa aleasă.\n");
                return false;
            }

            Rezervare rezervare = new Rezervare(numeClient, nrPersoane, dataOra, specific, masa);
            masa.adaugaRezervare(rezervare);

            int ramaseDupa = masa.getLocuriRamase(dataOra);
            if (ramaseDupa <= 2 && ramaseDupa > 0) {
                System.out.println("[Info] Masa " + masa.getId() + " (" +
                        masa.getCapacitate() + " locuri) blocată pentru rezervări ulterioare (" +
                        ramaseDupa + " locuri rămase).\n");
            }
            return true;

        } else {
            System.out.println("[Eroare] Rezervare imposibilă: masa este ocupată de alt client sau nu are locuri suficiente.\n");
            return false;
        }
    }

    public List<String> afiseazaRezervari() {
        List<String> lista = new ArrayList<>();
        for (AranjareMese m : mese) {
            lista.addAll(
                    m.rezervari.stream()
                            .map(Rezervare::toString)
                            .collect(Collectors.toList())
            );
        }
        return lista;
    }

    public List<Rezervare> cautaRezervariClient(String numeClient,
                                                LocalDateTime deLa,
                                                LocalDateTime panaLa) {
        return mese.stream()
                .flatMap(m -> m.rezervari.stream())
                .filter(r -> r.getNumeClient().equalsIgnoreCase(numeClient))
                .filter(r -> !r.getDataOra().isBefore(deLa) && !r.getDataOra().isAfter(panaLa))
                .collect(Collectors.toList());
    }
}
