package de.prob.animator.domainobjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hhu.stups.prob.translator.BValue;
import de.hhu.stups.prob.translator.Translator;
import de.hhu.stups.prob.translator.exceptions.TranslationException;
import de.prob.parser.BindingGenerator;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.prolog.term.PrologTerm;
import de.prob.unicode.UnicodeTranslator;

import groovy.lang.MissingPropertyException;

public class EvalResult extends AbstractEvalResult {
	public static final  EvalResult TRUE = new EvalResult("TRUE", Collections.emptyMap());
	public static final  EvalResult FALSE = new EvalResult("FALSE", Collections.emptyMap());
	private static final  HashMap<String, EvalResult> formulaCache = new HashMap<>();

	private final String value;
	private final Map<String, String> solutions;

	public EvalResult(final String value, final Map<String, String> solutions) {
		super();
		this.value = value;
		this.solutions = solutions;
	}

	public Map<String, String> getSolutions() {
		return solutions;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		String v = UnicodeTranslator.toUnicode(value);
		if (solutions.isEmpty()) {
			return v;
		}

		return solutions.entrySet().stream()
			.map(e -> e.getKey() + " = " + UnicodeTranslator.toUnicode(e.getValue()))
			.collect(Collectors.joining(" ∧ ", v + " (", ")"));
	}

	/**
	 * Get the String representation of the value of the solution with the
	 * specified name
	 *
	 * @param name
	 *            of solution
	 * @return String representation of solution, or <code>null</code> if no
	 *         solution with that name exists
	 */
	public String getSolution(String name) {
		return solutions.get(name);
	}

	@Override
	public Object getProperty(final String property) {
		try {
			return super.getProperty(property);
		} catch (MissingPropertyException e) {
			if (this.getSolutions().containsKey(property)) {
				return this.getSolution(property);
			} else {
				throw e;
			}
		}
	}

	public <T extends BValue> TranslatedEvalResult<T> translate() throws TranslationException {
		T val = Translator.translate(value);
		Map<String, BValue> sols = new HashMap<>();
		Set<Map.Entry<String, String>> entrySet = solutions.entrySet();
		for (Map.Entry<String, String> entry : entrySet) {
			sols.put(entry.getKey(), Translator.translate(entry.getValue()));
		}
		return new TranslatedEvalResult<>(val, sols);
	}

	/**
	 * Translates the results from ProB into an {@link AbstractEvalResult}. This
	 * is intended mainly for internal use, for developers who are writing
	 * commands and want to translate them into an {@link AbstractEvalResult}.
	 *
	 * @param pt
	 *            PrologTerm
	 * @return {@link AbstractEvalResult} translation of pt
	 */
	public static AbstractEvalResult getEvalResult(PrologTerm pt) {
		if (pt instanceof ListPrologTerm) {
			/*
			 * If the evaluation was not successful, the result should be a
			 * Prolog list with the code on the first index and a list of errors
			 * This results therefore in a ComputationNotCompleted command
			 */
			final List<String> strings = PrologTerm.atomicStrings((ListPrologTerm)pt);
			final String code = strings.get(0);
			final List<String> errors = strings.subList(1, strings.size());
			return new ComputationNotCompletedResult(code, String.join(",", errors));
		} else if ("result".equals(pt.getFunctor())) {
			/*
			 * If the formula in question was a predicate, the result term will
			 * have the following form: result(Value,Solutions) where Value is
			 * 'TRUE','POSSIBLY TRUE', or 'FALSE' Solutions is then a list of
			 * triples bind(Name,Solution,PPSol) where Name is the name of the
			 * free variable calculated by ProB, Solution is the Prolog
			 * representation of the solution, and PPSol is the String pretty
			 * print of the solution calculated by Prolog.
			 *
			 * If the formula in question was an expression, the result term
			 * will have the following form: result(v(SRes,PRes),[],Code) where
			 * SRes is the string representation of the result calculated by
			 * ProB and PRes is the Prolog representation of the value.
			 *
			 * From this information, an EvalResult object is created.
			 */

			PrologTerm v = pt.getArgument(1);
			String value = v.getFunctor().intern();
			ListPrologTerm solutionList = BindingGenerator.getList(pt.getArgument(2));
			if ("TRUE".equals(value) && solutionList.isEmpty()) {
				return TRUE;
			}
			if ("FALSE".equals(value) && solutionList.isEmpty()) {
				return FALSE;
			}
			if (!"TRUE".equals(value) && !"FALSE".equals(value) && formulaCache.containsKey(value)) {
				return formulaCache.get(value);
			}

			if (v instanceof CompoundPrologTerm && v.getArity() == 2) {
				CompoundPrologTerm cpt = BindingGenerator.getCompoundTerm(v, 2);
				value = PrologTerm.atomicString(cpt.getArgument(1));
			}

			Map<String, String> solutions = solutionList.isEmpty() ? Collections.emptyMap() : new HashMap<>();

			for (PrologTerm t : solutionList) {
				CompoundPrologTerm cpt = BindingGenerator.getCompoundTerm(t, 2);
				solutions.put(PrologTerm.atomicString(cpt.getArgument(1)).intern(), PrologTerm.atomicString(cpt.getArgument(2)).intern());
			}

			EvalResult res = new EvalResult(value, solutions);
			if (!"TRUE".equals(value) && !"FALSE".equals(value)) {
				formulaCache.put(value, res);
			}
			return res;
		} else if ("errors".equals(pt.getFunctor())) {
			final CompoundPrologTerm errorsTerm = BindingGenerator.getCompoundTerm(pt, 2);
			final String errorType = PrologTerm.atomicString(errorsTerm.getArgument(1));
			final List<String> errors = PrologTerm.atomicStrings(BindingGenerator.getList(errorsTerm.getArgument(2)));
			switch (errorType) {
				case "NOT-WELL-DEFINED":
					return new WDError(errors);
				
				case "UNKNOWN":
					return new UnknownEvaluationResult(errors);
				
				case "IDENTIFIER(S) NOT YET INITIALISED; INITIALISE MACHINE FIRST":
					return new IdentifierNotInitialised(errors);
				
				default:
					throw new IllegalArgumentException("Unknown error type: " + errorType);
			}
		} else if ("enum_warning".equals(pt.getFunctor())) {
			return new EnumerationWarning();
		}
		throw new IllegalArgumentException("Unknown result type " + pt.toString());
	}
}
