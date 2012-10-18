package de.prob.statespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.IAnimator;
import de.prob.animator.command.EvaluateFormulasCommand;
import de.prob.animator.command.ExploreStateCommand;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.ICommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvaluationResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.OpInfo;
import de.prob.model.classicalb.ClassicalBMachine;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EBMachine;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.IEntity;

/**
 * 
 * The StateSpace is where the animation of a given model is carried out. The
 * methods in the StateSpace allow the user to:
 * 
 * 1) Find new states and operations
 * 
 * 2) Move between states within the StateSpace to inspect them
 * 
 * 3) Perform random animation steps
 * 
 * 4) Evaluate custom predicates and expressions -
 * 
 * 5) Register listeners that are notified of animation steps/new states and
 * operations.
 * 
 * 6) View information about the current state
 * 
 * @author joy
 * 
 */
public class StateSpace extends StateSpaceGraph implements IAnimator {

	Logger logger = LoggerFactory.getLogger(StateSpace.class);

	private transient IAnimator animator;

	private ICommand loadcmd;

	private final HashSet<StateId> explored = new HashSet<StateId>();
	private final StateSpaceInfo info;

	private final HashMap<IEvalElement, Set<Object>> formulaRegistry = new HashMap<IEvalElement, Set<Object>>();

	private final List<IStateSpaceChangeListener> stateSpaceListeners = new ArrayList<IStateSpaceChangeListener>();

	private final HashMap<String, StateId> states = new HashMap<String, StateId>();
	private final HashMap<String, OpInfo> ops = new HashMap<String, OpInfo>();
	private IEntity model;
	private final Map<StateId, Map<IEvalElement, String>> values = new HashMap<StateId, Map<IEvalElement, String>>();

	public final StateId __root;

	@Inject
	public StateSpace(final IAnimator animator,
			final DirectedMultigraphProvider graphProvider,
			final StateSpaceInfo info) {
		super(graphProvider.get());
		this.animator = animator;
		this.info = info;
		__root = new StateId("root", "1", this);
		addVertex(__root);
		states.put(__root.getId(), __root);
	}

	public StateId getRoot() {
		this.explore(__root);
		return __root;
	}

	// MAKE CHANGES TO THE STATESPACE GRAPH
	/**
	 * Takes a state id and calculates the successor states, the invariant,
	 * timeout, etc.
	 * 
	 * @param state
	 */
	public void explore(final StateId state) {
		if (!containsVertex(state)) {
			throw new IllegalArgumentException("state " + state
					+ " does not exist");
		}

		final ExploreStateCommand command = new ExploreStateCommand(
				state.getId());
		animator.execute(command);
		info.add(state, command);

		explored.add(state);
		final List<OpInfo> enabledOperations = command.getEnabledOperations();

		for (final OpInfo op : enabledOperations) {
			if (!containsEdge(op)) {
				ops.put(op.id, op);
				notifyStateSpaceChange(op.id,
						containsVertex(getVertex(op.dest)));
				final StateId newState = new StateId(op.dest, op.targetState,
						this);
				addVertex(newState);
				states.put(newState.getId(), newState);
				addEdge(states.get(op.src), states.get(op.dest), op);
			}
		}

		// Testing!!!
		// if (!state.getId().equals("root")) {
		// evaluateIFormulas(state);
		// }
		getInfo().add(state, command);
	}

	public StateId getVertex(final String key) {
		return states.get(key);
	}

	public void explore(final String state) {
		explore(states.get(state));
	}

	public void explore(final int i) {
		final String si = String.valueOf(i);
		explore(si);
	}

	/**
	 * Takes the name of an operation and a predicate and finds Operations that
	 * satisfy the name and predicate at the given stateId. New Operations are
	 * added to the graph.
	 * 
	 * @param stateId
	 * @param name
	 * @param predicate
	 * @param nrOfSolutions
	 * @return list of operations
	 * @throws BException
	 */
	public List<OpInfo> opFromPredicate(final StateId stateId,
			final String name, final String predicate, final int nrOfSolutions)
			throws BException {
		final ClassicalB pred = new ClassicalB(predicate);
		final GetOperationByPredicateCommand command = new GetOperationByPredicateCommand(
				stateId.getId(), name, pred, nrOfSolutions);
		animator.execute(command);
		final List<OpInfo> newOps = command.getOperations();

		// (id,name,src,dest,args)
		for (final OpInfo op : newOps) {
			if (!containsEdge(op)) {
				ops.put(op.id, op);
				notifyStateSpaceChange(op.id,
						containsVertex(getVertex(op.dest)));
				addEdge(getVertex(op.src), getVertex(op.dest), op);
			}
		}
		return newOps;
	}

	/**
	 * Checks if the state with stateId is a deadlock
	 * 
	 * @param state
	 * @return returns if a specific state is deadlocked
	 */
	public boolean isDeadlock(final StateId state) {
		if (!isExplored(state)) {
			explore(state);
		}
		return outDegreeOf(state) == 0;
	}

	public boolean isDeadlock(final String state) {
		return isDeadlock(states.get(state));
	}

	/**
	 * Checks if the state with stateId has been explored yet
	 * 
	 * @param state
	 * @return returns if a specific state is explored
	 */
	public boolean isExplored(final StateId state) {
		if (!containsVertex(state)) {
			throw new IllegalArgumentException("Unknown State id");
		}
		return explored.contains(state);
	}

	// EVALUATE PART OF STATESPACE

	/**
	 * Adds an expression or predicate to the list of user formulas. This
	 * expression or predicate is evaluated and the result is added to the map
	 * of variables in the info object.
	 * 
	 * @param formula
	 * @throws BException
	 */
	public void addUserFormula(final IEvalElement formula) {
		formulaRegistry.put(formula, new HashSet<Object>());
		subscribe(this, formula);
	}

	public List<EvaluationResult> eval(final StateId stateId,
			final List<IEvalElement> code) throws BException {
		if (!containsVertex(stateId)) {
			throw new IllegalArgumentException("state does not exist");
		}

		if (code.isEmpty()) {
			return new ArrayList<EvaluationResult>();
		}

		final EvaluateFormulasCommand command = new EvaluateFormulasCommand(
				code, stateId.getId());
		execute(command);

		final List<EvaluationResult> values = command.getValues();

		return values;

	}

	/**
	 * Evaluates a single formula or an array of formulas (represented as
	 * strings) for the given state. Returns as list of EvaluationResults.
	 * 
	 * @param state
	 * @param code
	 * @return returns a list of evaluation results
	 * @throws BException
	 */
	public List<EvaluationResult> eval(final String state,
			final List<IEvalElement> code) throws BException {
		final StateId stateId = getVertex(state);
		return eval(stateId, code);
	}

	public void evaluateFormulas(final StateId state) {
		if (state.getId().equals("root")) {
			return;
		}
		final Set<IEvalElement> formulas = formulaRegistry.keySet();
		final List<IEvalElement> toEvaluate = new ArrayList<IEvalElement>();
		final HashMap<IEvalElement, String> valueMap = new HashMap<IEvalElement, String>();

		for (final IEvalElement iEvalElement : formulas) {
			if (!formulaRegistry.get(iEvalElement).isEmpty()) {
				toEvaluate.add(iEvalElement);
			}
		}
		List<EvaluationResult> results = null;
		try {
			results = eval(state, toEvaluate);
		} catch (final BException e) {
			System.out.println("Evaluation of formulas failed!");
			e.printStackTrace();
		}

		assert results.size() == toEvaluate.size();
		if (results != null) {
			for (int i = 0; i < results.size(); i++) {
				valueMap.put(toEvaluate.get(i), results.get(i).value);
			}
		}
		values.put(state, valueMap);
	}

	public List<EvaluationResult> eval(final String state, final String... code)
			throws BException {
		final List<IEvalElement> list = new ArrayList<IEvalElement>();
		for (final String c : code) {
			list.add(new ClassicalB(c));
		}
		return eval(state, list);
	}

	@Override
	public void execute(final ICommand command) {
		animator.execute(command);
	}

	@Override
	public void execute(final ICommand... commands) {
		animator.execute(commands);
	}

	@Override
	public void sendInterrupt() {
		animator.sendInterrupt();
	}

	// NOTIFICATION SYSTEM

	/**
	 * Adds an IStateSpaceChangeListener to the list of StateSpaceListeners.
	 * This listener will be notified whenever a new operation or a new state is
	 * added to the graph.
	 * 
	 * @param l
	 */
	public void registerStateSpaceListener(final IStateSpaceChangeListener l) {
		stateSpaceListeners.add(l);
	}

	private void notifyStateSpaceChange(final String opName,
			final boolean isDestStateNew) {
		for (final IStateSpaceChangeListener listener : stateSpaceListeners) {
			listener.newTransition(opName, isDestStateNew);
		}
	}

	// INFORMATION ABOUT THE STATE
	@Override
	public String toString() {
		String result = "";
		result += super.toString();
		return result;
	}

	public String printInfo() {
		return getInfo().toString();
	}

	// assert !s.getOutEdges(s.getCurrentState()).contains(new OperationId("1"))
	public boolean isOutEdge(final StateId sId, final OpInfo oId) {
		return outgoingEdgesOf(sId).contains(oId);
	}

	public boolean isOutEdge(final String stateId, final String opId) {
		return isOutEdge(getVertex(stateId), ops.get(opId));
	}

	public HashMap<String, StateId> getStates() {
		return states;
	}

	public HashMap<String, OpInfo> getOps() {
		return ops;
	}

	public StateSpaceInfo getInfo() {
		return info;
	}

	public StateId getState(final StateId state) {
		if (!isExplored(state)) {
			explore(state);
		}
		return state;
	}

	public StateId getState(final OpInfo op) {
		final StateId edgeTarget = getEdgeTarget(op);
		if (!isExplored(edgeTarget)) {
			explore(edgeTarget);
		}
		return edgeTarget;
	}

	public String printOps(final StateId state) {
		final StringBuilder sb = new StringBuilder();
		final Collection<OpInfo> opIds = outgoingEdgesOf(state);
		sb.append("Operations: \n");
		for (final OpInfo opId : opIds) {
			sb.append("  " + opId.id + ": " + opId.toString() + "\n");
		}
		return sb.toString();
	}

	public String printState(final StateId state) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Current State Id: " + state + "\n");
		final HashMap<String, String> currentState = getInfo().getState(state);
		if (currentState != null) {
			final Set<Entry<String, String>> entrySet = currentState.entrySet();
			for (final Entry<String, String> entry : entrySet) {
				sb.append("  " + entry.getKey() + " -> " + entry.getValue()
						+ "\n");
			}
		}
		return sb.toString();
	}

	public History getTrace(final int state) {
		final StateId id = states.get(String.valueOf(state));
		final List<OpInfo> path = new DijkstraShortestPath<StateId, OpInfo>(
				this, this.__root, id).getPathEdgeList();
		History h = new History(this);
		for (final OpInfo opInfo : path) {
			h = h.add(opInfo.getId());
		}
		return h;
	}

	public void setAnimator(final IAnimator animator) {
		this.animator = animator;
	}

	public ICommand getLoadcmd() {
		return loadcmd;
	}

	public void setLoadcmd(final ICommand loadcmd) {
		this.loadcmd = loadcmd;
	}

	public void setModel(final IEntity model) {
		this.model = model;
		registerFormulas(model.getChildren());
		if (model instanceof ClassicalBModel) {
			final List<IEntity> machines = model.getChildren();
			for (final IEntity iEntity : machines) {
				if (iEntity instanceof ClassicalBMachine) {
					final ClassicalBMachine machine = (ClassicalBMachine) iEntity;
					subscribeChildren(this, machine.variables);
					subscribeChildren(this, machine.invariants);
				}
			}
		}
		if (model instanceof EventBModel) {
			final List<IEntity> components = model.getChildren();
			for (final IEntity iEntity : components) {
				if (iEntity instanceof EBMachine) {
					final EBMachine machine = (EBMachine) iEntity;
					subscribeChildren(this, machine.variables);
					subscribeChildren(this, machine.invariants);
				}
			}
		}
	}

	public void registerFormulas(final List<IEntity> entities) {
		for (final IEntity iEntity : entities) {
			if (iEntity instanceof IEvalElement) {
				final IEvalElement evalElement = (IEvalElement) iEntity;
				formulaRegistry.put(evalElement, new HashSet<Object>());
			}
			registerFormulas(iEntity.getChildren());
		}
	}

	public IEntity getModel() {
		return model;
	}

	public void subscribeChildren(final Object subscriber, final IEntity entity) {
		for (final IEntity iEntity : entity.getChildren()) {
			if (iEntity instanceof IEvalElement) {
				subscribe(subscriber, (IEvalElement) iEntity);
			}
		}
	}

	public void subscribe(final Object subscriber,
			final IEvalElement formulaOfInterest) {
		if (formulaRegistry.containsKey(formulaOfInterest)) {
			formulaRegistry.get(formulaOfInterest).add(subscriber);
		}
	}

	public void unsubscribe(final Object subscriber,
			final IEvalElement formulaOfInterest) {
		if (formulaRegistry.containsKey(formulaOfInterest)) {
			final Set<Object> subscribers = formulaRegistry
					.get(formulaOfInterest);
			subscribers.remove(subscriber);
		}
	}

	public Object asType(final Class<?> className) {
		if (className.getSimpleName().equals("AbstractModel")) {
			if (model instanceof AbstractModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("EventBModel")) {
			if (model instanceof EventBModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("ClassicalBModel")) {
			if (model instanceof ClassicalBModel) {
				return model;
			}
		}
		if (className.getSimpleName().equals("History")) {
			return new History(this);
		}
		throw new ClassCastException("An element of class " + className
				+ " was not found");
	}
}
