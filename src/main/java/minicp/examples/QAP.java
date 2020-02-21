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

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;

class Pair {
    int i;
    int j;
    Pair(int i, int j) {
        this.i = i;
        this.j = j;
    }
    boolean sameij() {
        return i == j;
    }
}

/**
 * The Quadratic Assignment problem.
 * There are a set of n facilities and a set of n locations.
 * For each pair of locations, a distance is specified and for
 * each pair of facilities a weight or flow is specified
 * (e.g., the amount of supplies transported between the two facilities).
 * The problem is to assign all facilities to different locations
 * with the goal of minimizing the sum of the distances multiplied
 * by the corresponding flows.
 * <a href="https://en.wikipedia.org/wiki/Quadratic_assignment_problem">Wikipedia</a>.
 */
public class QAP {

    public static void main(String[] args) {

        // ---- read the instance -----

        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int[][] w = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
            }
        }

        // ----- build the model ---
        solve(n, w, d, true, stats -> false);
    }

    /**
     * @param n       size of the problem
     * @param w       weights
     * @param d       distances
     * @param verbose indicates if the solver should indicates on stdout its progression
     * @param limit   allow to interrupt the solver faster if needed. See dfs.solve().
     * @return list of solutions encountered
     */
    public static List<Integer> solve(int n, int[][] w, int[][] d, boolean verbose, Predicate<SearchStatistics> limit) {
        Solver cp = makeSolver();
        IntVar[] x = makeIntVarArray(cp, n, n);

        cp.post(allDifferent(x));


        // build the objective function
        IntVar[] weightedDist = new IntVar[n * n];
        for (int k = 0, i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                weightedDist[k] = mul(element(d, x[i], x[j]), w[i][j]);
                k++;
            }
        }
        IntVar totCost = sum(weightedDist);
        Objective obj = cp.minimize(totCost);

        Pair[] p = new Pair[n * n];
        int index = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                p[index++] = new Pair(i, j); // add all possible pairs of facilities
            }
        }
        DFSearch dfs = makeDfs(cp, () -> {
            Pair sel = selectMin(p,
                    pair -> !x[pair.i].isBound(), // filter (only take if the facility's location is not bound yet)
                    pair -> -w[pair.i][pair.j] // selector (minus sign because we want the maximum of the weight)
            );
            if (sel == null)
                return EMPTY;

            // get values of the chosen pair
            int[] xivalues = new int[x[sel.i].size()];
            x[sel.i].fillArray(xivalues);
            int[] xjvalues = new int[x[sel.j].size()];
            x[sel.j].fillArray(xjvalues);

            Pair[] newP = new Pair[xivalues.length * xjvalues.length]; // all possible pairs
            int ind = 0;
            for (int i = 0; i < xivalues.length; i++) {
                for (int j = 0; j < xjvalues.length; j++) {
                    newP[ind++] = new Pair(xivalues[i], xjvalues[j]); // add all possible pairs of locations
                }
            }
            Pair val = selectMin(newP,
                    pair -> !pair.sameij(), // filter (location must be unique)
                    pair -> d[pair.i][pair.j] // selector (minimize distance between locations)
            );
            return branch(
                    () -> equal(x[sel.i], val.i), // left branch: equals
                    () -> notEqual(x[sel.i], val.i) // right branch: not equals
            );
        });

        ArrayList<Integer> solutions = new ArrayList<>();
        dfs.onSolution(() -> {
            solutions.add(totCost.min());

            if (verbose)
                System.out.println("objective:" + totCost.min());
        });

        SearchStatistics stats = dfs.optimize(obj, limit);
        if (verbose)
            System.out.println(stats);

        return solutions;
    }
}
