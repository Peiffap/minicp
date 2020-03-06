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

package minicp.examples;

import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1D;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;

/**
 * Traveling salesman problem.
 * <a href="https://en.wikipedia.org/wiki/Travelling_salesman_problem">Wikipedia</a>.
 */
public class TSPLNS {

    public static void main(String[] args) {

        // ---- read the instance -----

        // instance gr17 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/gr17_d.txt
        InputReader reader = new InputReader("data/tsp.txt");

        int n = reader.getInt();

        int[][] distanceMatrix = reader.getMatrix(n, n);

        Solver cp = makeSolver(false);
        IntVar[] succ = makeIntVarArray(cp, n, n);
        IntVar[] distSucc = makeIntVarArray(cp, n, 1000);

        cp.post(new Circuit(succ));

        for (int i = 0; i < n; i++) {
            cp.post(new Element1D(distanceMatrix[i], succ[i], distSucc[i]));
        }

        IntVar totalDist = sum(distSucc);

        Objective obj = cp.minimize(totalDist);

        // Variable selection: min-regret.
        // Choose the variable xi which has the largest difference between the closest two successor cities:
        // s1(xi) = min(distanceMatrix[xi][.])
        // s2(xi) = min(distanceMatrix[xi][.] \ {distanceMatrix[xi][indexOf(s1)]})
        // xi : max(s2(xi) - s1(xi))

        DFSearch dfs = makeDfs(cp, () -> {
            IntVar xs = selectMin(succ,
                    xi -> xi.size() > 1,
                    xi -> {
                        int[] xidom = new int[xi.size()];
                        xi.fillArray(xidom);
                        int i = Arrays.asList(succ).indexOf(xi);
                        int s1val = Integer.MAX_VALUE;
                        int s2val = Integer.MAX_VALUE;
                        for (int ind = 0; ind < xidom.length; ind++) {
                            int j = xidom[ind];
                            int d = distanceMatrix[i][j];
                            if (d < s1val) {
                                s2val = s1val;
                                s1val = d;
                            } else if (d < s2val) {
                                s2val = d;
                            }
                        }
                        return s1val - s2val;
                    });

            // Value selection: select successor with minimal distance.
            if (xs == null)
                return EMPTY;
            else {
                int j = Arrays.asList(succ).indexOf(xs);
                int[] xsdom = new int[xs.size()];
                xs.fillArray(xsdom);
                int minDist = Integer.MAX_VALUE;
                int index = -1;
                for (int i = 0; i < xsdom.length; i++) {
                    if (distanceMatrix[xsdom[i]][j] < minDist) {
                        minDist = distanceMatrix[xsdom[i]][j];
                        index = xsdom[i];
                    }
                }
                int v = index;
                return branch(() -> xs.getSolver().post(equal(xs, v)),
                        () -> xs.getSolver().post(notEqual(xs, v)));
            }
        });

        // --- Large Neighborhood Search ---

        // Current best solution
        int[] xBest = IntStream.range(0, n).toArray();

        dfs.onSolution(() -> {
            // Update the current best solution
            for (int i = 0; i < n; i++) {
                xBest[i] = succ[i].min();
            }
            System.out.println("objective:" + totalDist.min());
        });


        int nRestarts = 1000;
        int failureLimit = 100;
        Random rand = new java.util.Random(0);

        for (int i = 0; i < nRestarts; i++) {
            if (i % 10 == 0)
                System.out.println("restart number #" + i);

            dfs.optimizeSubjectTo(obj, statistics -> statistics.numberOfFailures() >= failureLimit, () -> {
                        // Assign the fragment 5% of the variables randomly chosen
                        for (int j = 0; j < n; j++) {
                            if (rand.nextInt(100) < 5) {
                                // after the solveSubjectTo those constraints are removed
                                cp.post(equal(succ[j], xBest[j]));
                            }
                        }
                    }
            );
        }


        // Analysis:
        // Convergence: LNS converges faster than regular TSP, but the optimal solution is not always found.
        // Percentage: Less restarts before finding a solution the higher the percentage is.
        // Failure limit: Restarts more frequently if limit is smaller.
    }
}
