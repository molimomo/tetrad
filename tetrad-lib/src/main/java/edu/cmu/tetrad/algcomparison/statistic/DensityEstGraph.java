package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;

/**
 * The adjacency precision. The true positives are the number of adjacencies in both
 * the true and estimated graphs.
 *
 * @author jdramsey
 */
public class DensityEstGraph implements Statistic {
    static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "DnE";
    }

    @Override
    public String getDescription() {
        return "Density for Estimated Graph";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        int V = estGraph.getNumNodes();
        int E = estGraph.getNumEdges();
        return (2.0 * E) / (double) (V * (V - 1));
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}
