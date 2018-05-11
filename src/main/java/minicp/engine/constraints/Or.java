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
 * Copyright (c)  2017. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.BoolVar;
import minicp.engine.core.Constraint;
import minicp.reversible.ReversibleInt;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;

import static minicp.util.InconsistencyException.INCONSISTENCY;

public class Or extends Constraint { // x1 or x2 or ... xn

    private final BoolVar[] x;
    private final int n;
    private ReversibleInt wL ; // watched literal left
    private ReversibleInt wR ; // watched literal right


    public Or(BoolVar[] x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        wL = new ReversibleInt(cp.getTrail(),0);
        wR = new ReversibleInt(cp.getTrail(),n-1);
    }

    @Override
    public void post() throws InconsistencyException {
        propagate();
    }


    @Override
    public void propagate() throws InconsistencyException {
        // TODO: implement the filtering using watched literal technique and make sure you pass all the tests
        throw new NotImplementedException();
    }
}
