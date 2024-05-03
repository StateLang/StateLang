package smc.generators.diagramGenerator;

import smc.OptimizedStateMachine;

import java.util.ArrayList;
import java.util.List;

public class OptimizedDiagramGenerator {
	private List<DiagramNode.StateNode> stateNodes;
	private List<DiagramNode.TransitionNode> transitionNodes;

	public OptimizedDiagramGenerator() {
		stateNodes = new ArrayList<>();
		transitionNodes = new ArrayList<>();
	}

	public DiagramNode generate(OptimizedStateMachine sm) {
		prepareFsm(sm);
		return makeFsmNode();
	}

	private void prepareFsm(OptimizedStateMachine sm) {
		addTransition("[*]", "", sm.header.initial, new ArrayList<>());
		for (String state : sm.states) {
			addState(state);
			addTransitions(sm, state);
		}
	}

	private void addState(String state) {
		stateNodes.add(new DiagramNode.StateNode(
				state,
				"",
				new ArrayList<>(),
				new ArrayList<>()
		));
	}

	private void addTransitions(OptimizedStateMachine sm, String state) {
		for (OptimizedStateMachine.Transition tr : sm.transitions)
			if (tr.currentState.equals(state)) {
				for (OptimizedStateMachine.SubTransition subTr : tr.subTransitions)
					addTransition(tr.currentState, subTr.event, subTr.nextState, subTr.actions);
			}
	}

	private void addTransition(String currentState, String event, String nextState, List<String> actions) {
		transitionNodes.add(new DiagramNode.TransitionNode(
				currentState,
				event,
				nextState,
				actions
		));
	}

	private DiagramNode.FSMNode makeFsmNode() {
		return new DiagramNode.FSMNode(stateNodes, transitionNodes);
	}
}
