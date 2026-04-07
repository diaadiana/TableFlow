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

    private List<AranjareMese> mese;

    public Restaurant(List<AranjareMese> mese) {
        this.mese = mese;
    }

    public boolean faRezervare(String numeClient,
                               int nrPersoane,
                               LocalDateTime dataOra,
                               Amplasare amplasare,
                               SpecificulRezervarii specific,
                               Integer masaIdCautata) {

        java.util.function.Predicate<AranjareMese> isOptimized = m -> {
            int cap = m.getCapacitate();
            if (nrPersoane == 1 && cap > 2) return false;
            if (nrPersoane == 3 && cap > 4) return false;
            // Standard rule: don't leave more than 1 seat empty if possible
            if (cap - nrPersoane > 1) return false;
            return true;
        };

        Optional<AranjareMese> targetMasa = Optional.empty();

        // 1. Try to find a PERFECTLY OPTIMIZED table in the requested location
        // Priority to the one clicked if it's optimized and available
        if (masaIdCautata != null) {
            targetMasa = mese.stream()
                    .filter(m -> m.getId() == (int)masaIdCautata && m.getAmplasare() == amplasare &&
                            isOptimized.test(m) && m.esteDisponibila(dataOra, nrPersoane, numeClient) &&
                            m.getLocuriRamase(dataOra) >= nrPersoane)
                    .findFirst();
        }

        // 2. If not found, look for ANY other optimized table in that location
        if (!targetMasa.isPresent()) {
            targetMasa = mese.stream()
                    .filter(m -> m.getAmplasare() == amplasare &&
                            isOptimized.test(m) && m.esteDisponibila(dataOra, nrPersoane, numeClient) &&
                            m.getLocuriRamase(dataOra) >= nrPersoane)
                    .findFirst();
        }

        // 3. Last Resort: If no *optimized* table found, take ANY available table that fits the guests 
        // to avoid throwing an error to the user.
        if (!targetMasa.isPresent()) {
            // Try clicked table first (if it fits capacity)
            if (masaIdCautata != null) {
                targetMasa = mese.stream()
                    .filter(m -> m.getId() == (int)masaIdCautata && m.getAmplasare() == amplasare &&
                            m.esteDisponibila(dataOra, nrPersoane, numeClient) &&
                            m.getLocuriRamase(dataOra) >= nrPersoane)
                    .findFirst();
            }
            // If still nothing, take literally any table that fits the guests
            if (!targetMasa.isPresent()) {
                targetMasa = mese.stream()
                    .filter(m -> m.getAmplasare() == amplasare &&
                            m.esteDisponibila(dataOra, nrPersoane, numeClient) &&
                            m.getLocuriRamase(dataOra) >= nrPersoane)
                    .findFirst();
            }
        }

        if (targetMasa.isPresent()) {
            AranjareMese masa = targetMasa.get();
            Rezervare rezervare = new Rezervare(numeClient, nrPersoane, dataOra, specific, masa);
            masa.adaugaRezervare(rezervare);
            Database.save(mese); // Save update

            int ramaseDupa = masa.getLocuriRamase(dataOra);
            if (ramaseDupa <= 2 && ramaseDupa > 0) {
                System.out.println("[Info] Table " + masa.getId() + " partially filled. Remaining: " + ramaseDupa);
            }
            return true;

        } else {
            throw new IllegalArgumentException("No tables available for " + nrPersoane + " guests in the " + amplasare + " zone.");
        }
    }

    public boolean anuleazaRezervare(int masaId, LocalDateTime dataOra) {
        Optional<AranjareMese> masaOpt = mese.stream().filter(m -> m.getId() == masaId).findFirst();
        if (masaOpt.isPresent()) {
            AranjareMese masa = masaOpt.get();
            boolean removed = masa.rezervari.removeIf(r -> r.getDataOra().equals(dataOra));
            if (removed) {
                Database.save(mese);
                return true;
            }
        }
        return false;
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
