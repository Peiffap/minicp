package minicp.search.variable;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.equal;
import static minicp.cp.Factory.notEqual;

public class ConflictOrderingSearch {

    private HashMap<IntVar, Integer> stamp;
    private int nConflicts;
    private Supplier<IntVar> fallBackVariableSelector;
    private Function<IntVar, Integer> fallBackValueSelector;

    /**
     * Conflict Ordering Search
     *
     * Gay, S., Hartert, R., Lecoutre, C., & Schaus, P. (2015).
     * Conflict ordering search for scheduling problems.
     * In International conference on principles and practice of constraint programming (pp. 140-148).
     * Springer.
     *
     * @param variableSelector returns the next variable to bind
     * @param valueSelector given a variable, returns the value to which
     *                      it must be assigned on the left branch (and excluded on the right)
     */
    public ConflictOrderingSearch(Supplier<IntVar> variableSelector, Function<IntVar, Integer> valueSelector) {
        this.fallBackVariableSelector = variableSelector; // Fallback variable selector.
        this.fallBackValueSelector = valueSelector; // Fallback value selector.
        nConflicts = 0;
        this.stamp = new HashMap<>();
    }


    /**
     * Returns the Supplier<Procedure[]> containing the various branches.
     */
    public Supplier<Procedure[]> search() {
        return () -> {
            int maxStamp = 0;
            IntVar lastVar = null;

            // Find unbound variable with highest timestamp.
            for (IntVar var: stamp.keySet()) {
                if (stamp.get(var) > maxStamp && !var.isBound()) {
                    lastVar = var;
                    maxStamp = stamp.get(var);
                }
            }

            // If the conflict ordering search variable is null, use fallback.
            if (lastVar == null) {
                lastVar = fallBackVariableSelector.get(); // Fallback heuristic.

                // If all variables are bound, return empty procedure.
                if (lastVar == null) {
                    return EMPTY;
                }
            }

            int branchValue = fallBackValueSelector.apply(lastVar); // Compute value.
            Solver cp = lastVar.getSolver(); // Get solver.

            final IntVar branchVariable = lastVar;

            // Branch on value, update timestamp if the branching is inconsistent.
            return branch(
                    () -> {
                        try {
                            cp.post(equal(branchVariable, branchValue));
                        } catch (InconsistencyException e) {
                            nConflicts++;
                            stamp.put(branchVariable, nConflicts);
                            throw e; // Rethrow error so minicp knows to backtrack.
                        }
                    },
                    () -> {
                        try {
                            cp.post(notEqual(branchVariable, branchValue));
                        } catch (InconsistencyException e) {
                            nConflicts++;
                            stamp.put(branchVariable, nConflicts);
                            throw e; // Rethrow error so minicp knows to backtrack.
                        }
                    });
        };
    }
}