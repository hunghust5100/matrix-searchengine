import vn.pipeline.Annotation;
import vn.pipeline.VnCoreNLP;
import vn.pipeline.Word;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class TachTu {

    public static void main(String[] args) {
        // Tự động xác định đường dẫn thư mục models
        String modelsPath = "models";
        if (!new File(modelsPath).exists() && new File("btt/models").exists()) {
            modelsPath = "btt/models";
        }

        System.out.println("Đang khởi tạo VnCoreNLP pipeline (vui lòng chờ vài giây)...");

        VnCoreNLP pipeline;
        try {
            // Chỉ nạp annotator 'wseg' để tách từ (tiết kiệm RAM & chạy nhanh nhất)
            String[] annotators = {"wseg"};
            pipeline = new VnCoreNLP(annotators);
            System.out.println("-> Khởi tạo pipeline thành công!\n");
        } catch (IOException e) {
            System.err.println("--- CHI TIẾT LỖI GỐC ---");
            e.printStackTrace(); // In chính xác lỗi của thư viện
            return;
        }

        // Đọc dữ liệu từ bàn phím (Console)
        Scanner scanner = new Scanner(System.in, "UTF-8");
        System.out.println("=== CHƯƠNG TRÌNH TÁCH TỪ TIẾNG VIỆT ===");
        System.out.println("Nhập văn bản cần tách từ (hoặc gõ 'exit' để thoát):");

        while (true) {
            System.out.print("\n[Input] > ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Kết thúc chương trình.");
                break;
            }

            if (input.isEmpty()) {
                System.out.println("Văn bản trống, vui lòng nhập lại!");
                continue;
            }

            try {
                // 1. Chạy pipeline tách từ
                Annotation annotation = new Annotation(input);
                pipeline.annotate(annotation);

                // 2. Gom các từ lại với nhau
                List<String> words = new ArrayList<>();
                for (Word word : annotation.getWords()) {
                    words.add(word.getForm());
                }
                String segmentedText = String.join(" ", words);

                // 3. Chuẩn hóa: viết thường và xóa dấu
                String normalizedText = removeAccents(segmentedText.toLowerCase());

                // 4. In kết quả ra console
                System.out.println("\n--- KẾT QUẢ ---");
                System.out.println("1. Văn bản gốc: " + input);
                System.out.println("2. Tách từ:     " + segmentedText);
                System.out.println("3. Xóa dấu:     " + normalizedText);

            } catch (IOException e) {
                System.err.println("Lỗi trong quá trình tách từ: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * Hàm loại bỏ dấu tiếng Việt bằng chuẩn Unicode NFD
     */
    public static String removeAccents(String input) {
        if (input == null) return null;

        String text = input.replace("đ", "d").replace("Đ", "D");
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("");
    }
}