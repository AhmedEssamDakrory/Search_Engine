package main.ranker;

public class PageRank {
    protected int dim;
    protected double[][] adjList;
    protected double[] pageRank;

    public PageRank(int m)
    {
        dim = m;
        adjList = new double[m][m];
        pageRank = new double[m];
    }

    // returns whether the filling process was successful or not
    public boolean fillAdjList(double[][] data)
    {
        if (data.length != dim)
        {
            return false;
        }
        for (int i = 0; i < dim; ++i) {
            if (data[i].length != adjList[i].length)
            {
                return false;
            }
            System.arraycopy(data[i], 0, adjList[i], 0, dim);
        }
        return true;
    }

    public double[] updateRank(Integer maxIter, Double damp)
    {
        if (maxIter == null)
        {
            maxIter = 100;
        }
        if (damp == null)
        {
            damp = 0.85;
        }

        double[][] modifiedAdjList = new double[dim][dim];
        for (int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                modifiedAdjList[i][j] = damp * adjList[i][j] + ((1 - damp) / dim);
            }
        }

        for (int i = 0; i < dim; ++i)
        {
            pageRank[i] = 1.0 / dim;
        }
        double[] tmp = new double[dim];
        for (int k = 0; k < maxIter; ++k)
        {
            for (int i = 0; i < dim; ++i) {
                tmp[i] = 0;
                for (int j = 0; j < dim; ++j) {
                    tmp[i] += modifiedAdjList[i][j] * pageRank[j];
                }
            }
            System.arraycopy(tmp, 0, pageRank, 0, dim);
        }
        return pageRank;
    }

    public void print()
    {
        double sum = 0;
        for (int i = 0; i < dim; i++) {
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
            double[] finalRank = pr.updateRank(100, 0.85);

            double sum = 0;
            for (double v : finalRank) {
                System.out.println(v);
                sum += v;
            }
            System.out.println(sum);

            pr.print();
        }
    }
}
