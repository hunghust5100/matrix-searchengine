package service;

import vn.pipeline.Annotation;
import vn.pipeline.VnCoreNLP;
import vn.pipeline.Word;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SegmentationService {

    private static SegmentationService instance;
    private final VnCoreNLP pipeline;

    private SegmentationService() {
        try {
            // Khởi tạo VnCoreNLP chỉ với annotator 'wseg'
            this.pipeline = new VnCoreNLP(new String[]{"wseg"});
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo VnCoreNLP: " + e.getMessage(), e);
        }
    }

    public static synchronized SegmentationService getInstance() {
        if (instance == null) {
            instance = new SegmentationService();
        }
        return instance;
    }

    /**
     * Tách từ ghép (_) + chuyển chữ thường + xóa dấu
     */
    public String segmentAndNormalize(String text) {
        if (text == null || text.isBlank()) return "";
        try {
            Annotation annotation = new Annotation(text);
            pipeline.annotate(annotation);

            List<String> words = new ArrayList<>();
            for (Word word : annotation.getWords()) {
                words.add(word.getForm());
            }
            String segmented = String.join(" ", words);
            return removeAccents(segmented.toLowerCase());
        } catch (IOException e) {
            System.err.println("Lỗi tách từ: " + e.getMessage());
            return removeAccents(text.toLowerCase());
        }
    }

    private String removeAccents(String input) {
        if (input == null) return null;
        String text = input.replace("đ", "d").replace("Đ", "D");
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("");
    }
}
