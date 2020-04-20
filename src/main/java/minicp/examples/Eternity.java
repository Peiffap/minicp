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

import minicp.cp.Factory;
import minicp.engine.constraints.*;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import java.util.Arrays;

import static minicp.cp.BranchingScheme.and;
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;

/**
 *
 *  The Eternity II puzzle is an edge-matching puzzle which
 *  involves placing 256 square puzzle pieces into a 16 by 16 grid,
 *  constrained by the requirement to match adjacent edges.
 *  <a href="https://en.wikipedia.org/wiki/Eternity_II_puzzle">Wikipedia.</a>
 */
public class Eternity {

    public static IntVar[] flatten(IntVar[][] x) {
        return Arrays.stream(x).flatMap(Arrays::stream).toArray(IntVar[]::new);
    }

    public static int[][] generatePermutations(int[][] pieces, int i) {
        int[][] possibilities = new int[4][4];

        for (int j = 0; j < 4; ++j) {
            possibilities[j] = new int[4];
            for (int k = 0; k < pieces[k].length; ++k) {
                possibilities[j][k] = pieces[i][(j + k) % 4];
            }
        }

        return possibilities;
    }

    public static void main(String[] args) {

        // Reading the data

        InputReader reader = new InputReader("data/eternity/eternity7x7.txt");

        int n = reader.getInt();
        int m = reader.getInt();

        int[][] pieces = new int[n * m][4];
        int maxTmp = 0;

        for (int i = 0; i < n * m; i++) {
            for (int j = 0; j < 4; j++) {
                pieces[i][j] = reader.getInt();
                if (pieces[i][j] > maxTmp)
                    maxTmp = pieces[i][j];
            }
            System.out.println(Arrays.toString(pieces[i]));
        }
        final int max = maxTmp;

        // ------------------------

        // create the table where each line correspond to one possible rotation of a piece
        // For instance if the line piece[6] = [2,3,5,1]
        // the four lines created in the table are
        // [6,2,3,5,1] // rotation of 0°
        // [6,3,5,1,2] // rotation of 90°
        // [6,5,1,2,3] // rotation of 180°
        // [6,1,2,3,5] // rotation of 270°

        // Table with makeIntVarArray pieces and for each their 4 possible rotations

        int [][] table = new int[4 * n * m][5];

        for (int j = 0; j < n * m; ++j) {
            int[][] permutations = generatePermutations(pieces, j);
            int start = j * 4;
            int end = ((j + 1) * 4) - 1;
            for (int i = start; i <= end; ++i) {
                table[i][0] = j;
                for (int k = 0; k < 4; ++k) {
                    table[i][k + 1] = permutations[i % 4][k];
                }
            }
        }

        Solver cp = makeSolver();

        //   |         |
        // - +---------+- -
        //   |    u    |
        //   | l  i  r |
        //   |    d    |
        // - +---------+- -
        //   |         |


        IntVar[][] id = new IntVar[n][m]; // id
        IntVar[][] u = new IntVar[n][m];  // up
        IntVar[][] r = new IntVar[n][m];  // right
        IntVar[][] d = new IntVar[n][m];  // down
        IntVar[][] l = new IntVar[n][m];  // left

        for (int i = 0; i < n; i++) {
            u[i] = Factory.makeIntVarArray(m, j -> makeIntVar(cp, 0, max));
            id[i] = makeIntVarArray(cp, m, n * m);
        }
        for (int k = 0; k < n; k++) {
            final int i = k;
            if (i < n - 1) d[i] = u[i + 1];
            else d[i] = Factory.makeIntVarArray(m, j -> makeIntVar(cp, 0, max));
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                l[i][j] = makeIntVar(cp, 0, max);
            }
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                if (j < m - 1) r[i][j] = l[i][j + 1];
                else r[i][j] = makeIntVar(cp, 0, max);
            }
        }

        // The constraints of the problem

        // Constraint1: all the pieces placed are different

        cp.post(new AllDifferentFW(flatten(id)));
        // For some reason, AllDifferentFW >> AllDifferentDC

        // Constraint2: all the pieces placed are valid ones i.e. one of the given mxn pieces possibly rotated

        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                IntVar[] tuple = {id[i][j], u[i][j], r[i][j], d[i][j], l[i][j]};
                cp.post(new TableCT(tuple, table));
            }
        }

        // Constraint3: place "0" one all external side of the border (gray color)

        for (int i = 0; i < m; ++i) {
            u[0][i].assign(0);
            d[n - 1][i].assign(0);
        }

        for (int i = 0; i < n; ++i) {
            l[i][0].assign(0);
            r[i][m - 1].assign(0);
        }

        // The search using the and combinator

        DFSearch dfs = makeDfs(cp,
                 and(
                         firstFail(flatten(id)),
                         firstFail(flatten(u)),
                         firstFail(flatten(l)),
                         firstFail(flatten(r)),
                         firstFail(flatten(d)))
        );


        dfs.onSolution(() -> {
            System.out.println("----------------");
            // Pretty Print
            for (int i = 0; i < n; i++) {
                String line = "   ";
                for (int j = 0; j < m; j++) {
                    line += u[i][j].min() + "   ";
                }
                System.out.println(line);
                line = " ";
                for (int j = 0; j < m; j++) {
                    line += l[i][j].min() + "   ";
                }
                line += r[i][m - 1].min();
                System.out.println(line);
            }
            String line = "   ";
            for (int j = 0; j < m; j++) {
                line += d[n - 1][j].min() + "   ";
            }
            System.out.println(line);

        });

        long t0 = System.currentTimeMillis();
        SearchStatistics stats = dfs.solve(statistics -> statistics.numberOfSolutions() == 5);

        System.out.format("#Solutions: %s\n", stats.numberOfSolutions());
        System.out.format("Statistics: %s\n", stats);
        System.out.format("time: %s\n", System.currentTimeMillis()-t0);
    }
}
