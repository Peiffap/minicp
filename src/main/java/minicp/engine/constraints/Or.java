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
import minicp.engine.core.BoolVar;
import minicp.state.StateInt;
import minicp.util.exception.NotImplementedException;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * Logical or constraint {@code  x1 or x2 or ... xn}
 */
public class Or extends AbstractConstraint { // x1 or x2 or ... xn

    private final BoolVar[] x;
    private final int n;
    private StateInt wL; // watched literal left
    private StateInt wR; // watched literal right


    /**
     * Creates a logical or constraint: at least one variable is true:
     * {@code  x1 or x2 or ... xn}
     *
     * @param x the variables in the scope of the constraint
     */
    public Or(BoolVar[] x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        wL = getSolver().getStateManager().makeStateInt(0);
        wR = getSolver().getStateManager().makeStateInt(n - 1);
    }

    @Override
    public void post() {
        propagate();
    }


    @Override
    public void propagate() {
        // update watched literals
        // implement the filtering using watched literal technique and make sure you pass all the tests
        int lo = wL.value();
        int hi = wR.value();

        // Find leftmost unbound variable.
        while (lo < n && x[lo].isBound()) {
            // If some variable is true, short-circuit.
            if (x[lo].isTrue()) {
                wL.setValue(lo);
                this.setActive(false);
                return;
            }

            lo++;
        }

        // Find rightmost unbound variable.
        while (hi >= 0 && x[hi].isBound()) {
            // If some variable is true, short-circuit.
            if (x[hi].isTrue()) {
                wR.setValue(hi);
                this.setActive(false);
                return;
            }

            hi--;
        }

        if (lo > hi) { // No true variable found, return inconsistency.
            throw INCONSISTENCY;
        } else if (lo == hi) { // One variable left, must be true.
            x[lo].assign(true);
            this.setActive(false);
        } else { // Propagate if one of the watched literals gets bound.
            x[lo].propagateOnBind(this);
            x[hi].propagateOnBind(this);
        }

        // Update literals.
        wL.setValue(lo);
        wR.setValue(hi);
    }
}
