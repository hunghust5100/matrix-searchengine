package model;

public class SearchResult {
    private final String id;
    private final String title;
    private final String content;
    private final float score;

    public SearchResult(String id, String title, String content, float score) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.score = score;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public float getScore() { return score; }
}
