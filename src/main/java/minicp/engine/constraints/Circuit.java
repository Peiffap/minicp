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

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.state.StateInt;
import minicp.util.exception.NotImplementedException;

import static minicp.cp.Factory.allDifferent;

/**
 * Hamiltonian Circuit Constraint with a successor model
 */
public class Circuit extends AbstractConstraint {

    private final IntVar[] x;
    private final StateInt[] dest;
    private final StateInt[] orig;
    private final StateInt[] lengthToDest;

    /**
     * Creates an Hamiltonian Circuit Constraint
     * with a successor model.
     *
     * @param x the variables representing the successor array that is
     *          {@code x[i]} is the city visited after city i
     */
    public Circuit(IntVar[] x) {
        super(x[0].getSolver());
        assert (x.length > 0);
        this.x = x;
        dest = new StateInt[x.length];
        orig = new StateInt[x.length];
        lengthToDest = new StateInt[x.length];
        for (int i = 0; i < x.length; i++) {
            dest[i] = getSolver().getStateManager().makeStateInt(i);
            orig[i] = getSolver().getStateManager().makeStateInt(i);
            lengthToDest[i] = getSolver().getStateManager().makeStateInt(0);
        }
    }


    @Override
    public void post() {
        // Post allDifferent constraint.
        getSolver().post(allDifferent(x));

        for (int i = 0; i < x.length; ++i) {
            int k = i;
            x[i].whenBind(() -> this.bind(k));

            if (x[i].isBound())
                bind(i);

            // Remove values outside the bounds.
            x[i].removeBelow(0);
            x[i].removeAbove(x.length - 1);

            // Remove sub-tours.
            if (x.length > 1)
                x[i].remove(i);
        }
    }


    private void bind(int i) {
        // Get the successor, destination and origin of the bound variable.
        int succ = x[i].min();
        int d = dest[succ].value();
        int origin = orig[i].value();

        // Destination of origin becomes destination of successor.
        dest[origin].setValue(d);

        // Origin of destination becomes origin of bound variable.
        orig[d].setValue(origin);

        // Total length from origin to new destination.
        // Computed as
        // current length from origin to i
        //  + length from succ to its destination
        //  + the length from i to succ (which is always one).
        lengthToDest[origin].setValue(lengthToDest[origin].value() + lengthToDest[succ].value() + 1);

        int len = lengthToDest[origin].value();

        // If the path is not yet a circuit,
        // we remove the origin as potential destination to avoid sub-tours.
        if (len < x.length - 1) {
            x[d].remove(origin);
        }
    }
}
