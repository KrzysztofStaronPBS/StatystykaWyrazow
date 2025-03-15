import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Producent implements Runnable {
    private final String directory;
    private final BlockingQueue<Optional<Path>> queue;
    private final AtomicBoolean fajrant;
    private final int liczbaKonsumentow;

    public Producent(String directory, BlockingQueue<Optional<Path>> queue, AtomicBoolean fajrant, int liczbaKonsumentow) {
        this.directory = directory;
        this.queue = queue;
        this.fajrant = fajrant;
        this.liczbaKonsumentow = liczbaKonsumentow;
    }

    @Override
    public void run() {
        final String name = Thread.currentThread().getName();
        System.out.printf("PRODUCENT %s URUCHOMIONY...%n", name);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (fajrant.get()) {
                    for (int i = 0; i < liczbaKonsumentow; i++) {
                        queue.put(Optional.empty()); // "Poison pill"
                    }
                    break;
                }

                Path dir = Paths.get(directory);
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.txt");

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (matcher.matches(file.getFileName())) {
                            try {
                                queue.put(Optional.of(file));
                                System.out.printf("Producent %s dodaje plik: %s%n", name, file);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                Thread.sleep(60000);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("PRODUCENT %s ZAKOŃCZYŁ PRACĘ%n", name);
    }
}
