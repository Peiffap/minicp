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
 * Maximum Constraint
 */
public class Maximum extends AbstractConstraint {

    private final IntVar[] x;
    private final IntVar y;

    /**
     * Creates the maximum constraint y = maximum(x[0],x[1],...,x[n])?
     *
     * @param x the variable on which the maximum is to be found
     * @param y the variable that is equal to the maximum on x
     */
    public Maximum(IntVar[] x, IntVar y) {
        super(x[0].getSolver());
        assert (x.length > 0);
        this.x = x;
        this.y = y;
    }


    @Override
    public void post() {
        propagate();
        for (IntVar xi : x)
            xi.propagateOnDomainChange(this);
        y.propagateOnDomainChange(this);
    }


    @Override
    public void propagate() {
        for (IntVar xi : x)
            xi.removeAbove(y.max()); // xi cannot be greater than max value of y
        int maxval = Integer.MIN_VALUE;
        int minval = Integer.MIN_VALUE;
        for (IntVar xi : x) {
            maxval = Math.max(maxval, xi.max()); // maximum of maximums
            minval = Math.max(minval, xi.min()); // maximum of minimums
        }
        y.removeAbove(maxval); // remove what is greater than max value in x
        y.removeBelow(minval); // remove what is lower than the largest minimum

        // if exactly one IntVar is left which intersects y (and can thus be the one used to get the max), both must be equal
        int cnt = 0;
        IntVar tmp = null;
        for (IntVar xi : x) {
            if (intersects(xi, y)) {
                cnt++;
                tmp = xi;
                if (cnt > 1)
                    return;
            }
        }

        // post equality constraint
        if (cnt == 1) {
            y.getSolver().post(new Equal(tmp, y));
            setActive(false);
        }
    }

    private boolean intersects(IntVar x, IntVar y) {
        int xmin = x.min();
        int ymin = y.min();
        int xmax = x.max();
        int ymax = y.max();
        return (xmin >= ymin && xmin <= ymax) || (ymin >= xmin && ymin <= xmax);
    }
}
