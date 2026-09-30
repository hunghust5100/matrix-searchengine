# HUST Matrix Search Engine 🔍

Hệ thống tìm kiếm và xếp hạng thông tin (Information Retrieval - IR) tiếng Việt được xây dựng bằng **Java**, sử dụng thư viện **Apache Lucene** kết hợp mô hình không gian vector **TF-IDF** (`ClassicSimilarity`) và bộ tách từ tiếng Việt **VnCoreNLP**.

---

## 🌟 Tính năng nổi bật

- ⚡ **Lập chỉ mục thông minh (Incremental Indexing)**: Tự động so sánh thời gian sửa đổi (`lastModified`) của các tài liệu trong thư mục `data/` với chỉ mục hiện có. Chỉ đánh chỉ mục tài liệu mới hoặc có thay đổi nội dung.
- 🇻🇳 **Xử lý tiếng Việt chuyên sâu**: Tích hợp **VnCoreNLP** (`wseg`) để tách từ ghép (ví dụ: `đại_học`, `bách_khoa`), kết hợp chuẩn hóa Unicode NFD loại bỏ dấu thanh, cho phép tìm kiếm chính xác kể cả khi người dùng gõ có dấu hoặc không dấu.
- 📊 **Mô hình xếp hạng TF-IDF**: Sử dụng `ClassicSimilarity` của Apache Lucene để tính điểm tương quan giữa từ khóa truy vấn và tập tài liệu, xếp hạng và trả về Top K kết quả phù hợp nhất.
- 🖥️ **CLI tương tác trực quan**: Giao diện dòng lệnh dễ sử dụng, hiển thị ID bài viết, tiêu đề, điểm số và đoạn trích dẫn (snippet).

---

## 📁 Cấu trúc dự án

```text
btt/
├── data/                               # Dữ liệu tài liệu văn bản đầu vào (doc1, doc2, ...)
├── libs/                               # Thư viện ngoài & mô hình nạp offline
│   ├── VnCoreNLP-1.2.jar               # Thư viện VnCoreNLP
│   └── models/wordsegmenter/           # Model từ điển phục vụ tách từ (vi-vocab, wordsegmenter.rdr)
├── src/
│   ├── Main.java                       # Điểm vào chính của ứng dụng tìm kiếm CLI
│   ├── TachTu.java                     # Công cụ demo/kiểm thử tính năng tách từ VnCoreNLP
│   ├── model/
│   │   └── SearchResult.java           # DTO lưu trữ kết quả tìm kiếm (ID, tiêu đề, trích dẫn, score)
│   └── service/
│       ├── SearchService.java          # Tầng dịch vụ quản lý Lucene (Index, Search, TF-IDF)
│       └── SegmentationService.java    # Singleton bọc VnCoreNLP & chuẩn hóa tiếng Việt
├── pom.xml                             # Cấu hình dự án Maven & khai báo dependencies
└── README.md                           # Tài liệu hướng dẫn sử dụng
```

---

## 🛠️ Yêu cầu môi trường

- **Java SDK**: Phiên bản 17 trở lên.
- **Apache Maven**: Phiên bản 3.8+ trở lên.

---

## 🚀 Hướng dẫn cài đặt và sử dụng

### 1. Chạy hệ thống tìm kiếm chính

Mở Terminal tại thư mục `btt/` và chạy lệnh:

```bash
mvn compile exec:exec
```

> **Lưu ý**: Lệnh này sẽ tự động biên dịch mã nguồn và khởi chạy chương trình thông qua cấu hình `exec-maven-plugin` đã được thiết lập sẵn trong `pom.xml`.

**Ví dụ phiên tìm kiếm:**
```text
1. Khởi tạo SearchService (TF-IDF ClassicSimilarity & VnCoreNLP)...
2. Kiểm tra cập nhật tài liệu trong thư mục 'data'...
-> Tất cả 10 tài liệu đều đã được lập chỉ mục.
-> Không có nội dung mới cần đánh chỉ mục.
-> Tổng số tài liệu trong index: 10

============================================================
     HỆ THỐNG TÌM KIẾM THÔNG TIN HUST (TF-IDF & VnCoreNLP)    
============================================================
Nhập từ khóa tìm kiếm (hoặc gõ 'exit' / 'quit' để thoát).

[Tìm kiếm] > Bách khoa

--- BẢNG XẾP HẠNG KẾT QUẢ THEO TF-IDF (Top 5) ---
[1] (ID:Score) doc2:0.2176
    Tiêu đề : Điểm chuẩn Đại học Bách khoa Hà Nội năm 2026
    Nội dung: Chiều nay (9/8), Đại học Bách khoa Hà Nội công bố điểm chuẩn...
------------------------------------------------------------
[2] (ID:Score) d10:0.2148
    Tiêu đề : Điện Biên và Đại học Bách khoa Hà Nội hợp tác đào tạo...
    Nội dung: Ngày 28/3, tại trung tâm thành phố Điện Biên Phủ...
------------------------------------------------------------
...
```

---

### 2. Chạy công cụ thử nghiệm tách từ độc lập

Nếu muốn kiểm tra riêng module tách từ và xóa dấu tiếng Việt của VnCoreNLP:

```bash
mvn compile
java -cp "target/classes:libs/VnCoreNLP-1.2.jar" TachTu
```

**Ví dụ:**
```text
[Input] > Đại học Bách Khoa Hà Nội

--- KẾT QUẢ ---
1. Văn bản gốc: Đại học Bách Khoa Hà Nội
2. Tách từ:     Đại_học Bách_Khoa Hà_Nội
3. Xóa dấu:     dai_hoc bach_khoa ha_noi
```

---

## ⚙️ Thư viện sử dụng

- [Apache Lucene Core & QueryParser 9.10.0](https://lucene.apache.org/) - Đánh chỉ mục và truy vấn toàn văn.
- [VnCoreNLP 1.2](https://github.com/vncorenlp/VnCoreNLP) - Bộ công cụ tiền xử lý ngôn ngữ tự nhiên tiếng Việt.
