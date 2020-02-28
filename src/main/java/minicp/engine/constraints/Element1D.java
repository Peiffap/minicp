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
import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.IntStream;

/**
 *
 * Element Constraint modeling {@code array[y] = z}
 *
 */
public class Element1D extends AbstractConstraint {

    private final int[] t;
    private final IntVar y, z;

    private final StateInt[] nColSup;
    private final StateInt low;
    private final StateInt up;

    private final ArrayList<Pair> yz;

    private static final class Pair implements Comparable<Pair> {
        private final int y, z;

        private Pair(int y, int z) {
            this.y = y;
            this.z = z;
        }

        @Override
        public int compareTo(Pair p) {
            return z - p.z;
        }
    }


    /**
     * Creates an element constraint {@code array[y] = z}
     *
     * @param array the array to index
     * @param y the index variable
     * @param z the result variable
     */
    public Element1D(int[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.t = array;

        this.yz = new ArrayList<Pair>();
        for (int i = 0; i < t.length; i++)
            yz.add(new Pair(i, t[i]));
        Collections.sort(yz);

        StateManager sm = getSolver().getStateManager();
        low = sm.makeStateInt(0);
        up = sm.makeStateInt(t.length - 1);

        this.y = y;
        this.z = z;

        nColSup = IntStream.range(0, t.length).mapToObj(i -> sm.makeStateInt(1)).toArray(StateInt[]::new); // init value = 1 because this denotes whether the pair is still available
    }

    @Override
    public void post() {
        y.removeBelow(0);
        y.removeAbove(t.length - 1);
        y.propagateOnDomainChange(this);
        z.propagateOnBoundChange(this);
        propagate();
    }

    private void updateSupport(int lostPos) {
        if (nColSup[yz.get(lostPos).y].decrement() == 0)
            y.remove(yz.get(lostPos).y);
    }

    @Override
    public void propagate() {
        int l = low.value(), u = up.value();
        int zMin = z.min(), zMax = z.max();

        while (yz.get(l).z < zMin || !y.contains(yz.get(l).y)) {
            updateSupport(l++);
            if (l > u) throw new InconsistencyException();
        }
        while (yz.get(u).z > zMax || !y.contains(yz.get(u).y)) {
            updateSupport(u--);
            if (l > u) throw new InconsistencyException();
        }
        z.removeBelow(yz.get(l).z);
        z.removeAbove(yz.get(u).z);
        low.setValue(l);
        up.setValue(u);
    }
}
