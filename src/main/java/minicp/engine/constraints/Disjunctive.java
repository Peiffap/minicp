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

import minicp.cp.Factory;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * Disjunctive Scheduling Constraint:
 * Any two pairs of activities cannot overlap in time.
 */
public class Disjunctive extends AbstractConstraint {

    private final IntVar[] start;
    private final int[] duration;
    private final IntVar[] end;
    private final boolean postMirror;
    private final int n;

    private final ThetaTree tree;
    private final int[] earliestStartingTime;
    private final int[] latestStartingTime;
    private final int[] earliestCompletionTime;
    private final int[] latestCompletionTime;
    private final int[] earliestStartingTimePlusDuration;
    private final int[] latestCompletionTimeMinusDuration;

    private final int[] indexOrder;

    private final int[] orderedByLCT;
    private final int[] orderedByEST;
    private final int[] orderedByLCTMinusDuration;
    private final int[] orderedByESTPlusDuration;

    private final int[] tmp;

    private boolean failure = false;
    

    /**
     * Creates a disjunctive constraint that enforces
     * that for any two pair i,j of activities we have
     * {@code start[i]+duration[i] <= start[j] or start[j]+duration[j] <= start[i]}.
     *
     * @param start the start times of the activities
     * @param duration the durations of the activities
     */
    public Disjunctive(IntVar[] start, int[] duration) {
        this(start, duration, true);
    }


    private Disjunctive(IntVar[] start, int[] duration, boolean postMirror) {
        super(start[0].getSolver());
        this.start = start;
        this.duration = duration;
        this.end = Factory.makeIntVarArray(start.length, i -> plus(start[i], duration[i]));
        this.postMirror = postMirror;
        this.n = start.length;

        this.earliestStartingTime = new int[n];
        this.latestStartingTime = new int[n];
        this.earliestCompletionTime = new int[n];
        this.latestCompletionTime = new int[n];
        this.earliestStartingTimePlusDuration = new int[n];
        this.latestCompletionTimeMinusDuration = new int[n];

        this.tree = new ThetaTree(n);

        this.indexOrder = new int[n];

        this.orderedByLCT = new int[n];
        this.orderedByEST = new int[n];

        this.orderedByLCTMinusDuration = new int[n];
        this.orderedByESTPlusDuration = new int[n];

        this.tmp = new int[n];
    }


    @Override
    public void post() {
        // 1: replace by  posting  binary decomposition using IsLessOrEqualVar
        Solver cp = getSolver();

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                BoolVar bij = makeBoolVar(cp);
                BoolVar bji = makeBoolVar(cp);
                cp.post(new IsLessOrEqualVar(bij, end[i], start[j]));
                cp.post(new IsLessOrEqualVar(bji, end[j], start[i]));
                cp.post(new NotEqual(bij, bji), false);
            }
        }

        // 2: add the mirror filtering as done in the Cumulative Constraint
        if (postMirror) {
            IntVar[] startMirror = Factory.makeIntVarArray(n, i -> minus(end[i]));
            getSolver().post(new Disjunctive(startMirror, duration, false), false);
        }

        for (int i = 0; i < n; i++) {
            start[i].propagateOnBoundChange(this);
        }

        propagate();
    }

    @Override
    public void propagate() {
        // TODO 6 (optional, for a bonus): implement the Lambda-Theta tree and implement the Edge-Finding        overLoadChecker();

        boolean changed = true;
        failure = false;

        setup();

        while (!failure && changed) {
            changed = overLoadChecker() || detectablePrecedence() || notLast();
            if (changed) {
                setup();
            }
        }

        if (failure) {
            throw INCONSISTENCY;
        }

    }

    private void setup() {
        for (int i = 0; i < n; i++) {
            earliestStartingTime[i] = start[i].min();
            latestStartingTime[i] = start[i].max();
            earliestCompletionTime[i] = end[i].min();
            latestCompletionTime[i] = end[i].max();
            earliestStartingTimePlusDuration[i] = earliestStartingTime[i] + duration[i];
            latestCompletionTimeMinusDuration[i] = latestCompletionTime[i] - duration[i];
        }
    }

    private int[] orderedIndices(int[] a) {
        return IntStream.range(0, a.length)
                .boxed().sorted(Comparator.comparingInt(i -> a[i]))
                .mapToInt(ele -> ele).toArray();
    }

    private void getIndexOrder(int[] indices, int[] order) {
        for (int i = 0; i < indices.length; i++) {
            order[indices[i]] = i;
        }
    }

    private boolean overLoadChecker() {
        // 3: add the OverLoadCheck algorithms.

        tree.reset(); // Start with empty tree.

        // Get ordered indices for EST and LCT.
        System.arraycopy(orderedIndices(earliestStartingTime), 0, orderedByEST, 0, n);
        System.arraycopy(orderedIndices(latestCompletionTime), 0, orderedByLCT, 0, n);

        // Get order of indices.
        getIndexOrder(orderedByEST, indexOrder);

        // Loop over activities.
        for (int i = 0; i < n; i++) {
            int index = orderedByLCT[i]; // Increasing order of LCT.

            // Insert node in tree.
            tree.insert(indexOrder[index], earliestCompletionTime[index], duration[index]);

            // If the ECT of the tree exceeds the LCT of the activity, throw inconsistency.
            if (tree.getECT() > latestCompletionTime[index]) {
                failure = true;
                return true;
            }
        }

        return false;
    }

    /**
     * @return true if one domain was changed by the detectable precedence algo
     */
    private boolean detectablePrecedence() {
        // 4: add the Detectable Precedences algorithm.
        tree.reset();

        System.arraycopy(orderedIndices(earliestStartingTime), 0, orderedByEST, 0, n);
        getIndexOrder(orderedByEST, indexOrder);

        System.arraycopy(orderedIndices(latestCompletionTimeMinusDuration), 0, orderedByLCTMinusDuration, 0, n);
        System.arraycopy(orderedIndices(earliestStartingTimePlusDuration), 0, orderedByESTPlusDuration, 0, n);

        int j = 0;
        for (int i = 0; i < n; i++) {
            int iIndex = orderedByESTPlusDuration[i];
            int jIndex = orderedByLCTMinusDuration[j];

            while (earliestStartingTimePlusDuration[iIndex] > latestCompletionTimeMinusDuration[jIndex]) {
                tree.insert(indexOrder[jIndex], earliestCompletionTime[jIndex], duration[jIndex]);
                if (j < n - 1) {
                    jIndex = orderedByLCTMinusDuration[++j];
                } else {
                    break;
                }
            }

            if (tree.isPresent(indexOrder[iIndex])) {
                tree.remove(indexOrder[iIndex]);
                tmp[iIndex] = Math.max(earliestStartingTime[iIndex], tree.getECT());
                tree.insert(indexOrder[iIndex], earliestCompletionTime[iIndex], duration[iIndex]);
            } else {
                tmp[iIndex] = Math.max(earliestStartingTime[iIndex], tree.getECT());
            }
        }

        boolean changed = false;
        for (int i = 0; i < n; i++) {
            if (tmp[i] != earliestStartingTime[i]) {
                start[i].removeBelow(tmp[i]);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * @return true if one domain was changed by the not-last algo
     */
    private boolean notLast() {
        // 5: add the Not-Last algorithm.
        tree.reset();

        System.arraycopy(orderedIndices(earliestStartingTime), 0, orderedByEST, 0, n);
        getIndexOrder(orderedByEST, indexOrder);

        System.arraycopy(orderedIndices(latestCompletionTimeMinusDuration), 0, orderedByLCTMinusDuration, 0, n);
        System.arraycopy(orderedIndices(latestCompletionTime), 0, orderedByLCT, 0, n);

        System.arraycopy(latestCompletionTime, 0, tmp, 0, n);

        int j = 0;
        for (int i = 0; i < n; i++) {
            int iIndex = orderedByLCT[i];
            int jIndex = orderedByLCTMinusDuration[j];

            while (latestCompletionTime[iIndex] > latestCompletionTimeMinusDuration[jIndex]) {
                tree.insert(indexOrder[jIndex], earliestCompletionTime[jIndex], duration[jIndex]);
                if (j < n - 1) {
                    jIndex = orderedByLCTMinusDuration[++j];
                } else {
                    break;
                }
            }

            int ect = tree.getECT();
            if (tree.isPresent(indexOrder[iIndex])) {
                tree.remove(indexOrder[iIndex]);
                ect = tree.getECT();
                tree.insert(indexOrder[iIndex], earliestCompletionTime[iIndex], duration[iIndex]);
            }

            if (ect > latestCompletionTimeMinusDuration[iIndex]) {
                tmp[iIndex] = Math.min(latestCompletionTimeMinusDuration[jIndex], latestCompletionTime[iIndex]);
            }
        }

        boolean changed = false;
        for (int i = 0; i < n; i++) {
            changed = changed || tmp[i] != latestCompletionTime[i];

            end[i].removeAbove(tmp[i]);
        }

        return changed;
    }


}
