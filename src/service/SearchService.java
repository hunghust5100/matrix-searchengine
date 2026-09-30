package service;

import model.SearchResult;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchService implements AutoCloseable {

    private final Path indexPath;
    private final Directory directory;
    private final WhitespaceAnalyzer analyzer;
    private final SegmentationService segmentationService;
    private IndexWriter writer;

    public SearchService() throws IOException {
        this(resolveDefaultIndexPath());
    }

    private static Path resolveDefaultIndexPath() {
        if (!new File("data").exists() && new File("btt/data").exists()) {
            return Paths.get("btt/index");
        }
        return Paths.get("index");
    }

    public SearchService(Path indexPath) throws IOException {
        this.indexPath = indexPath;
        this.directory = FSDirectory.open(indexPath);
        this.analyzer = new WhitespaceAnalyzer();
        this.segmentationService = SegmentationService.getInstance();
    }

    private synchronized void openWriterIfNeeded() throws IOException {
        if (writer == null || !writer.isOpen()) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new ClassicSimilarity());
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            this.writer = new IndexWriter(directory, config);
        }
    }

    /**
     * Lấy danh sách ID tài liệu đã có trong chỉ mục kèm theo timestamp sửa đổi cuối cùng
     */
    public Map<String, Long> getIndexedDocumentsWithTimestamp() throws IOException {
        Map<String, Long> map = new HashMap<>();
        if (!DirectoryReader.indexExists(directory)) {
            return map;
        }

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            Bits liveDocs = MultiBits.getLiveDocs(reader);
            for (int i = 0; i < reader.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }
                Document doc = reader.document(i);
                String id = doc.get("id");
                if (id != null) {
                    Number num = doc.getField("last_modified") != null ? doc.getField("last_modified").numericValue() : null;
                    long lm = num != null ? num.longValue() : 0L;
                    map.put(id, lm);
                }
            }
        }
        return map;
    }

    /**
     * Trả về tổng số tài liệu hiện có trong index
     */
    public int getTotalDocuments() throws IOException {
        if (!DirectoryReader.indexExists(directory)) {
            return 0;
        }
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            return reader.numDocs();
        }
    }

    /**
     * Đánh chỉ mục / cập nhật tài liệu vào Lucene
     */
    public void indexDocument(String id, String title, String content, long lastModified) throws IOException {
        openWriterIfNeeded();

        // Tách từ và chuẩn hóa nội dung lẫn tiêu đề trước khi lập chỉ mục
        String normalizedTitle = segmentationService.segmentAndNormalize(title);
        String normalizedContent = segmentationService.segmentAndNormalize(content);

        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StoredField("last_modified", lastModified));
        doc.add(new TextField("raw_title", title, Field.Store.YES));
        doc.add(new TextField("raw_content", content, Field.Store.YES));

        // Trường dùng để tính điểm TF-IDF khi tìm kiếm
        doc.add(new TextField("search_text", normalizedTitle + " " + normalizedContent, Field.Store.NO));

        // Cập nhật hoặc thêm mới nếu chưa tồn tại
        writer.updateDocument(new Term("id", id), doc);
    }

    public void indexDocument(String id, String title, String content) throws IOException {
        indexDocument(id, title, content, System.currentTimeMillis());
    }

    public synchronized void commit() throws IOException {
        if (writer != null && writer.isOpen()) {
            writer.commit();
            writer.close();
            writer = null;
        }
    }

    /**
     * Tìm kiếm văn bản bằng mô hình TF-IDF (ClassicSimilarity)
     */
    public List<SearchResult> search(String queryStr, int topK) throws Exception {
        if (!DirectoryReader.indexExists(directory)) {
            return new ArrayList<>();
        }

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new ClassicSimilarity());

            // Chuẩn hóa query đầu vào bằng VnCoreNLP
            String processedQuery = segmentationService.segmentAndNormalize(queryStr);
            if (processedQuery.isBlank()) {
                return new ArrayList<>();
            }

            QueryParser parser = new QueryParser("search_text", analyzer);
            Query query;
            try {
                query = parser.parse(processedQuery);
            } catch (Exception e) {
                query = parser.parse(QueryParser.escape(processedQuery));
            }

            TopDocs hits = searcher.search(query, topK);
            List<SearchResult> results = new ArrayList<>();

            for (ScoreDoc sd : hits.scoreDocs) {
                Document hitDoc = searcher.doc(sd.doc);
                results.add(new SearchResult(
                        hitDoc.get("id"),
                        hitDoc.get("raw_title"),
                        hitDoc.get("raw_content"),
                        sd.score
                ));
            }

            return results;
        }
    }

    @Override
    public void close() throws IOException {
        commit();
        if (directory != null) {
            directory.close();
        }
    }
}
