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
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.IntVarImpl;
import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.util.exception.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;


/**
 *
 * Element Constraint modeling {@code array[y] = z}
 *
 */
public class Element1DDomainConsistent extends AbstractConstraint {

    private final int[] t;
    private final IntVar y, z;

    /**
     * Creates an element constraint {@code array[y] = z}
     *
     * @param array the array to index
     * @param y the index variable
     * @param z the result variable
     */
    public Element1DDomainConsistent(int[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.t = array;
        this.y = y;
        this.z = z;
    }

    @Override
    public void post() {
        y.removeBelow(0);
        y.removeAbove(t.length - 1);
        y.propagateOnDomainChange(this);
        z.propagateOnDomainChange(this);
        propagate();
    }

    @Override
    public void propagate() {
        // remove from y all the values for which t[i] is not in z
        for (int i = 0; i < t.length; i++) {
            if (!z.contains(t[i])) {
                y.remove(i);
            }
        }

        int[] zSupp = new int[z.size()];

        int[] Dz = new int[z.size()];
        z.fillArray(Dz);
        int[] Dy = new int[y.size()];
        y.fillArray(Dy);

        // compute zSupp
        for (int ind = 0; ind < Dz.length; ind++) {
            int v = Dz[ind];
            for (int i : Dy) {
                if (t[i] == v) {
                    zSupp[ind]++;
                }
            }
        }

        // remove from z all values for which support is 0
        for (int i = 0; i < zSupp.length; i++) {
            if (zSupp[i] == 0) {
                z.remove(Dz[i]);
            }
        }
    }
}
