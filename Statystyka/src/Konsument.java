import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public class Konsument implements Runnable {
    private final BlockingQueue<Optional<Path>> queue;
    private final int liczbaWyrazowStatystyki;
    private final Optional<Path> plikWzorcowy;

    public Konsument(BlockingQueue<Optional<Path>> queue, int liczbaWyrazowStatystyki, Path plikWzorcowy) {
        this.queue = queue;
        this.liczbaWyrazowStatystyki = liczbaWyrazowStatystyki;
        this.plikWzorcowy = Optional.ofNullable(plikWzorcowy);
    }

    @Override
    public void run() {
        final String name = Thread.currentThread().getName();
        System.out.printf("KONSUMENT %s URUCHOMIONY...%n", name);

        Map<String, Long> wektorWzorcowy = plikWzorcowy
                .map(p -> FileProcessor.getLinkedCountedWords(p, liczbaWyrazowStatystyki))
                .orElse(null);

        try {
            while (true) {
                Optional<Path> optPath = queue.take();

                if (optPath.isEmpty()) {
                    System.out.printf("KONSUMENT %s ZAKOŃCZYŁ PRACĘ%n", name);
                    break;
                }

                Path path = optPath.get();
                System.out.printf("KONSUMENT %s analizuje plik: %s%n", name, path);
                Map<String, Long> wektorPliku = FileProcessor.getLinkedCountedWords(path, liczbaWyrazowStatystyki);

                if (wektorWzorcowy != null) {
                    double similarity = SimilarityCalculator.cosineSimilarity(wektorWzorcowy, wektorPliku);
                    System.out.printf("Podobieństwo do pliku wzorcowego: %.2f%n", similarity);
                } else {
                    System.out.printf("%n===== Statystyki dla pliku: %s =====%n", path.getFileName());
                    wektorPliku.forEach((word, count) ->
                            System.out.printf("KONSUMENT %s: %s - %d%n", name, word, count)
                    );
                    System.out.println();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
