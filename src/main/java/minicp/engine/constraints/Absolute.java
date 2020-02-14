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
import minicp.util.exception.NotImplementedException;

/**
 * Absolute value constraint
 */
public class Absolute extends AbstractConstraint {

    private final IntVar x;
    private final IntVar y;

    /**
     * Creates the absolute value constraint {@code y = |x|}.
     *
     * @param x the input variable such that its absolute value is equal to y
     * @param y the variable that represents the absolute value of x
     */
    public Absolute(IntVar x, IntVar y) {
        super(x.getSolver());
        this.x = x;
        this.y = y;
    }

    public void post() {
        propagate();
        x.propagateOnDomainChange(this);
        y.propagateOnDomainChange(this);
    }

    @Override
    public void propagate() {
        int[] domValx = new int[x.size()]; //
        int[] domValy = new int[y.size()];
        if (y.isBound())
            pruneAbsolute(y, x, domValx); // only retain y and -y
        else if (x.isBound())
            y.assign(Math.abs(x.min())); // only retain x
        else {
            y.removeBelow(0); // this can always be done; y is an absolute value
            pruneAbsolute(y, x, domValx);
            pruneEquals(x, y, domValy);
            x.whenDomainChange(() -> {
                pruneEquals(x, y, domValy);
            });
            y.whenDomainChange(() -> {
                pruneAbsolute(y, x, domValx);
            });
        }
    }

    // dom consistent filtering in the direction from -> to
    // every value of to has a support in from
    private void pruneEquals(IntVar from, IntVar to, int[] domVal) {
        // dump the domain of to into domVal
        to.fillArray(domVal);
        for (int k: domVal)
            if (!from.contains(k) && !from.contains(-k)) // check whether x contains either y or -y
                to.remove(k);
    }

    // dom consistent filtering in the direction from -> to
    // every value of to has a support in from
    private void pruneAbsolute(IntVar from, IntVar to, int[] domVal) {
        // dump the domain of to into domVal
        to.fillArray(domVal);
        for (int k : domVal)
            if (!from.contains(Math.abs(k))) { // check if y contains absolute value of x
                to.remove(k);
            }
    }
}
