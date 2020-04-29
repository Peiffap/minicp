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
import minicp.engine.constraints.Cumulative;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import java.util.concurrent.atomic.AtomicReference;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;
import static minicp.examples.TSPBoundImpact.boundImpactValueSelector;


/**
 * Resource Constrained Project Scheduling Problem.
 * <a href="http://www.om-db.wi.tum.de/psplib/library.html">PSPLIB</a>.
 */
public class RCPSP {

    public static void main(String[] args) {

        // Reading the data

        InputReader reader = new InputReader("data/rcpsp/j30_1_1.rcp");

        int nActivities = reader.getInt();
        int nResources = reader.getInt();

        int[] capa = new int[nResources];
        for (int i = 0; i < nResources; i++) {
            capa[i] = reader.getInt();
        }

        int[] duration = new int[nActivities];
        int[][] consumption = new int[nResources][nActivities];
        int[][] successors = new int[nActivities][];


        int horizon = 0;
        for (int i = 0; i < nActivities; i++) {
            // durations, demand for each resource, successors
            duration[i] = reader.getInt();
            horizon += duration[i];
            for (int r = 0; r < nResources; r++) {
                consumption[r][i] = reader.getInt();
            }
            successors[i] = new int[reader.getInt()];
            for (int k = 0; k < successors[i].length; k++) {
                successors[i][k] = reader.getInt() - 1;
            }
        }


        // -------------------------------------------

        // The Model

        Solver cp = makeSolver();

        IntVar[] start = makeIntVarArray(cp, nActivities, horizon);
        IntVar[] end = new IntVar[nActivities];


        for (int i = 0; i < nActivities; i++) {
            end[i] = plus(start[i], duration[i]);
        }

        // 1: add the cumulative constraint to model the resource
        // capa[r] is the capacity of resource r
        // consumption[r] is the consumption for each activity on the resource [r]
        // duration is the duration of each activity
        for (int r = 0; r < nResources; r++) {
            cp.post(new Cumulative(start, duration, consumption[r], capa[r]));
        }

        // 2: add the precedence constraints
        // successors[i] is the successors of activity i

        // Each successor of i should start after i has ended.
        for (int i = 0; i < nActivities; i++) {
            for (int j = 0; j < successors[i].length; j++) {
                int succ = successors[i][j];
                cp.post(lessOrEqual(end[i], start[succ]));
            }
        }

        // 3: minimize the makespan

        IntVar makespan = maximum(end); // Makespan is last end time.

        Objective obj = cp.minimize(makespan); // We want to minimize the makespan.

        // 4: implement the search

        // We use conflict ordering search because we're fancy.
        DFSearch dfs = makeDfs(cp, BranchingScheme.conflictOrderingSearch(
                () -> { // Select first unbound variable in start as fallback.
                    for (IntVar z: start)
                        if (!z.isBound())
                            return z;
                    return null;
                },
                xs -> boundImpactValueSelector(xs, makespan) // Use BIVS value selector.
        ));

        dfs.onSolution(() -> System.out.println(makespan)); // Print found solutions.

        SearchStatistics stats = dfs.optimize(obj);

        System.out.println(stats);
    }
}
