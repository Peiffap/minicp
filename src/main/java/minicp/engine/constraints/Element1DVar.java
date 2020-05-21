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
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;

import static minicp.cp.Factory.makeIntVar;

public class Element1DVar extends AbstractConstraint {

    private final IntVar[] T;
    private final IntVar x;
    private final IntVar y;
    private final int n;

    public Element1DVar(IntVar[] T, IntVar x, IntVar y) {
        super(y.getSolver());

        this.T = T;
        this.n = T.length;
        this.x = x;
        this.y = y;
    }

    @Override
    public void post() {
        x.removeBelow(0);
        x.removeAbove(n - 1);

        x.propagateOnDomainChange(this);
        y.propagateOnBoundChange(this);
        propagate();
    }

    @Override
    public void propagate() {

        int[] xDomain = new int[x.size()];
        int xSize = x.fillArray(xDomain);

        for (int i = 0; i < xSize; ++i) {
            IntVar t = T[xDomain[i]];
            int min = t.min();
            int max = y.max();

            if (min > max) {
                x.remove(xDomain[i]);
            }

            min = y.min();
            max = t.max();

            if (min > max) {
                x.remove(xDomain[i]);
            }
        }

        if (xSize != x.size()) {
            xSize = x.fillArray(xDomain);
        }

        int tMin = Integer.MAX_VALUE;
        int tMax = Integer.MIN_VALUE;
        for (int i = 0; i < xSize; ++i) {
            IntVar t = T[xDomain[i]];
            if (t.min() < tMin) {
                tMin = t.min();
            }

            if (t.max() > tMax) {
                tMax = t.max();
            }
        }

        int yMin = Math.max(y.min(), tMin);
        int yMax = Math.min(y.max(), tMax);

        y.removeBelow(yMin);
        y.removeAbove(yMax);


        if (x.isBound()) {
            IntVar t = T[x.min()];
            y.removeBelow(Math.max(y.min(), t.min()));
            y.removeAbove(Math.min(y.max(), t.max()));

            t.removeBelow(Math.max(y.min(), t.min()));
            t.removeAbove(Math.min(y.max(), t.max()));
        }

    }
}