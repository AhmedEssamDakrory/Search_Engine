package main.model;

public class ImageSearchResult extends SearchResult {
    private Integer id;
    private String imageUrl;
    private String url;
    private String title;

    public ImageSearchResult(Integer id, String imageUrl, String url, String title, Double score) {
        super(score);
        this.id = id;
        this.imageUrl = imageUrl;
        this.url = url;
        this.title = title;
    }

    @Override
    public Integer getID() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

}
