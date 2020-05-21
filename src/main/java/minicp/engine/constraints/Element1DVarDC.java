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

import java.util.Arrays;
import java.util.HashSet;

public class Element1DVarDC extends AbstractConstraint {

    private final IntVar[] array;
    private final IntVar y;
    private final IntVar z;

    private static int[] supportT; // Used for residue caching.
    private static boolean[] residueAssigned; // Used for residue caching.


    public Element1DVarDC(IntVar[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.array = array;
        supportT = new int[array.length];
        residueAssigned = new boolean[array.length];
        this.y = y;
        this.z = z;
    }

    @Override
    public void post() {
        y.removeBelow(0);
        y.removeAbove(array.length - 1);
        y.propagateOnDomainChange(this);
        z.propagateOnDomainChange(this);
        propagate();
    }

    @Override
    public void propagate() {
        // Domain of y.
        int[] Dy = new int[y.size()];
        y.fillArray(Dy);

        // Domain of z.
        int[] Dz = new int[z.size()];
        z.fillArray(Dz);

        // Remove i from D(y) if T[i] and D(z) are disjoint.
        for (int i : Dy) {
            int[] DTi = new int[array[i].size()];
            array[i].fillArray(DTi);
            if (disjoint(DTi, Dz, i))
                y.remove(i);
        }

        // Remove v from D(z) if for all i in D(y), T[i] does not contain v.
        for (int v : Dz) {
            boolean remove = true;
            for (int i : Dy) {
                if (array[i].contains(v)) {
                    remove = false;
                    break;
                }
            }
            if (remove)
                z.remove(v);
        }

        // If only one possible value is left for y, post equality constraint.
        if (y.size() == 1)
            getSolver().post(new Equal(array[y.min()], z));
    }

    public static boolean disjoint(int[] D1, int[] D2, int i) {
        // If a residue has been assigned and is still valid, return false.
        if (residueAssigned[i]) {
            if (Arrays.binarySearch(D1, supportT[i]) >= 0 && Arrays.binarySearch(D2, supportT[i]) >= 0)
                return false;
            // If not, do not consider it anymore.
            residueAssigned[i] = true;
        }

        HashSet<Integer> set = new HashSet<>();

        // Add elements to the HashSet.
        for (int a : D1)
            set.add(a);

        // Check if some element matches.
        for (int b : D2) {
            if (set.contains(b)) {
                residueAssigned[i] = true;
                supportT[i] = b;
                return false;
            }
        }

        // Otherwise return true.
        return true;
    }
}
