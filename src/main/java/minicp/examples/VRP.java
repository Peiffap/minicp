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

import minicp.cp.BranchingScheme;
import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1D;
import minicp.engine.constraints.Element1DDomainConsistent;
import minicp.engine.constraints.Element1DVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.LimitedDiscrepancyBranching;
import minicp.search.Objective;
import minicp.util.exception.InconsistencyException;
import minicp.util.io.InputReader;
import minicp.search.SearchStatistics;
import sun.plugin.dom.core.Element;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;

/**
 * Vehicle routing problem.
 * <a href="https://en.wikipedia.org/wiki/Vehicle_routing_problem">Wikipedia</a>.
 */
public class VRP {
    public static void main(String[] args) {

        // Variant 1: the fastest solution is to run the basic TSP on the first vehicle,
        // and leave the others at the depot.

        // Variant 2: duplicate the depots once for each vehicle (copy column 1 / row 1 (k-1) times),
        // and add the constraint that one cannot go from one depot to another.
        // To do this, minimize over maximum of distances from depot i to depot i+1.
        // For this, add sumToMe counter which counts distance from 0 to current node.


        // instance gr17 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/gr17_d.txt
        InputReader reader = new InputReader("data/tsp.txt");

        int n = reader.getInt();

        int[][] dMatrix = reader.getMatrix(n, n);

        int k = 3;
        int[][] distanceMatrix = new int[n+k][n+k];

        for (int i = 0; i < n+k; i++) {
            for (int j = 0; j < n+k; j++) {
                if (i >= n && j >= n) {
                    distanceMatrix[i][j] = 0;
                } else if (i >= n) {
                    distanceMatrix[i][j] = dMatrix[0][j];
                } else if (j >= n) {
                    distanceMatrix[i][j] = dMatrix[i][0];
                } else {
                    distanceMatrix[i][j] = dMatrix[i][j];
                }
            }
        }

        /*
        int k = 2;
        int n = 3;
        int[][] distanceMatrix = {{0, 3, 4, 0, 0},
                                  {3, 0, 5, 3, 3},
                                  {4, 5, 0, 4, 4},
                                  {0, 3, 4, 0, 0},
                                  {0, 3, 4, 0, 0}};
         */


        int[] depots = new int[k];
        depots[0] = 0;
        int next = 1;
        for (int i = n; i < n+k-1; i++) {
            depots[next++] = i;
        }

        print(distanceMatrix);

        Solver cp = makeSolver(false);
        IntVar[] succ = makeIntVarArray(cp, n+k, n+k);
        IntVar[] distSucc = makeIntVarArray(cp, n+k, 1000);
        IntVar[] cumulativeDistance = makeIntVarArray(cp, n+k, 3000);
        IntVar[] distancePerTruck = makeIntVarArray(cp, k, 3000);

        for (int i: depots) {
            for (int j: depots) {
                cp.post(notEqual(succ[i], j));
            }
            cp.post(notEqual(succ[i], n+k-1));
        }

        cp.post(new Circuit(succ));

        cp.post(equal(sum(succ), (n+k-1)*(n+k)/2));
        cp.post(equal(cumulativeDistance[0], 0));
        cp.post(equal(succ[n+k-1], 0));
        cp.post(equal(distSucc[n+k-1],0));
        for (int i = 0; i < n+k-1; i++) {
            cp.post(new Element1DDomainConsistent(distanceMatrix[i], succ[i], distSucc[i]));
            cp.post(new Element1DVar(cumulativeDistance, succ[i], sum(cumulativeDistance[i], distSucc[i])));
        }

        for (int i = 1; i < k; i++) {
            cp.post(equal(distancePerTruck[i], sum(cumulativeDistance[depots[i]], minus(cumulativeDistance[depots[i-1]]))));
        }
        if (k > 1) {
            cp.post(equal(distancePerTruck[0], sum(cumulativeDistance[n+k-1], minus(cumulativeDistance[n+k-2]))));
        } else {
            cp.post(equal(distancePerTruck[0], cumulativeDistance[n+k-1]));
        }

        IntVar maxDist = maximum(distancePerTruck);

        Objective obj = cp.minimize(maxDist);

        // Variable selection: min-regret.
        // Choose the variable xi which has the largest difference between the closest two successor cities:
        // s1(xi) = min(distanceMatrix[xi][.])
        // s2(xi) = min(distanceMatrix[xi][.] \ {distanceMatrix[xi][indexOf(s1)]})
        // xi : max(s2(xi) - s1(xi))

        DFSearch dfs = new DFSearch(cp.getStateManager(), BranchingScheme.conflictOrderingSearch(
            () -> {
                IntVar xs = selectMin(succ,
                    xi -> xi.size() > 1,
                    xi -> {
                        int[] xidom = new int[xi.size()];
                        xi.fillArray(xidom);
                        int i = Arrays.asList(succ).indexOf(xi);
                        int s1val = Integer.MAX_VALUE;
                        int s2val = Integer.MAX_VALUE;
                        for (int j : xidom) {
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
                return xs;
            },
            xs -> {
                // Value selection: select successor with minimal distance.
                if (xs == null) {
                    return null;
                } else {
                    int j = Arrays.asList(succ).indexOf(xs);
                    int[] xsdom = new int[xs.size()];
                    xs.fillArray(xsdom);
                    int minDist = Integer.MAX_VALUE;
                    int index = -1;
                    for (int value : xsdom) {
                        if (distanceMatrix[value][j] < minDist) {
                            minDist = distanceMatrix[value][j];
                            index = value;
                        }
                    }
                    return index;
                }
            }));

        /*
        DFSearch dfs = makeDfs(cp, limitedDiscrepancy(() -> {
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
            if (xs == null) {
                return EMPTY;
            } else {
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
        }, 10));

        dfs.onSolution(() -> {
                System.out.println(maxDist);
                System.out.println(Arrays.toString(distancePerTruck));
                System.out.println(Arrays.toString(cumulativeDistance));
                System.out.println(Arrays.toString(succ));
            }
        );

        SearchStatistics stats = dfs.optimize(obj);
        System.out.println(stats);
         */
        // Current best solution
        int[] xBest = IntStream.range(0, n).toArray();

        dfs.onSolution(() -> {
            // Update the current best solution
            for (int i = 0; i < n; i++) {
                xBest[i] = succ[i].min();
            }
            System.out.println("objective:" + maxDist.min());
        });


        int nRestarts = 1000;
        int failureLimit = 1000;
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
    }

    public static void print(int[][] arr) {
        for (int[] row: arr) {
            for (int i: row) {
                System.out.printf("%3d ", i);
            }
            System.out.println();
        }
    }
}
