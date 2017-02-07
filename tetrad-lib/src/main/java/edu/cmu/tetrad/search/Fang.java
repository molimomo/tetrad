///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import org.apache.commons.math3.distribution.TDistribution;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.tetrad.util.StatUtils.mean;
import static edu.cmu.tetrad.util.StatUtils.sd;
import static java.lang.Math.*;

/**
 * Fast adjacency search followed by robust skew orientation. Checks are done for adding
 * two-cycles. The two-cycle checks do not require non-Gaussianity. The robust skew
 * orientation of edges left or right does.
 *
 * @author Joseph Ramsey
 */
public final class Fang implements GraphSearch {

    // Elapsed time of the search, in milliseconds.
    private long elapsed = 0;

    // The data sets being analyzed. They must all have the same variables and the same
    // number of records.
    private List<DataSet> dataSets = null;

    // nonGaussian[i] is true iff the i'th variable is judged non-Gaussian by the
    // Anderson-Darling test.
    private boolean[] nonGaussian;

    // For the Fast Adjacency Search.
    private int depth = 5;

    // For the SEM BIC score, for the Fast Adjacency Search.
    private double penaltyDiscount = 6;

    // For the T tests of equality and the Anderson-Darling tests.
    private double alpha = 0.05;

    public Fang(List<DataSet> dataSets) {
        this.dataSets = dataSets;
    }

    //======================================== PUBLIC METHODS ====================================//

    /**
     * Runs the search on the concatenated data, returning a graph, possibly cyclic, possibly with
     * two-cycles.
     */
    public Graph search() {
        long start = System.currentTimeMillis();

        List<DataSet> _dataSets = new ArrayList<>();
        for (DataSet dataSet : dataSets) _dataSets.add(DataUtils.standardizeData(dataSet));

        DataSet dataSet = DataUtils.concatenate(_dataSets);

        SemBicScore score = new SemBicScore(new CovarianceMatrix(dataSet));
        score.setPenaltyDiscount(penaltyDiscount);
        IndependenceTest test = new IndTestScore(score, dataSet);

        final ICovarianceMatrix cov = new CovarianceMatrix(dataSet);
        List<Node> variables = cov.getVariables();
        Graph graph = new EdgeListGraph(variables);

        double[][] colData = dataSet.getDoubleData().transpose().toArray();

        nonGaussian = new boolean[colData.length];

        for (int i = 0; i < colData.length; i++) {
            nonGaussian[i] = new AndersonDarlingTest(colData[i]).getP() < alpha;
        }

        final int n = dataSet.getNumRows();
        double T = abs(new TDistribution(n).inverseCumulativeProbability(alpha / 2.0));

        FasStableConcurrent fas = new FasStableConcurrent(null, test);
        fas.setDepth(getDepth());
        fas.setVerbose(false);
        Graph graph0 = fas.search();

        for (int i = 0; i < variables.size(); i++) {
            for (int j = i + 1; j < variables.size(); j++) {
                if (i == j) continue;

                final double[] xData = colData[i];
                final double[] yData = colData[j];

                double[] xxg = new double[xData.length];
                double[] yyg = new double[yData.length];
                double[] xxh = new double[xData.length];
                double[] yyh = new double[yData.length];

                for (int k = 0; k < xxg.length; k++) {
                    double xi = xData[k];
                    double yi = yData[k];

                    xxg[k] = g(xi) * yi;
                    yyg[k] = xi * g(yi);
                    xxh[k] = h(xi) * yi;
                    yyh[k] = xi * h(yi);
                }

                double[] Dh = new double[xxg.length];

                for (int k = 0; k < xxg.length; k++) {
                    Dh[k] = xxh[k] - yyh[k];
                }

                double mdh = mean(Dh);
                double sdh = sd(Dh);

                double mxxg = mean(xxg);
                double myyg = mean(yyg);
                double mxxh = mean(xxh);
                double myyh = mean(yyh);

                double tdh = (mdh - 0.0) / (sdh / sqrt(n));
                double tdxh = (mxxh - 0.0) / (sdh / sqrt(n));
                double tdyh = (myyh - 0.0) / (sdh / sqrt(n));

                if (abs(tdh) > 12 || graph0.isAdjacentTo(variables.get(i), variables.get(j))) {
                    Node x = variables.get(i);
                    Node y = variables.get(j);
                    graph.removeEdge(x, y);

                    if (signum(tdxh) == -signum(tdyh) || abs(tdxh) < T || abs(tdyh) < T || abs(tdh) < T) {
                        graph.addDirectedEdge(x, y);
                        graph.addDirectedEdge(y, x);
                    } else if (isNonGaussian(i) && isNonGaussian(j) && mxxg > myyg) {
                        graph.addDirectedEdge(x, y);
                    } else if (isNonGaussian(i) && isNonGaussian(j) && myyg > mxxg) {
                        graph.addDirectedEdge(y, x);
                    } else {
                        graph.addUndirectedEdge(x, y);
                    }
                }
            }
        }

        long stop = System.currentTimeMillis();
        this.elapsed = stop - start;

        return graph;
    }

    /**
     * @return The depth of search for the Fast Adjacency Search.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth The depth of search for the Fast Adjacency Search.
     *              The default is 5. Making this too high may results in statistical errors.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * @return The elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return elapsed;
    }

    /**
     * @return The alpha value used for T tests of equality and non-Gaussianity tests.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * @param alpha The alpha value used for T tests of equality and non-Gaussianity tests.
     *              The default is 0.05.
     */
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     * @return Returns the penalty discount used for the adjacency search.
     */
    public double getPenaltyDiscount() {
        return penaltyDiscount;
    }

    /**
     * @param penaltyDiscount Sets the penalty discount used for the adjacency search.
     *                        The default is 6, deliberately high to remove weak edges from
     *                        the graph.
     */
    public void setPenaltyDiscount(double penaltyDiscount) {
        this.penaltyDiscount = penaltyDiscount;
    }

    //======================================== PRIVATE METHODS ====================================//

    private boolean isNonGaussian(int i) {
        return nonGaussian[i];
    }

    private double g(double x) {
        return log(Math.cosh(Math.max(x, 0)));
    }

    private double h(double x) {
        return x < 0 ? 0 : x;
    }
}






