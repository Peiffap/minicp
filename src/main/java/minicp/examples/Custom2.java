package minicp.examples;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;

import java.util.ArrayList;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;
import static minicp.examples.DialARide.stopType;

public class Custom2 {

    public static DFSearch custom(Solver cp, IntVar[] succ, IntVar[] prec, int n, int k, int[][] distanceMatrix, int maxRouteDuration, IntVar[] distanceSinceDepot, ArrayList<DialARide.RideStop> allStops) {
        return makeDfs(cp, () -> {
            int lowestDistance = Integer.MAX_VALUE;
            int var = 0;
            for (int i = 0; i < 2*k+2*n; i++) {
                if (prec[i].isBound() && !succ[i].isBound()) {
                    if (distanceSinceDepot[i].min() < lowestDistance) {
                        lowestDistance = distanceSinceDepot[i].min();
                        var = i;
                    }
                }
            }

            int[] dom = new int[succ[var].size()];
            succ[var].fillArray(dom);

            int bestPick = -1;
            int bestDrop = -1;
            int bPickMetric = Integer.MAX_VALUE;
            int bDropMetric = Integer.MAX_VALUE;

            for (int d: dom) {
                char t = stopType(d, n, k);
                if (t == 'd') {
                    IntVar corresponding = succ[d - n];
                    if (corresponding == succ[var] || corresponding.isBound()) {
                        int metric = allStops.get(d).window_end;
                        if (metric < bDropMetric) {
                            bDropMetric = metric;
                            bestDrop = d;
                        }
                    }
                } else if (t == 'p') {
                    int metric = distanceMatrix[var][d];//Math.min(allStops.get(d).window_end - distanceMatrix[var][d], allStops.get(d + n).window_end + distanceMatrix[d][d + n] + distanceMatrix[var][d]);
                    if (metric < bPickMetric) {
                        bPickMetric = metric;
                        bestPick = d;
                    }
                }
            }

            IntVar finalVar = succ[var];
            int v = finalVar.min();
            if (bestDrop != -1) {
                v = bestDrop;
            } else if (bestPick != -1 && distanceMatrix[var][bestPick] + distanceMatrix[bestPick][bestPick + n] <= maxRouteDuration - distanceSinceDepot[var].min()) {
                v = bestPick;
            }

            int finalVal = v;

            return branch(() -> cp.post(equal(finalVar, finalVal)),
                    () -> cp.post(notEqual(finalVar, finalVal)));
        });
    }
}
