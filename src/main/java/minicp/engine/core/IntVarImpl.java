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

package minicp.engine.core;

import minicp.state.StateStack;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of a variable
 * with a {@link SparseSetDomain}.
 */
public class IntVarImpl implements IntVar {

    private Solver cp;
    private IntDomain domain;
    private StateStack<Constraint> onDomain;
    private StateStack<Constraint> onBind;
    private StateStack<Constraint> onBounds;

    private DomainListener domListener = new DomainListener() {
        @Override
        public void empty() {
            throw InconsistencyException.INCONSISTENCY; // Integer Vars cannot be empty
        }

        @Override
        public void bind() {
            scheduleAll(onBind);
        }

        @Override
        public void change() {
            scheduleAll(onDomain);
        }

        @Override
        public void changeMin() {
            scheduleAll(onBounds);
        }

        @Override
        public void changeMax() {
            scheduleAll(onBounds);
        }
    };

    /**
     * Creates a variable with the elements {@code {0,...,n-1}}
     * as initial domain.
     *
     * @param cp the solver in which the variable is created
     * @param n  the number of values with {@code n > 0}
     */
    public IntVarImpl(Solver cp, int n) {
        this(cp, 0, n - 1);
    }

    /**
     * Creates a variable with the elements {@code {min,...,max}}
     * as initial domain.
     *
     * @param cp the solver in which the variable is created
     * @param min the minimum value of the domain
     * @param max the maximum value of the domain with {@code max >= min}
     */
    public IntVarImpl(Solver cp, int min, int max) {
        if (min == Integer.MIN_VALUE || max == Integer.MAX_VALUE) throw new InvalidParameterException("consider reducing the domains, Integer.MIN _VALUE and Integer.MAX_VALUE not allowed");
        if (min > max) throw new InvalidParameterException("at least one setValue in the domain");
        this.cp = cp;
        domain = new SparseSetDomain(cp.getStateManager(), min, max);
        onDomain = new StateStack<>(cp.getStateManager());
        onBind = new StateStack<>(cp.getStateManager());
        onBounds = new StateStack<>(cp.getStateManager());
    }



    /**
     * Creates a variable with a given set of values as initial domain.
     *
     * @param cp the solver in which the variable is created
     * @param values the initial values in the domain, it must be nonempty
     */
    public IntVarImpl(Solver cp, Set<Integer> values) {
        this.cp = cp;
        int min = Collections.min(values);
        int max = Collections.max(values);
        domain = new SparseSetDomain(cp.getStateManager(), min, max); // add dense set
        onDomain = new StateStack<>(cp.getStateManager());
        onBind = new StateStack<>(cp.getStateManager());
        onBounds = new StateStack<>(cp.getStateManager());
        for (int i  = min; i <= max; i++)
            if (!values.contains(i))
                domain.remove(i, domListener); // domain \ values
    }

    @Override
    public Solver getSolver() {
        return cp;
    }

    @Override
    public boolean isBound() {
        return domain.size() == 1;
    }

    @Override
    public String toString() {
        return domain.toString();
    }

    @Override
    public void whenBind(Procedure f) {
        onBind.push(constraintClosure(f));
    }

    @Override
    public void whenBoundsChange(Procedure f) {
        onBounds.push(constraintClosure(f));
    }

    @Override
    public void whenDomainChange(Procedure f) {
        onDomain.push(constraintClosure(f));
    }

    private Constraint constraintClosure(Procedure f) {
        Constraint c = new ConstraintClosure(cp, f);
        getSolver().post(c, false);
        return c;
    }

    @Override
    public void propagateOnDomainChange(Constraint c) {
        onDomain.push(c);
    }

    @Override
    public void propagateOnBind(Constraint c) {
        onBind.push(c);
    }

    @Override
    public void propagateOnBoundChange(Constraint c) {
        onBounds.push(c);
    }


    protected void scheduleAll(StateStack<Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++)
            cp.schedule(constraints.get(i));
    }

    @Override
    public int min() {
        return domain.min();
    }

    @Override
    public int max() {
        return domain.max();
    }

    @Override
    public int size() {
        return domain.size();
    }

    @Override
    public int fillArray(int[] dest) {
        return domain.fillArray(dest);
    }

    @Override
    public boolean contains(int v) {
        return domain.contains(v);
    }

    @Override
    public void remove(int v) {
        domain.remove(v, domListener);
    }

    @Override
    public void assign(int v) {
        domain.removeAllBut(v, domListener);
    }

    @Override
    public void removeBelow(int v) {
        domain.removeBelow(v, domListener);
    }

    @Override
    public void removeAbove(int v) {
        domain.removeAbove(v, domListener);
    }
}
