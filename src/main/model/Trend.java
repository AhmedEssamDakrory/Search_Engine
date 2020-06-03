package main.model;

public class Trend {
    private String name;
    private int percentage;

    public Trend(String name, int percentage) {
        this.name = name;
        this.percentage = percentage;
    }

    public String getName() {
        return name;
    }

    public int getPercentage() {
        return percentage;
    }

}
