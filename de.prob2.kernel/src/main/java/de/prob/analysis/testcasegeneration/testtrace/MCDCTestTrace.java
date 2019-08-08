package de.prob.analysis.testcasegeneration.testtrace;

import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.prob.analysis.mcdc.ConcreteMCDCTestCase;
import de.prob.analysis.testcasegeneration.Target;
import de.prob.statespace.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A test trace created by the test case generator with the aim to fulfill MC/DC coverage.
 *
 * In addition to the {@link TestTrace}, an MCDCTestTrace contains the MC/DC targets that were fulfilled along the trace
 * to allow for a more detailed output once the test case generation is finished.
 */
public class MCDCTestTrace extends TestTrace {

    private List<ConcreteMCDCTestCase> mcdcTargets;

    public MCDCTestTrace(List<String> priorTransitions, String newTransition, List<ConcreteMCDCTestCase> mcdcTargets,
                         boolean isComplete, boolean lastTransitionIsFeasible) {
        super(priorTransitions, newTransition, isComplete, lastTransitionIsFeasible);
        this.mcdcTargets = mcdcTargets;
    }

    public MCDCTestTrace(List<String> priorTransitions, String newTransition, List<ConcreteMCDCTestCase> mcdcTargets,
                         boolean isComplete, boolean lastTransitionIsFeasible, Trace trace) {
        super(priorTransitions, newTransition, isComplete, lastTransitionIsFeasible, trace);
        this.mcdcTargets = mcdcTargets;
    }

    private List<ConcreteMCDCTestCase> getMcdcTargets() {
        return mcdcTargets;
    }

    public MCDCTestTrace createNewTrace(List<String> priorTransitions, Target t, boolean isComplete) {
        List<ConcreteMCDCTestCase> newTestCaseList = new ArrayList<>(getMcdcTargets());
        newTestCaseList.add(new ConcreteMCDCTestCase(t.getGuard(), t.getFeasible()));
        return new MCDCTestTrace(priorTransitions, t.getOperation(), newTestCaseList, isComplete, t.getFeasible());
    }

    public MCDCTestTrace createNewTrace(List<String> priorTransitions, Target t, boolean isComplete, Trace trace) {
        List<ConcreteMCDCTestCase> newTestCaseList = new ArrayList<>(getMcdcTargets());
        newTestCaseList.add(new ConcreteMCDCTestCase(t.getGuard(), t.getFeasible()));
        return new MCDCTestTrace(priorTransitions, t.getOperation(), newTestCaseList, isComplete, t.getFeasible(), trace);
    }

    public String toString() {
        PrettyPrinter prettyPrinter;
        StringJoiner stringJoiner = new StringJoiner(", ", "{", "}");
        for (int i = 0; i < transitionNames.size(); i++) {
            prettyPrinter = new PrettyPrinter();
            mcdcTargets.get(i).getPredicate().apply(prettyPrinter);
            stringJoiner.add(transitionNames.get(i) + " [" + prettyPrinter.getPrettyPrint() + " -> "
                    + mcdcTargets.get(i).getTruthValue() + "]");
        }
        return stringJoiner.toString();
    }
}
