package main.model;

public class TextSearchResult extends SearchResult {
    private Integer id;
    private String url;
    private String iconUrl;
    private String title;
    private String description;

    public TextSearchResult(Integer id, String url, String iconUrl, String title, String description, Integer score) {
        super(score);
        this.id = id;
        this.url = url;
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
    }

    @Override
    public Integer getID() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
