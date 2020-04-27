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
import minicp.engine.constraints.Profile.Rectangle;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;

import java.util.ArrayList;

import static minicp.cp.Factory.minus;
import static minicp.cp.Factory.plus;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * Cumulative constraint with time-table filtering
 */
public class Cumulative extends AbstractConstraint {

    private final IntVar[] start;
    private final int[] duration;
    private final IntVar[] end;
    private final int[] demand;
    private final int capa;
    private final boolean postMirror;


    /**
     * Creates a cumulative constraint with a time-table filtering.
     * At any time-point t, the sum of the demands
     * of the activities overlapping t do not overlap the capacity.
     *
     * @param start the start time of each activities
     * @param duration the duration of each activities (non negative)
     * @param demand the demand of each activities, non negative
     * @param capa the capacity of the constraint
     */
    public Cumulative(IntVar[] start, int[] duration, int[] demand, int capa) {
        this(start, duration, demand, capa, true);
    }

    private Cumulative(IntVar[] start, int[] duration, int[] demand, int capa, boolean postMirror) {
        super(start[0].getSolver());
        this.start = start;
        this.duration = duration;
        this.end = Factory.makeIntVarArray(start.length, i -> plus(start[i], duration[i]));
        this.demand = demand;
        this.capa = capa;
        this.postMirror = postMirror;
    }


    @Override
    public void post() {
        for (int i = 0; i < start.length; i++) {
            start[i].propagateOnBoundChange(this);
        }

        if (postMirror) {
            IntVar[] startMirror = Factory.makeIntVarArray(start.length, i -> minus(end[i]));
            getSolver().post(new Cumulative(startMirror, duration, demand, capa, false), false);
        }

        propagate();
    }

    @Override
    public void propagate() {
        Profile profile = buildProfile();
        // 2: check that the profile is not exceeding the capa otherwise throw an INCONSISTENCY
        Rectangle[] rectangles = profile.rectangles();
        // Check whether at least one rectangle in the profile exceeds the capacity.
        for (Rectangle r: rectangles) {
            if (r.height() > capa) {
                throw INCONSISTENCY;
            }
        }

        for (int i = 0; i < start.length; i++) {
            if (!start[i].isBound()) {
                // j is the index of the profile rectangle overlapping t
                int j = profile.rectangleIndex(start[i].min());
                // 3: push i to the right
                // hint:
                // Check that at every-point on the interval
                // [start[i].getMin() ... start[i].getMin()+duration[i]-1] there is enough space.
                // You may have to look-ahead on the next profile rectangle(s)
                // Be careful that the activity you are currently pushing may have contributed to the profile.

                // Start and end of interval.
                int first = start[i].min();
                int last = first + duration[i];

                int max = start[i].max();

                // rect is the profile rectangle overlapping at the current time.
                Rectangle rect = profile.get(profile.rectangleIndex(first));
                int rEnd = rect.end();
                int rHeight = rect.height();

                int removePosition = first - 1; // Position below which to remove.

                // Find the earliest starting time which does not violate the capa constraint.
                for (int t = first; t < last; t++) {
                    // Check whether this part of activity i has already been counted at t:
                    // beenCounted <=> start[i].max = max <= t < last = end[i].min(),
                    // where the second inequality is guaranteed by the for loop.
                    // and choose the demand left to contribute:
                    //  beenCounted => toContribute = 0,
                    // !beenCounted => toContribute = demand[i].
                    boolean tGeMax = t >= max;
                    int toContribute = tGeMax ? 0 : demand[i];

                    // Recompute height at t if needed.
                    if (rEnd <= t) {
                        rect = profile.get(profile.rectangleIndex(t));
                        rEnd = rect.end();
                        rHeight = rect.height();
                    }

                    // If the profile at t exceeds the capacity,
                    // then activity i must start after t.
                    if (rHeight + toContribute > capa) {
                        // If we are going to remove everything below t + 1,
                        // and t >= max, then we can already throw the error.
                        if (tGeMax) {
                            throw INCONSISTENCY;
                        } else {
                            removePosition = t;
                        }
                    }
                }

                // Remove every start position beyond the last failure.
                start[i].removeBelow(removePosition + 1);
            }
        }
    }

    public Profile buildProfile() {
        ArrayList<Rectangle> mandatoryParts = new ArrayList<Rectangle>();
        for (int i = 0; i < start.length; i++) {
            // 1: add mandatory part of activity i if any
            int s = start[i].max();
            int e = end[i].min();
            if (s < e) {
                mandatoryParts.add(new Rectangle(s, e, demand[i]));
            }
        }
        return new Profile(mandatoryParts.toArray(new Profile.Rectangle[0]));
    }
}
