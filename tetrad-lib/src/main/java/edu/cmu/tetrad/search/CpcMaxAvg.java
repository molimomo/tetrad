///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014 by Peter Spirtes, Richard Scheines, Joseph   //
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

import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.ChoiceGenerator;
import edu.cmu.tetrad.util.DepthChoiceGenerator;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;

import java.io.PrintStream;
import java.util.*;

/**
 * Implements the CPC-MaxAvg search.
 *
 * @author Joseph Ramsey.
 */
public class CpcMaxAvg implements IFas {

    /**
     * The search graph. It is assumed going in that all of the true adjacencies of x are in this graph for every node
     * x. It is hoped (i.e. true in the large sample limit) that true adjacencies are never removed.
     */
    private Graph graph;

    /**
     * The search nodes.
     */
    private List<Node> nodes;

    /**
     * The independence test. This should be appropriate to the types
     */
    private IndependenceTest test;

    /**
     * Specification of which edges are forbidden or required.
     */
    private IKnowledge knowledge = new Knowledge2();

    /**
     * The maximum number of variables conditioned on in any conditional independence test. If the depth is -1, it will
     * be taken to be the maximum value, which is 1000. Otherwise, it should be set to a non-negative integer.
     */
    private int depth = 1000;

    /**
     * The logger, by default the empty logger.
     */
    private TetradLogger logger = TetradLogger.getInstance();

    /**
     * The sepsets found during the search.
     */
    private SepsetMap sepset = new SepsetMap();

    /**
     * True iff verbose output should be printed.
     */
    private boolean verbose = false;

    private int useMaxTopN = 1;

    //==========================CONSTRUCTORS=============================//

    public CpcMaxAvg(IndependenceTest test) {
        this.test = test;
        this.nodes = test.getVariables();
    }

    //==========================PUBLIC METHODS===========================//

    /**
     * Discovers all adjacencies in data.  The procedure is to remove edges in the graph which connect pairs of
     * variables which are independent conditional on some other set of variables in the graph (the "sepset"). These are
     * removed in tiers.  First, edges which are independent conditional on zero other variables are removed, then edges
     * which are independent conditional on one other variable are removed, then two, then three, and so on, until no
     * more edges can be removed from the graph.  The edges which remain in the graph after this procedure are the
     * adjacencies in the data.
     *
     * @return a SepSet, which indicates which variables are independent conditional on which other variables
     */
    public Graph search() {
        this.logger.log("info", "Starting Fast Adjacency Search.");

        sepset = new SepsetMap();

        graph = new EdgeListGraph(nodes);
        graph = GraphUtils.completeGraph(graph);

        Map<NodePair, List<PValue>> pValueMap = new HashMap<>();

        findAdjacencies(pValueMap);

        orientTriples(pValueMap, depth, graph);

        MeekRules meekRules = new MeekRules();
        meekRules.setKnowledge(knowledge);
        meekRules.orientImplied(graph);

        this.logger.log("info", "Finishing Fast Adjacency Search.");

        return graph;
    }

    private void findAdjacencies(Map<NodePair, List<PValue>> pValueMap) {
        FasStable fas = new FasStable(test);
        this.graph = fas.search();


//        boolean more = false;
//
//        int d = 0;
//
//        do {
//            more = adjust(pValueMap, graph, d++, test);
//            if (d > depth) break;
//        } while (more);
//
////        for (int f = 0; f < 10; f++) {
////            adjust(pValueMap, graph, d, test);
////        }
    }

    public void setDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException(
                    "Depth must be -1 (unlimited) or >= 0.");
        }

        if (depth == -1) depth = 1000;
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public IKnowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(IKnowledge knowledge) {
        if (knowledge == null) {
            throw new NullPointerException("Cannot set knowledge to null");
        }
        this.knowledge = knowledge;
    }


    public int getNumIndependenceTests() {
        return 0;
    }

    public void setTrueGraph(Graph trueGraph) {
//        this.trueGraph = trueGraph;
    }

    public int getNumFalseDependenceJudgments() {
        return 0;
    }

    public int getNumDependenceJudgments() {
        return 0;
    }

    public SepsetMap getSepsets() {
        return sepset;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isAggressivelyPreventCycles() {
        return false;
    }

    @Override
    public void setAggressivelyPreventCycles(boolean aggressivelyPreventCycles) {

    }

    @Override
    public IndependenceTest getIndependenceTest() {
        return null;
    }

    @Override
    public Graph search(List<Node> nodes) {
        return null;
    }

    @Override
    public long getElapsedTime() {
        return 0;
    }

    @Override
    public List<Node> getNodes() {
        return test.getVariables();
    }

    @Override
    public List<Triple> getAmbiguousTriples(Node node) {
        return null;
    }

    @Override
    public void setOut(PrintStream out) {

    }

    /**
     * The maximum number of top p-values to average.
     */
    private int getUseMaxTopN() {
        return useMaxTopN;
    }

    public void setUseMaxTopN(int useMaxTopN) {
        this.useMaxTopN = useMaxTopN;
    }

    public static enum TripleType {COLLIDER, NONCOLLIDER, AMBIGUOUS, NEITHER}

    //==============================PRIVATE METHODS======================/

    private boolean adjust(Map<NodePair, List<PValue>> pValueMap,
                           Graph graph, int depth, IndependenceTest test) {
        Graph complete = GraphUtils.completeGraph(graph);

        List<Edge> toRemove = new ArrayList<>();

        for (Edge edge : graph.getEdges()) {
            Node a = edge.getNode1();
            Node c = edge.getNode2();

            boolean existsSepset = existsSepset(pValueMap, a, c, depth, graph, test);

            if (existsSepset && graph.isAdjacentTo(a, c)) {
                toRemove.add(edge);
            }
//
//            if (existsSepset && graph.isAdjacentTo(a, c)) {
//                graph.removeEdge(a, c);
//            } else if (!existsSepset && !graph.isAdjacentTo(a, c)) {
//                graph.addUndirectedEdge(a, c);
//            }
        }

        for (Edge edge : toRemove) {
            graph.removeEdge(edge);
        }

        return freeDegree(nodes, graph) > depth;
    }

    private void orientTriples(Map<NodePair, List<PValue>> pValueMap, int depth, Graph graph) {
        List<Node> nodes1 = graph.getNodes();

        for (Node b : nodes1) {
            List<Node> adjacentNodes = graph.getAdjacentNodes(b);

            if (adjacentNodes.size() < 2) {
                continue;
            }

            ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                Node a = adjacentNodes.get(combination[0]);
                Node c = adjacentNodes.get(combination[1]);

                // Skip triples that are shielded.
                if (graph.isAdjacentTo(a, c)) {
                    continue;
                }

                TripleType type = getTripleType(pValueMap, a, b, c, depth, graph, test);

                if (type == TripleType.COLLIDER && !graph.isUnderlineTriple(a, b, c)
                        && !graph.isAmbiguousTriple(a, c, b)) {
                    orientCollider(a, b, c, graph);
                    graph.removeAmbiguousTriple(a, b, c);
                    graph.removeUnderlineTriple(a, b, c);
                } else if (type == TripleType.NONCOLLIDER) {
                    graph.addUnderlineTriple(a, b, c);
                    graph.removeAmbiguousTriple(a, b, c);
                } else if (type == TripleType.NEITHER) {
//                    throw new IllegalArgumentException("Neither collider nor uncollider; this shouldn't happen: "
//                            + a + "---" + b + "---" + c);
                    graph.addUnderlineTriple(a, b, c);
                } else if (type == TripleType.AMBIGUOUS) {
                    System.out.println("Both");
//                    graph.addUndirectedEdge(a, c);
                    graph.addAmbiguousTriple(a, b, c);
                    graph.removeUnderlineTriple(a, b, c);
                }
            }
        }
    }

    private void orientCollider(Node x, Node y, Node z, Graph graph) {
        if (!(graph.getEndpoint(y, x) == Endpoint.ARROW || graph.getEndpoint(y, z) == Endpoint.ARROW)) {
            graph.removeEdge(x, y);
            graph.removeEdge(z, y);
            graph.addDirectedEdge(x, y);
            graph.addDirectedEdge(z, y);
        }

        TetradLogger.getInstance().log("colliderOrientations", SearchLogUtils.colliderOrientedMsg(x, y, z));
    }

    private TripleType getTripleType(Map<NodePair, List<PValue>> pValueMap, Node a, Node b, Node c, int depth,
                                     Graph graph, IndependenceTest test) {
        System.out.println("Calculating max avg for " + a + " --- " + b + " --- " + c + " depth = " + depth);


//        List<PValue> pValues = pValueMap.get(new NodePair(a, c));
        List<PValue> pValues = getAllPValues(a, c, depth, graph, test);

//        if (false) return maxPSepetContainsB(pValues, b) ? TripleType.NONCOLLIDER : TripleType.COLLIDER;


//        List<PValue> pValues = getAllPValues(a, c, depth, graph, test);

        List<PValue> bPvals = new ArrayList<>();
        List<PValue> notbPvals = new ArrayList<>();

        for (PValue p : pValues) {
            if (p.getSepset().contains(b)) {
                bPvals.add(p);
            } else {
                notbPvals.add(p);
            }
        }

        bPvals.sort(Comparator.comparingDouble(PValue::getP));
        notbPvals.sort(Comparator.comparingDouble(PValue::getP));

        double q = test.getAlpha();

        boolean existsb = existsSepsetFromList(bPvals, q, 1);
        boolean existsnotb = existsSepsetFromList(notbPvals, q, .4);

        if (existsb && !existsnotb) {
            return TripleType.NONCOLLIDER;
        } else if (!existsb && existsnotb) {
            return TripleType.COLLIDER;
        } else if (existsb) {
            return TripleType.AMBIGUOUS;
        } else {
            return TripleType.NEITHER;
        }
    }

    private boolean maxPSepetContainsB(List<PValue> pValues, Node b) {
        pValues.sort(Comparator.comparingDouble(PValue::getP));
        PValue max = pValues.get(pValues.size() - 1);
        return max.getSepset().contains(b);
    }

    private int freeDegree(List<Node> nodes, Graph graph) {
        int max = 0;

        for (Node x : nodes) {
            List<Node> opposites = graph.getAdjacentNodes(x);

            for (Node y : opposites) {
                Set<Node> adjx = new HashSet<>(opposites);
                adjx.remove(y);

                if (adjx.size() > max) {
                    max = adjx.size();
                }
            }
        }

        return max;
    }

    private synchronized boolean existsSepset(Map<NodePair, List<PValue>> pValueList,
                                              Node a, Node c, int depth, Graph graph, IndependenceTest test) {
        System.out.println("Calculating max avg for " + a + " --- " + c + " depth = " + depth);
        List<PValue> pValues = getAllPValues(a, c, depth, graph, test);
        pValueList.remove(new NodePair(a, c));
        pValueList.put(new NodePair(a, c), pValues);
        return existsSepsetFromList(pValues, test.getAlpha(), 1);
    }

    private static class PValue {
        private double p;
        private Set<Node> sepset;

        PValue(double p, List<Node> sepset) {
            this.p = p;
            this.sepset = new HashSet<>(sepset);
        }

        public Set<Node> getSepset() {
            return sepset;
        }

        public double getP() {
            return p;
        }

        public boolean equals(Object o) {
            if (!(o instanceof PValue)) return false;
            PValue _o = (PValue) o;
            return _o.getP() == getP() && _o.getSepset().equals(getSepset());
        }
    }

    private List<PValue> getAllPValues(Node a, Node c, int depth, Graph graph, IndependenceTest test) {
        List<Node> adja = graph.getAdjacentNodes(a);
        List<Node> adjc = graph.getAdjacentNodes(c);

        adja.remove(c);
        adjc.remove(a);

        List<List<Node>> adj = new ArrayList<>();
        adj.add(adja);
        adj.add(adjc);

        List<PValue> pValues = new ArrayList<>();

        for (List<Node> _adj : adj) {
            DepthChoiceGenerator cg1 = new DepthChoiceGenerator(_adj.size(), depth);
            int[] comb2;

            while ((comb2 = cg1.next()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                List<Node> s = GraphUtils.asList(comb2, _adj);

                test.isIndependent(a, c, s);
                PValue e = new PValue(test.getPValue(), s);
                if (pValues.contains(e)) continue;
                pValues.add(e);
            }
        }

        return pValues;
    }

    private boolean existsSepsetFromList(List<PValue> pValues, double alpha, double factor) {
        if (pValues.isEmpty()) return false;

        pValues.sort(Comparator.comparingDouble(PValue::getP));

        List<Double> _pValues = new ArrayList<>();

        for (PValue p : pValues) {
            _pValues.add(p.getP());
        }

        double cutoff = StatUtils.fdrCutoff(alpha, _pValues, false, false);

        System.out.println("cutoff = " + cutoff);

        for (double p : _pValues) {
            if (p * factor > cutoff) return true;
        }

        return false;

//        double sum = 0;
//        int count = 0;
//
//        for (int i = pValues.size() - 1; i >= 0; i--) {
//            if (i >= 0) {
//                double p = pValues.get(i).getP();
//
//                sum += p * p;
//                count++;
//            }
//        }
//
//        double avg = sum / count;
//        return avg > test.getAlpha();
    }
}
