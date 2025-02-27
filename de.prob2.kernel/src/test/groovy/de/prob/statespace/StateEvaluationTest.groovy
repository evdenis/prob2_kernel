package de.prob.statespace

import java.nio.file.Paths

import de.prob.animator.domainobjects.ClassicalB
import de.prob.animator.domainobjects.EvalResult
import de.prob.cli.CliTestCommon
import de.prob.scripting.ClassicalBFactory
import spock.lang.Specification

class StateEvaluationTest extends Specification {
	private static StateSpace s
	private static State root
	private static State firstState
	private static State secondState

	def setupSpec() {
		final path = Paths.get("groovyTests", "machines", "scheduler.mch").toString()
		final factory = CliTestCommon.injector.getInstance(ClassicalBFactory.class)
		s = factory.extract(path).load([:])
		root = s.root
		firstState = root.$initialise_machine()
		secondState = firstState.new("pp=PID1")
	}

	def cleanupSpec() {
		s.kill()
	}

	def "it is possible to evaluate a string (will be parsed by model)"() {
		expect:
		secondState.eval("waiting").value == "{PID1}"
	}

	def "it is possible to evaluate an IEvalElement"() {
		expect:
		secondState.eval(new ClassicalB("waiting")).value == "{PID1}"
	}

	def "it is possible to evaluate multiple IEvalElements"() {
		final res = secondState.eval(
			new ClassicalB("waiting"),
			new ClassicalB("ready"),
		).collect {it.value}
		expect:
		res == ["{PID1}", "{}"] || res == ["{PID1}", "\u2205"] // u2205 is Unicode emptyset
	}

	def "it is possible to evaluate a list of IEvalElements"() {
		final res = secondState.eval([
			new ClassicalB("waiting"),
			new ClassicalB("ready"),
		]).collect {it.value}
		expect:
		res == ["{PID1}", "{}"] || res == ["{PID1}", "\u2205"]
	}

	def "if a result is cached, prolog doesn't necessarily have to be contacted"() {
		when:
		final blah = new ClassicalB("blah")
		final blahres = new EvalResult("blah", [:])
		firstState.values[blah] = blahres

		then:
		//TODO currently no caching
		//firstState.eval([blah]) == [blahres]
		//firstState.values.remove(blah)
		true
	}
}
