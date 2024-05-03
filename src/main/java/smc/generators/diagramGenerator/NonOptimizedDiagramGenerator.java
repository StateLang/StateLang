package smc.generators.diagramGenerator;

import smc.semanticAnalyzer.SemanticStateMachine;

import java.util.ArrayList;
import java.util.List;

public class NonOptimizedDiagramGenerator {
  private List<DiagramNode.StateNode> stateNodes;
  private List<DiagramNode.TransitionNode> transitionNodes;

	public NonOptimizedDiagramGenerator() {
		stateNodes = new ArrayList<>();
		transitionNodes = new ArrayList<>();
	}

  public DiagramNode generate(SemanticStateMachine sm) {
    prepareFsm(sm);
    return makeFsmNode();
  }

  private void prepareFsm(SemanticStateMachine sm) {
	  addTransition("[*]", "", sm.initialState.name, new ArrayList<>());
	  for (SemanticStateMachine.SemanticState state : sm.states.values()) {
      addState(state);
      addTransitions(sm, state);
    }
  }

  private void addState(SemanticStateMachine.SemanticState state) {
    String parentName = state.superStates.isEmpty()
        ? ""
        : state.superStates.first().name;

    stateNodes.add(new DiagramNode.StateNode(
        state.name,
        parentName,
        state.entryActions,
        state.exitActions
    ));
  }

  private void addTransitions(SemanticStateMachine sm, SemanticStateMachine.SemanticState state) {
    for (SemanticStateMachine.SemanticTransition tr : state.transitions)
      addTransition(state.name, tr.event, tr.nextState.name, tr.actions);
  }

  private void addTransition(String currentState, String event, String nextState, List<String> actions) {
    transitionNodes.add(new DiagramNode.TransitionNode(
        currentState,
        event,
        nextState,
        actions
    ));
  }

  private DiagramNode makeFsmNode() {
    return new DiagramNode.FSMNode(stateNodes, transitionNodes);
  }
}
