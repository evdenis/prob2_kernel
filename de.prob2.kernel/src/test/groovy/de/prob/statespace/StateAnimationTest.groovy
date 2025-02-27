package de.prob.statespace

import java.nio.file.Paths

import de.prob.animator.domainobjects.ClassicalB
import de.prob.cli.CliTestCommon
import de.prob.scripting.ClassicalBFactory
import spock.lang.Specification

class StateAnimationTest extends Specification {
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

	def "for invoking method, names can be escaped with a \$ if it is needed"() {
		expect:
		root.$$initialise_machine() == firstState
	}

	def "it is possible to treat events on the state as methods on the class"() {
		expect:
		root.$initialise_machine() == firstState
		firstState.new("pp=PID1").eval(new ClassicalB("waiting")).getValue() == "{PID1}"
		firstState.new("pp=PID1") == secondState
	}

	def "you can use the perform event to execute a transition"() {
		expect:
		root.perform("\$initialise_machine") == firstState
		firstState.perform("new","pp=PID1") == secondState
	}

	def "the perform event can take a list of predicates as an argument"() {
		expect:
		root.perform("\$initialise_machine",[]) == firstState
		firstState.perform("new",["pp=PID1"]) == secondState
	}

	def "performing an illegal event results in an exception"() {
		when:
		root.perform("blah",[])

		then:
		thrown(IllegalArgumentException)
	}

	def "you can find a transition that is outgoing from the current state vararg"() {
		expect:
		root.findTransition("\$initialise_machine").name == "\$initialise_machine"
		firstState.findTransition("new", "pp=PID1").name == "new"
	}

	def "you can find a transition that is outgoing from the current state with a list of predicates"() {
		expect:
		root.findTransition("\$initialise_machine").name == "\$initialise_machine"
		firstState.findTransition("new", "pp=PID1").name == "new"
	}

	def "you can't find an illegal transition"() {
		expect:
		root.findTransition("blah", []) == null
	}

	def "transition that is cached will be simply returned if no predicates are given"() {
		when:
		final t = Transition.generateArtificialTransition(s, "blah", "blah", "blah", "blah")
		root.transitions << t

		then:
		root.findTransition("blah",[]) == t
		root.transitions.remove(t)
	}

	def "you can find transitions with or without a predicate"() {
		expect:
		firstState.findTransitions("new", [], 3).size() == 3
		firstState.findTransitions("new", ["pp=PID1"], 1)[0].params == ["PID1"]
	}

	def "can execute an event via the anyOperation method"() {
		when:
		final s2 = root.anyOperation()
		final s3 = root.anyOperation().anyOperation("new")
		final s4 = root.anyOperation().anyOperation(["new"])
		final s5 = root.anyOperation("blah") // will return original state

		then:
		s2 == firstState
		s3.eval("waiting").value != "{}"
		s4.eval("waiting").value != "{}"
		s5 == root
	}

	def "can execute an event via the anyEvent method"() {
		when:
		final s2 = root.anyEvent()
		final s3 = root.anyEvent().anyEvent("new")
		final s4 = root.anyEvent().anyEvent(["new"])
		final s5 = root.anyEvent("blah") // will return original state

		then:
		s2 == firstState
		s3.eval("waiting").value != "{}"
		s4.eval("waiting").value != "{}"
		s5 == root
	}
}
