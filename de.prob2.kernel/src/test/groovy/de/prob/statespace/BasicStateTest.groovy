package de.prob.statespace

import java.nio.file.Paths

import de.prob.animator.domainobjects.ClassicalB
import de.prob.animator.domainobjects.StateError
import de.prob.cli.CliTestCommon
import de.prob.model.representation.CSPModel
import de.prob.scripting.ClassicalBFactory

import spock.lang.Specification

class BasicStateTest extends Specification {
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

	def "toString is id"() {
		expect:
		root.toString() == "root"
	}

	def "id is id"() {
		expect:
		root.getId() == "root"
		firstState.getId() == "0"
	}

	def "you can also get the numerical id"() {
		expect:
		root.numericalId() == -1
		firstState.numericalId() == 0
	}

	def "it is possible to get the state representation of a state"() {
		when:
		def rep = secondState.getStateRep()

		then:
		firstState.eval(rep).getValue() == "FALSE"
		secondState.eval(rep).getValue() == "TRUE"
	}

	def "if the model is not of type B, then it will not be generated"() {
		setup:
		def oldmodel = s.getModel()
		
		
		def model = CliTestCommon.injector.getInstance(CSPModel.class)

		s.model = model

		expect:
		secondState.getStateRep() == "unknown"

		cleanup:
		s.model = oldmodel
	}

	def "equivalence in a state is based on id and state space"() {
		when:
		def sameroot = new State("root", s)
		def otherroot = new State("root", Mock(StateSpace))

		then:
		root == sameroot
		sameroot == root
		root != otherroot
		otherroot != root
	}

	def "a state is not equal to something else"() {
		expect:
		root != "I'm not a State!"
	}

	def "hashcode is based also on id and state space"() {
		when:
		def sameroot = new State("root", s)
		def otherroot = new State("root", Mock(StateSpace))

		then:
		root.hashCode() == sameroot.hashCode()
		root.hashCode() != otherroot.hashCode()
	}

	def "isConstantsSetUp contacts Prolog to check constants setup status (if it isn't explored)"() {
		setup:
		root.constantsSetUp = false
		root.explored = false

		when:
		def constantsSetUp = root.isConstantsSetUp()

		then:
		constantsSetUp
		root.isExplored()
	}

	def "if cached, constantsSetUp doesn't contact prolog"() {
		setup:
		root.constantsSetUp = false
		root.explored = true

		when:
		def constantsSetUp = root.isConstantsSetUp()

		then:
		!constantsSetUp
	}

	def "isInitialised contacts Prolog to check initialisation status (if it isn't explored)"() {
		setup:
		root.initialised = true
		root.explored = false

		when:
		def init = root.isInitialised()

		then:
		!init
		root.isExplored()
	}

	def "if cached, initialised doesn't contact prolog"() {
		setup:
		root.initialised = true
		root.explored = true

		when:
		def init = root.isInitialised()

		then:
		init
	}

	def "isInvariantOk contacts Prolog to check invariant status (if it isn't explored)"() {
		setup:
		firstState.invariantOk = false
		firstState.explored = false

		when:
		def invok = firstState.isInvariantOk()

		then:
		invok
		firstState.isExplored()
	}

	def "if cached, invariantOk doesn't contact prolog"() {
		setup:
		firstState.invariantOk = false
		firstState.explored = true

		when:
		def invok = firstState.isInvariantOk()

		then:
		!invok
	}

	def "isMaxTransitionsCalculated contacts Prolog to check max transitions status (if it isn't explored)"() {
		setup:
		firstState.maxTransitionsCalculated = true
		firstState.explored = false

		when:
		def maxtrans = firstState.isMaxTransitionsCalculated()

		then:
		!maxtrans
		firstState.isExplored()
	}

	def "isMaxTransitionsCalculated doesn't contact prolog if cached"() {
		setup:
		firstState.maxTransitionsCalculated = true
		firstState.explored = true

		when:
		def maxtrans = firstState.isMaxTransitionsCalculated()

		then:
		maxtrans
	}

	def "isTimeoutOccurred contacts Prolog to check if a timeout occurred (if it isn't explored)"() {
		setup:
		firstState.timeoutOccurred = true
		firstState.explored = false

		when:
		def timeout = firstState.isTimeoutOccurred()

		then:
		!timeout
		firstState.isExplored()
	}

	def "isTimeoutOccurred doesn't contact prolog if cached"() {
		setup:
		firstState.timeoutOccurred = true
		firstState.explored = true

		when:
		def timeout = firstState.isTimeoutOccurred()

		then:
		timeout
	}

	def "getTransitionsWithTimeout contacts prolog if not explored"() {
		setup:
		firstState.transitionsWithTimeout = new HashSet<String>(["blah", "blub"])
		firstState.explored = false

		when:
		def transitions = firstState.getTransitionsWithTimeout()

		then:
		transitions.isEmpty()
		firstState.isExplored()
	}

	def "getTransitionsWithTimeout does not contacts prolog if explored"() {
		setup:
		def set = new HashSet<String>(["blah", "blub"])
		firstState.transitionsWithTimeout = set
		firstState.explored = true

		when:
		def transitions = firstState.getTransitionsWithTimeout()

		then:
		transitions == set
	}

	def "getStateErrors contacts prolog if not explored"() {
		setup:
		def errors = [
			new StateError("blah", "it went wront", "it went wrong")
		]
		firstState.stateErrors = errors
		firstState.explored = false

		when:
		def errs = firstState.getStateErrors()

		then:
		errs.isEmpty()
		firstState.isExplored()
	}

	def "getStateErrors does not contacts prolog if explored"() {
		setup:
		def errors = [
			new StateError("blah", "it went wront", "it went wrong")
		]
		firstState.stateErrors = errors
		firstState.explored = true

		when:
		def errs = firstState.getStateErrors()

		then:
		errs == errors
	}

	def "state contacts prolog to calculate out transitions if not explored"() {
		setup:
		root.explored = false
		root.transitions = [
			Transition.generateArtificialTransition(s, "blah", "desc", "srcId", "destId")
		]

		when:
		def transitions = root.getOutTransitions()

		then:
		transitions.size() == 1
		transitions[0].getName() == "\$initialise_machine"
	}

	def "state returns cached transitions if not explored"() {
		setup:
		root.explored = true
		def t = [
			Transition.generateArtificialTransition(s, "blah", "desc", "srcId", "destId")
		]
		root.transitions = t

		when:
		def transitions = root.getOutTransitions()

		then:
		transitions == t
	}

	def "getOutTransitions doesn't evaluate by default"() {
		setup:
		firstState.explored = false

		when:
		def transitions = firstState.getOutTransitions()

		then:
		transitions.every {!it.isEvaluated()}
	}

	def "explore changes all the values"() {
		setup:
		def f = new ClassicalB("1+2")
		s.subscribe(root, [f])
		root.transitions = []
		root.constantsSetUp = false
		root.initialised = true
		root.invariantOk = false
		root.timeoutOccurred = true
		root.maxTransitionsCalculated = true
		root.stateErrors = [
			new StateError("blah", "blub", "blih")
		]
		root.transitionsWithTimeout = new HashSet<String>(["blah"])
		root.explored = false


		when:
		root.explore()

		then:
		!root.transitions.isEmpty()
		!root.getValues().isEmpty()
		root.constantsSetUp == true
		root.initialised == false
		root.invariantOk == true
		root.timeoutOccurred == false
		root.maxTransitionsCalculated == false
		root.stateErrors.isEmpty()
		root.transitionsWithTimeout.isEmpty()
		root.explored == true

		cleanup:
		s.unsubscribe(root, f)
	}
}
