/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.GraphUtil;
import minicp.util.GraphUtil.*;

import java.util.ArrayList;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * Arc Consistent AllDifferent Constraint
 *
 * Algorithm described in
 * "A filtering algorithm for constraints of difference in CSPs" J-C. Régin, AAAI-94
 */
public class AllDifferentDC extends AbstractConstraint {

    private IntVar[] x;

    private final MaximumMatching maximumMatching;

    private final int nVar;
    private int nVal;

    // residual graph
    private ArrayList<Integer>[] in;
    private ArrayList<Integer>[] out;
    private int nNodes;
    private DirectedGraph g;

    private int[] match;
    private int[] domain;

    private int minVal;
    private int maxVal;
    private int maxSize;


    public AllDifferentDC(IntVar... x) {
        super(x[0].getSolver());
        this.x = x;

        maximumMatching = new MaximumMatching(x);
        match = new int[x.length];
        this.nVar = x.length;
    }


    @Override
    public void post() {
        for (int i = 0; i < nVar; i++) {
            x[i].propagateOnDomainChange(this);
        }
        updateRange();
        domain = new int[maxSize];
        nNodes = nVar + nVal + 1;
        g = new DirectedGraph(nNodes);
        in = new ArrayList[nNodes];
        out = new ArrayList[nNodes];
        for (int i = 0; i < nNodes; i++) {
            in[i] = new ArrayList<>();
            out[i] = new ArrayList<>();
        }
        propagate();
    }


    private void updateRange() {
        minVal = Integer.MAX_VALUE;
        maxVal = Integer.MIN_VALUE;
        maxSize = Integer.MIN_VALUE;
        for (int i = 0; i < nVar; i++) {
            minVal = Math.min(minVal, x[i].min());
            maxVal = Math.max(maxVal, x[i].max());
            maxSize = Math.max(maxSize, x[i].size());
        }
        nVal = maxVal - minVal + 1;
    }


    // Node ID - N + Min Value = Value
    // Node ID = Value + N - Min Value
    private int getNodeId(int value) {
        return value + x.length - minVal;
    }


    private void updateGraph() {
        g.clear();
        nNodes = nVar + nVal + 1;
        int sink = nNodes - 1;
        for (int j = 0; j < nNodes; j++) {
            in[j].clear();
            out[j].clear();
        }

        for (int v = minVal; v <= maxVal; ++v) {
            int id = getNodeId(v);

            // Link value to dummy
            g.link(id, sink);
        }

        for (int i = 0; i < x.length; ++i) {
            int v = match[i];
            int id = getNodeId(v);
            // Link matching val to variable
            g.link(id, i);

            // unlink matched value to dummy
            g.unlink(id, sink);

            // Link dummy to value
            g.link(sink, id);

            int size = x[i].fillArray(domain);
            for (int j = 0; j < size; ++j) {
                if (domain[j] != v) {

                    // Match variable to node value
                    g.link(i, getNodeId(domain[j]));
                }
            }
        }
    }


    @Override
    public void propagate() {
        // hint: use maximumMatching.compute(match) to update the maximum matching
        //       use updateRange() to update the range of values
        //       use updateGraph() to update the residual graph
        //       use  GraphUtil.stronglyConnectedComponents to compute SCC's
        int sizeMatching = maximumMatching.compute(match);

        if (sizeMatching < x.length) {
            throw INCONSISTENCY;
        }

        updateGraph();

        int[] components = GraphUtil.stronglyConnectedComponents(g);
        for (int i = 0; i < x.length; ++i) {
            int size = x[i].fillArray(domain);
            for (int j = 0; j < size; ++j) {
                int v = domain[j];
                // If the value is not the value from the maximum matching and if the component
                // of the variable is different from the component of the value
                if (match[i] != v && components[i] != components[getNodeId(v)]) {
                    x[i].remove(v);
                }
            }
        }
    }
}
