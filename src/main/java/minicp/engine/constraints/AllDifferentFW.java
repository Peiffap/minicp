package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.state.StateInt;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static minicp.cp.Factory.makeIntVar;

public class AllDifferentFW extends AbstractConstraint {

    private IntVar[] x;
    private int n;
    private int[] unBounds;
    private StateInt nUnBounds;

    public AllDifferentFW(IntVar... x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        nUnBounds = getSolver().getStateManager().makeStateInt(n);
        unBounds = IntStream.range(0, n).toArray();
    }

    @Override
    public void post() {
        for (IntVar v : x) {
            v.propagateOnBind(this);
        }
        propagate();
    }

    @Override
    public void propagate() {
        // Create list with bound values.
        ArrayList<Integer> al = new ArrayList<>();
        int nU = nUnBounds.value();
        for (int i = nU - 1; i >= 0; i--) {
            int idx = unBounds[i];
            IntVar y = x[idx];
            if (y.isBound()) {
                al.add(x[idx].min());
                unBounds[i] = unBounds[nU - 1];
                unBounds[nU - 1] = idx;
                nU--;
            }
        }
        nUnBounds.setValue(nU);

        // Remove bound values from other domains.
        for (int i = nU - 1; i >= 0; i--) {
            for (int v : al) {
                int idx = unBounds[i];
                x[idx].remove(v);
            }
        }
    }

}