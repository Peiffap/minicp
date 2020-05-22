package minicp.examples;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;
import static minicp.examples.DialARide.stopType;

public class Custom1 {

    public static DFSearch custom(Solver cp, IntVar[] succ, int n, int k, int[][] distanceMatrix, int maxRouteDuration, IntVar[] distanceSinceDepot) {
        return makeDfs(cp, () -> {
            int var = 0;
            while (succ[var].isBound()) {
                var = succ[var].min();
                if (var == 0) {
                    return EMPTY;
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
                        int metric = distanceMatrix[var][d];
                        if (metric < bDropMetric) {
                            bDropMetric = metric;
                            bestDrop = d;
                        }
                    }
                } else if (t == 'p') {
                    int metric = distanceMatrix[var][d] + distanceMatrix[d][d + n];
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
            } else if (bestPick != -1 && bPickMetric <= maxRouteDuration - distanceSinceDepot[var].min()) {
                v = bestPick;
            }

            int finalVal = v;

            return branch(() -> cp.post(equal(finalVar, finalVal)),
                    () -> cp.post(notEqual(finalVar, finalVal)));
        });
    }
}
