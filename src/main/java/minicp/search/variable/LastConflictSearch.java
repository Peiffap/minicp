package minicp.search.variable;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.util.function.Function;
import java.util.function.Supplier;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.equal;
import static minicp.cp.Factory.notEqual;

public class LastConflictSearch {

    private IntVar lastConflictVariable;
    private Supplier<IntVar> fallBackVariableSelector;
    private Function<IntVar, Integer> fallBackValueSelector;

    /**
     * Last conflict heuristic
     * Attempts to branch first on the last variable that caused an Inconsistency
     *
     * Lecoutre, C., Saïs, L., Tabary, S., & Vidal, V. (2009).
     * Reasoning from last conflict (s) in constraint programming.
     * Artificial Intelligence, 173(18), 1592-1614.
     *
     * @param variableSelector returns the next variable to bind
     * @param valueSelector given a variable, returns the value to which
     *                      it must be assigned on the left branch (and excluded on the right)
     */
    public LastConflictSearch(Supplier<IntVar> variableSelector, Function<IntVar, Integer> valueSelector) {
        this.fallBackVariableSelector = variableSelector; // Fallback variable selector.
        this.fallBackValueSelector = valueSelector; // Fallback value selector.
        this.lastConflictVariable = null;
    }


    /**
     * Returns the Supplier<Procedure[]> containing the various branches.
     */
    public Supplier<Procedure[]> search() {
        return () -> {
            IntVar branchVariable;

            // If the last conflict variable is bound/null, use fallback.
            if (lastConflictVariable == null || lastConflictVariable.isBound()) {
                branchVariable = fallBackVariableSelector.get(); // Fallback heuristic.
            } else {
                branchVariable = lastConflictVariable; // Use last conflict variable to branch on.
            }

            // If all variables are bound, return empty procedure.
            if (branchVariable == null) {
                return EMPTY;
            }

            int branchValue = fallBackValueSelector.apply(branchVariable); // Compute value.
            Solver cp = branchVariable.getSolver(); // Get solver.

            // Branch on value, assign last conflict variable if the branching is inconsistent.
            return branch(
                    () -> {
                        try {
                            cp.post(equal(branchVariable, branchValue));
                        } catch (InconsistencyException e) {
                            lastConflictVariable = branchVariable;
                            throw e; // Rethrow error so minicp knows to backtrack.
                        }
                    },
                    () -> {
                        try {
                            cp.post(notEqual(branchVariable, branchValue));
                        } catch (InconsistencyException e) {
                            lastConflictVariable = branchVariable;
                            throw e; // Rethrow error so minicp knows to backtrack.
                        }
                    });
        };
    }
}