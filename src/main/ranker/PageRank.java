package main.ranker;

public class PageRank {
    protected int m;
    protected double[][] adjList;
    protected double[] pageRank;

    public PageRank(int m)
    {
        this.m = m;
        adjList = new double[m][m];
        pageRank = new double[m];
    }

    // returns whether the filling process was successful or not
    public boolean fillAdjList(double[][] data)
    {
        if (data.length != m)
        {
            return false;
        }
        for (int i = 0; i < m; ++i) {
            System.arraycopy(data[i], 0, adjList[i], 0, m);
        }
        return true;
    }

    public void updateRank(Integer maxIter, Double damp)
    {
        if (maxIter == null)
        {
            maxIter = 100;
        }
        if (damp == null)
        {
            damp = 0.85;
        }

        double[][] modifiedAdjList = new double[m][m];
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < m; ++j) {
                modifiedAdjList[i][j] = damp * adjList[i][j] + ((1 - damp) / m);
            }
        }

        for (int i = 0; i < m; ++i)
        {
            pageRank[i] = 1.0 / m;
        }
        double[] tmp = new double[m];
        for (int k = 0; k < maxIter; ++k)
        {
            for (int i = 0; i < m; ++i) {
                tmp[i] = 0;
                for (int j = 0; j < m; ++j) {
                    tmp[i] += modifiedAdjList[i][j] * pageRank[j];
                }
            }
            System.arraycopy(tmp, 0, pageRank, 0, m);
        }
    }

    public void print()
    {
        double sum = 0;
        for (int i = 0; i < m; i++) {
            System.out.println(pageRank[i]);
            sum += pageRank[i];
        }
        System.out.println(sum);
    }

    public static void main(String[] args)
    {
        PageRank pr = new PageRank(5);

        double[][] data = {{0, 0, 0, 0, 1}, {0.5, 0, 0, 0, 0}, {0.5, 0, 0, 0, 0}, {0, 1, 0.5, 0, 0}, {0, 0, 0.5, 1, 0}};
        if (!pr.fillAdjList(data))
        {
            System.out.println("Failed to fill adjacency list");
        }
        else
        {
            pr.updateRank(100, 0.85);

            pr.print();
        }
    }
}
