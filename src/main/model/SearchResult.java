package main.model;

public abstract class SearchResult {
    private transient Double score;

    public SearchResult(Double score) {
        this.score = score;
    }

    public abstract String getUrl();

    public abstract Integer getID();

    public Double getScore()
    {
        return score;
    }
}
