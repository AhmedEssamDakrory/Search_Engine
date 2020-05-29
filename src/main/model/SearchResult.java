package main.model;

public abstract class SearchResult {
    private transient Integer score;

    public SearchResult(Integer score) {
        this.score = score;
    }

    public abstract String getUrl();

    public abstract Integer getID();

    public Integer getScore()
    {
        return score;
    }
}
