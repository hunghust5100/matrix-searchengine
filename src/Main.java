import model.SearchResult;
import service.SearchService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            // 1. Tự động xác định thư mục dữ liệu 'data' (hỗ trợ cả chạy từ root Haha hoặc btt)
            File dataFolder = new File("data");
            if (!dataFolder.exists() && new File("btt/data").exists()) {
                dataFolder = new File("btt/data");
            }

            if (!dataFolder.exists() || !dataFolder.isDirectory()) {
                System.err.println("Lỗi: Không tìm thấy thư mục dữ liệu 'data' hoặc 'btt/data' trong dự án!");
                return;
            }

            File[] files = dataFolder.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
            if (files == null || files.length == 0) {
                System.err.println("Lỗi: Thư mục '" + dataFolder.getPath() + "' không chứa bài viết nào!");
                return;
            }

            // Tự động xác định đường dẫn index tương ứng với thư mục data
            java.nio.file.Path indexPath = dataFolder.getPath().startsWith("btt")
                    ? java.nio.file.Paths.get("btt/index")
                    : java.nio.file.Paths.get("index");

            System.out.println("1. Khởi tạo SearchService (TF-IDF ClassicSimilarity & VnCoreNLP)...");
            SearchService searchService = new SearchService(indexPath);

            System.out.println("2. Kiểm tra cập nhật tài liệu trong thư mục '" + dataFolder.getPath() + "'...");
            Map<String, Long> indexedDocs = searchService.getIndexedDocumentsWithTimestamp();
            List<File> toIndex = new ArrayList<>();

            for (File f : files) {
                String docId = f.getName();
                Long indexedTime = indexedDocs.get(docId);
                // Nếu chưa có trong index hoặc file trên đĩa mới hơn thời điểm lập chỉ mục
                if (indexedTime == null || f.lastModified() > indexedTime) {
                    toIndex.add(f);
                }
            }

            if (toIndex.isEmpty()) {
                System.out.println("-> Tất cả " + files.length + " tài liệu đều đã được lập chỉ mục.");
                System.out.println("-> Không có nội dung mới cần đánh chỉ mục.");
                System.out.println("-> Tổng số tài liệu trong index: " + searchService.getTotalDocuments() + "\n");
            } else {
                System.out.println("-> Phát hiện " + toIndex.size() + " tài liệu mới/cập nhật. Đang tiến hành tạo index...");
                for (File f : toIndex) {
                    List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                    if (lines.isEmpty()) continue;
                    String title = lines.get(0).trim();
                    String content = lines.size() > 1 ? String.join("\n", lines.subList(1, lines.size())).trim() : title;

                    searchService.indexDocument(f.getName(), title, content, f.lastModified());
                    System.out.println("   [+] Đã đánh chỉ mục: " + f.getName() + " - " + title);
                }

                // Ghi dữ liệu index vào đĩa
                searchService.commit();
                System.out.println("-> Hoàn tất lập chỉ mục cho các tài liệu mới!");
                System.out.println("-> Tổng số tài liệu hiện có trong index: " + searchService.getTotalDocuments() + "\n");
            }

            // 3. Vòng lặp tìm kiếm tương tác và xếp hạng theo TF-IDF
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
            System.out.println("============================================================");
            System.out.println("     HỆ THỐNG TÌM KIẾM THÔNG TIN HUST (TF-IDF & VnCoreNLP)    ");
            System.out.println("============================================================");
            System.out.println("Nhập từ khóa tìm kiếm (hoặc gõ 'exit' / 'quit' để thoát).\n");

            while (true) {
                System.out.print("[Tìm kiếm] > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String query = scanner.nextLine().trim();

                if (query.equalsIgnoreCase("exit") || query.equalsIgnoreCase("quit")) {
                    System.out.println("Kết thúc chương trình tìm kiếm.");
                    break;
                }

                if (query.isEmpty()) {
                    System.out.println("Vui lòng nhập từ khóa tìm kiếm!\n");
                    continue;
                }

                List<SearchResult> results = searchService.search(query, 5);

                if (results.isEmpty()) {
                    System.out.println("Không tìm thấy tài liệu phù hợp với truy vấn: \"" + query + "\"\n");
                    continue;
                }

                System.out.println("\n--- BẢNG XẾP HẠNG KẾT QUẢ THEO TF-IDF (Top " + results.size() + ") ---");
                for (int i = 0; i < results.size(); i++) {
                    SearchResult r = results.get(i);
                    System.out.printf("[%d] (ID:Score) %s:%.4f\n", i + 1, r.getId(), r.getScore());
                    System.out.println("    Tiêu đề : " + r.getTitle());
                    String snippet = r.getContent();
                    if (snippet.length() > 250) {
                        snippet = snippet.substring(0, 250) + "...";
                    }
                    System.out.println("    Nội dung: " + snippet);
                    System.out.println("------------------------------------------------------------");
                }
                System.out.println();
            }

            scanner.close();
            searchService.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
