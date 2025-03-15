import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainFrame {
    private JFrame frame;
    private static final String DIR_PATH = "files";
    private Path plikWzorcowy = null;  // Ścieżka do pliku wzorcowego
    private final int liczbaWyrazowStatystyki = 10;
    private final int liczbaProducentow = 1;
    private final int liczbaKonsumentow = 2;
    private final AtomicBoolean fajrant = new AtomicBoolean(false);
    private final ExecutorService executor;

    private final List<Future<?>> konsumentFuture = new ArrayList<>();
    private final List<Future<?>> producentFuture = new ArrayList<>();

    private BlockingQueue<Optional<Path>> kolejka;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            MainFrame window = new MainFrame();
            window.frame.setVisible(true);
        });
    }

    public MainFrame() {
        executor = Executors.newFixedThreadPool(liczbaProducentow + liczbaKonsumentow);
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Analiza tekstu");
        frame.setSize(450, 70);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });

        JPanel panel = new JPanel();
        JButton btnWybierzPlik = new JButton("Wybierz plik wzorcowy");
        JButton btnStart = new JButton("Start");
        JButton btnStop = new JButton("Stop");
        JButton btnZamknij = new JButton("Zamknij");

        btnWybierzPlik.addActionListener(e -> wybierzPlikWzorcowy());
        btnStart.addActionListener(e -> multiThreadedStatistics());
        btnStop.addActionListener(e -> stopProcessing());
        btnZamknij.addActionListener(e -> {
            executor.shutdownNow();
            System.exit(0);
        });

        panel.add(btnWybierzPlik);
        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnZamknij);
        frame.add(panel, BorderLayout.NORTH);
    }

    private void wybierzPlikWzorcowy() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            plikWzorcowy = fileChooser.getSelectedFile().toPath();
            JOptionPane.showMessageDialog(frame, "Wybrano plik wzorcowy: " + plikWzorcowy);
        }
    }

    private void multiThreadedStatistics() {
        if (!producentFuture.isEmpty() && producentFuture.stream().anyMatch(f -> !f.isDone())) {
            JOptionPane.showMessageDialog(frame, "Nie można uruchomić nowego zadania! " +
                    "Przynajmniej jeden producent nadal działa!", "OSTRZEŻENIE", JOptionPane.WARNING_MESSAGE);
            return;
        }

        kolejka = new LinkedBlockingQueue<>(liczbaKonsumentow);
        fajrant.set(false);

        producentFuture.clear();
        konsumentFuture.clear();  // Czyścimy listę konsumentów przed ponownym uruchomieniem

        // Uruchomienie PRODUCENTÓW
        for (int i = 0; i < liczbaProducentow; i++) {
            producentFuture.add(executor.submit(new Producent(DIR_PATH, kolejka, fajrant, liczbaKonsumentow)));
        }

        // Uruchomienie KONSUMENTÓW
        for (int i = 0; i < liczbaKonsumentow; i++) {
            konsumentFuture.add(executor.submit(new Konsument(kolejka, liczbaWyrazowStatystyki, plikWzorcowy)));
        }
    }

    private void stopProcessing() {
        fajrant.set(true);

        // Zatrzymanie producentów
        producentFuture.forEach(f -> f.cancel(true));

        // Wysłanie "poison pill" do każdego konsumenta
        for (int i = 0; i < liczbaKonsumentow; i++) {
            try {
                kolejka.put(Optional.empty());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Zatrzymanie konsumentów
        konsumentFuture.forEach(f -> f.cancel(true));

        plikWzorcowy = null;
    }
}