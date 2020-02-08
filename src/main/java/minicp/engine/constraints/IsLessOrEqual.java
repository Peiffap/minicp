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
 * Copyright (v)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

/**
 * Reified less or equal constraint.
 */
public class IsLessOrEqual extends AbstractConstraint { // b <=> x <= v

    private final BoolVar b;
    private final IntVar x;
    private final int v;

    /**
     * Creates a constraint that
     * link a boolean variable representing
     * whether one variable is less or equal to the given constant.
     * @param b a boolean variable that is true if and only if
     *         x takes a value less or equal to v
     * @param x the variable
     * @param v the constant
     * @see minicp.cp.Factory#isLessOrEqual(IntVar, int)
     */
    public IsLessOrEqual(BoolVar b, IntVar x, int v) {
        super(b.getSolver());
        this.b = b;
        this.x = x;
        this.v = v;
    }

    @Override
    public void post() {
        propagate();
        if (isActive()) {
            x.propagateOnDomainChange(this);
            b.propagateOnBind(this);
        }
    }

    @Override
    public void propagate() {
        if (b.isTrue()) {
            x.removeAbove(v); // x <= v hence all values above v can be removed from the domain.
            setActive(false);
        } else if (b.isFalse()) {
            x.removeBelow(v + 1); // x !<= v -> x > v hence all values below below v + 1 can be removed.
            setActive(false);
        } else if (x.min() > v) { // if the smallest value is larger than v, then the constraint must be false.
            b.assign(false);
            setActive(false);
        } else if (x.max() <= v) { // if the largest value is less than or equal to v, the constraint is always satisfied.
            b.assign(true);
            setActive(false);
        } else if (x.isBound()) { // if x has a single value in its domain, one can find whether the constraint is satisfied.
            b.assign(x.min() <= v);
            setActive(false);
        }
    }
}
