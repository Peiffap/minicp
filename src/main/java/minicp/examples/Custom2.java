package minicp.examples;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.util.exception.InconsistencyException;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;
import static minicp.examples.DialARide.distance;
import static minicp.examples.DialARide.stopType;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class Custom2 {

    public static DFSearch custom(Solver cp, IntVar[] succ, IntVar[] prec, int n, int k, int[][] distanceMatrix, int maxRouteDuration, IntVar[] distanceSinceDepot, ArrayList<DialARide.RideStop> allStops, IntVar[] vehicles) {
        return makeDfs(cp, () -> {
            Integer[] indexes = new Integer[2*k+2*n];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = i;
            }
            Integer id = selectMin(
                    indexes,
                    xi -> (prec[xi].isBound() && succ[xi].size() > 1) || (xi < 2*k && !succ[xi].isBound()) ,
                    xi -> distanceSinceDepot[xi].max());

            if (id == null)
                return EMPTY;
            IntVar xs = succ[id];
            int[] val = new int[xs.size()];
            xs.fillArray(val);
            Integer[] values = new Integer[val.length];
            for (int i = 0; i < val.length; ++i)
                values[i] = val[i];
            double eval;
            int dist;
            double minEval = Double.MAX_VALUE;
            int currentRouteId = vehicles[id].min();
            // tries to select a valid drop
            Integer v = selectMin(
                    values,
                    xi -> !prec[xi].isBound() && xi >= 2*k + n && isEqual(vehicles[xi], currentRouteId).isTrue(),
                    xi -> distanceSinceDepot[xi].max());
            if (v == null) {
                // tries to go to a pickup
                v = selectMin(
                        values,
                        xi -> !prec[xi].isBound() && xi >= 2*k && xi < 2*k + n,
                        xi -> distanceSinceDepot[xi].max());
                // no valid pickup available -> select a depot
                if (v == null) {
                    v = selectMin(
                            values,
                            xi -> !prec[xi].isBound() && (xi < 2*k || xi >= 2*k + n),
                            xi -> distanceSinceDepot[xi].max() );
                    if (v==null)
                        return EMPTY;
                }
            }

            int finalV = v;

            return branch(() -> cp.post(equal(xs, finalV)),
                    () -> cp.post(notEqual(xs, finalV)));
        });
    }
}
