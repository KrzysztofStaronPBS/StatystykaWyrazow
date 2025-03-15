import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimilarityCalculator {
    public static double cosineSimilarity(Map<String, Long> vec1, Map<String, Long> vec2) {
        Set<String> words = new HashSet<>(vec1.keySet());
        words.addAll(vec2.keySet());

        double dotProduct = 0;
        double normVec1 = 0;
        double normVec2 = 0;

        for (String word : words) {
            long freq1 = vec1.getOrDefault(word, 0L);
            long freq2 = vec2.getOrDefault(word, 0L);
            dotProduct += freq1 * freq2;
            normVec1 += Math.pow(freq1, 2);
            normVec2 += Math.pow(freq2, 2);
        }

        if (normVec1 == 0 || normVec2 == 0) return 0;
        return dotProduct / (Math.sqrt(normVec1) * Math.sqrt(normVec2));
    }
}