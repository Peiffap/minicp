package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.SparseSetDomain;
import minicp.state.StateInt;
import minicp.state.StateSparseSet;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.IntStream;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class AllDifferentFW extends AbstractConstraint {

    private IntVar[] x;
    private int n;
    private StateSparseSet unBounds;

    public AllDifferentFW(IntVar... x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        unBounds = new StateSparseSet(getSolver().getStateManager(), n, 0);
    }

    @Override
    public void post() {
        for (int i = 0; i < x.length; i++) {
            final int ii = i;

            // When bound => propagate.
            x[i].whenBind(() -> bind(ii));

            // If already bound, should still propagate.
            if (x[i].isBound()) {
                bind(i);
            }
        }
    }

    public void bind(int i) {
        unBounds.remove(i); // x[i] is now bound.

        int[] domUnbounds = new int[unBounds.size()];
        unBounds.fillArray(domUnbounds);

        for (int index: domUnbounds) {
            x[index].remove(x[i].min());
        }

        // Optimization: deactivate the constraint if all variable are bounds.
        if (unBounds.isEmpty()) {
            this.setActive(false);
        }
    }

}