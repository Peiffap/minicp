package minicp.search.value;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;

import static minicp.cp.Factory.equal;

public class BoundImpactValueSelector {

    private IntVar x;
    private IntVar obj;

    /**
     * Fages, J. G., & Prud’Homme, C. Making the first solution good! In 2017 IEEE 29th International Conference on Tools with Artificial Intelligence (ICTAI). IEEE.
     * @param x
     * @param obj
     * @return the value that if assigned to v induced the least augmentation of the objective obj
     */
    public BoundImpactValueSelector(IntVar x, IntVar obj) {
        this.x = x;
        this.obj = obj;
    }

    public int value() {
        // Get domain.
        int[] domain = new int[x.size()];
        x.fillArray(domain);

        // Best known objective and value.
        int bestObjective = Integer.MAX_VALUE;
        int bestValue = x.min();

        Solver cp = x.getSolver();
        StateManager sm = cp.getStateManager();

        // Find the value with the lowest objective bound.
        for (int v: domain) {
            try {
                sm.saveState(); // Save state before posting constraint.
                cp.post(equal(x, v)); // Post equal constraint to find minimum objective bound.
                int omin = obj.min();

                // Note from TA: in case of equality, take lowest value.
                if (omin < bestObjective || (omin == bestObjective && v < bestValue)) {
                    bestObjective = obj.min();
                    bestValue = v;
                }
            } catch (InconsistencyException e) {
            } finally {
                sm.restoreState(); // Restore state.
            }
        }

        return bestValue;
    }
}
