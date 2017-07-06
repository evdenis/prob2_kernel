package de.prob.model.brules;

import static de.prob.util.DebugPrinter.debugPrint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.be4.classicalb.core.parser.ParsingBehaviour;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.rules.RulesProject;
import de.prob.animator.domainobjects.StateError;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.util.StopWatch;

public class RulesMachineRun {

	public enum ERROR_TYPES {
		PARSE_ERROR, PROB_ERROR, UNEXPECTED_ERROR
	}

	private final RulesMachineRunner prob2Runner = RulesMachineRunner.getInstance();

	private RulesProject rulesProject;
	private ExecuteRun executeRun;

	private final List<Error> errors = new ArrayList<>();

	private final File runnerFile;
	private final Map<String, String> injectedConstantValues;

	private RuleResults rulesResult;

	
	public RulesMachineRun(File runner) {
		this(runner, new HashMap<String, String>());
	}
	
	public RulesMachineRun(File runner, Map<String, String> injectedConstantValues) {
		this.runnerFile = runner;
		this.injectedConstantValues = injectedConstantValues;
	}

	public void start() {
		debugPrint("------- Starting RulesMachine Run: " + this.runnerFile.getAbsolutePath());
		StopWatch.start("parsing");
		boolean hasParseErrors = parseAndTranslateRulesProject();
		debugPrint(StopWatch.getRunTimeAsString("parsing"));
		if (hasParseErrors) {
			return;
		}

		this.executeRun = prob2Runner.createRulesMachineExecuteRun(this.rulesProject, runnerFile);

		try {
			StopWatch.start("prob2Run");
			debugPrint("Start execute ...");

			// start
			this.executeRun.start();

			debugPrint("End execute.");
			debugPrint(StopWatch.getRunTimeAsString("prob2Run"));
		} catch (ProBError e) {
			debugPrint("****ProBError: " + e.getMessage());
			if (executeRun.getExecuteModelCommand() != null) {
				State finalState = executeRun.getExecuteModelCommand().getFinalState();
				Collection<StateError> stateErrors = finalState.getStateErrors();
				for (StateError stateError : stateErrors) {
					this.errors.add(new Error(ERROR_TYPES.PROB_ERROR, stateError.getShortDescription(), e));
					return;
				}
			}
			this.errors.add(new Error(ERROR_TYPES.PROB_ERROR, e.getMessage(), e));
			return;
		} catch (Exception e) {
			// storing all error messages
			debugPrint("****Unkown error: " + e.getMessage());
			this.errors.add(new Error(ERROR_TYPES.PROB_ERROR, e.getMessage(), e));
			return;
		}
		StopWatch.start("ExtractResults");
		this.rulesResult = new RuleResults(this.rulesProject, executeRun.getFinalState());
		debugPrint(StopWatch.getRunTimeAsString("ExtractResults"));
	}

	private boolean parseAndTranslateRulesProject() {
		this.rulesProject = new RulesProject(runnerFile);
		rulesProject.setParsingBehaviour(new ParsingBehaviour());

		for (Entry<String, String> pair : injectedConstantValues.entrySet()) {
			rulesProject.addConstantValue(pair.getKey(), pair.getValue());
		}

		/*
		 * parse errors and errors from semantic checks are stored in the
		 * rulesProject
		 */
		rulesProject.translateProject();
		if (rulesProject.hasErrors()) {
			BException bException = rulesProject.getBExceptionList().get(0);
			String message = bException.getMessage();
			debugPrint("****ParseError: " + message);
			this.errors.add(new Error(ERROR_TYPES.PARSE_ERROR, message, bException));
		}
		return rulesProject.hasErrors();
	}

	public boolean hasError() {
		return !this.errors.isEmpty();
	}

	public List<Error> getErrorList() {
		return new ArrayList<>(this.errors);
	}

	public Error getError() {
		return this.errors.get(0);
	}

	public RulesProject getRulesProject() {
		return this.rulesProject;
	}

	public RulesMachineRunner getProb2Runner() {
		return this.prob2Runner;
	}

	public RuleResults getRuleResults() {
		return this.rulesResult;
	}

	public ExecuteRun getExecuteRun() {
		return this.executeRun;
	}

	public File getMainMachineFile() {
		return runnerFile;
	}

	public Map<String, String> getInjectedConstantsValues() {
		return new HashMap<>(this.injectedConstantValues);
	}

	public class Error {
		final ERROR_TYPES type;
		final String message;
		final Exception exception;

		public ERROR_TYPES getType() {
			return this.type;
		}

		public String getMessage() {
			return this.message;
		}

		public Exception getException() {
			return this.exception;
		}

		@Override
		public String toString() {
			return type + ": " + message;
		}

		Error(ERROR_TYPES type, String message, Exception exception) {
			this.type = type;
			this.message = message;
			this.exception = exception;
		}
	}

}
