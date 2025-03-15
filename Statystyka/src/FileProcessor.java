import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileProcessor {
    public static Map<String, Long> getLinkedCountedWords(Path path, int wordsLimit) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return reader.lines()
                    .flatMap(line -> Stream.of(line.split("\\s+")))
                    .map(word -> word.replaceAll("[^a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]", ""))
                    .filter(word -> word.length() >= 3)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(wordsLimit)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } catch (IOException e) {
            throw new RuntimeException("Błąd odczytu pliku: " + path, e);
        }
    }
}