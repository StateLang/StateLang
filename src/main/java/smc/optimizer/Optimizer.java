package smc.optimizer;

import smc.OptimizedStateMachine;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.util.*;

import static smc.OptimizedStateMachine.*;
import static smc.semanticAnalyzer.SemanticStateMachine.SemanticState;
import static smc.semanticAnalyzer.SemanticStateMachine.SemanticTransition;

public class Optimizer {
  private OptimizedStateMachine optimizedStateMachine;
  private SemanticStateMachine semanticStateMachine;

  public OptimizedStateMachine optimize(SemanticStateMachine ast) {
    this.semanticStateMachine = ast;
    optimizedStateMachine = new OptimizedStateMachine();
    addHeader(ast);
    addLists();
    addTransitions();
    return optimizedStateMachine;
  }

  private void addTransitions() {
    for (SemanticState s : semanticStateMachine.states.values())
      if (!s.abstractState)
        new StateOptimizer(s).addTransitionsForState();
  }

  private class StateOptimizer {
    private final SemanticState currentState;
    private final Set<String> eventsForThisState = new HashSet<>();

    public StateOptimizer(SemanticState currentState) {
      this.currentState = currentState;
    }

    private void addTransitionsForState() {
      Transition transition = new Transition();
      transition.currentState = currentState.name;
      addSubTransitions(transition);
      optimizedStateMachine.transitions.add(transition);
    }

    private void addSubTransitions(Transition transition) {
      for (SemanticState stateInHierarchy : makeRootFirstHierarchyOfStates())
        addStateTransitions(transition, stateInHierarchy);
    }

    private List<SemanticState> makeRootFirstHierarchyOfStates() {
      LinkedHashSet<SemanticState> hierarchy = new LinkedHashSet<>();
      addAllStatesInHierarchyLeafFirst(currentState, hierarchy);
      List<SemanticState> superStates = new ArrayList<>(hierarchy);
      Collections.reverse(superStates);
      return superStates;
    }

    private void addStateTransitions(Transition transition, SemanticState state) {
      for (SemanticTransition semanticTransition : state.transitions) {
        if (eventExistsAndHasNotBeenOverridden(semanticTransition.event))
          addSubTransition(semanticTransition, transition);
      }
    }

    private boolean eventExistsAndHasNotBeenOverridden(String event) {
      return event != null && !eventsForThisState.contains(event);
    }

    private void addSubTransition(SemanticTransition semanticTransition, Transition transition) {
      eventsForThisState.add(semanticTransition.event);
      SubTransition subTransition = new SubTransition();
      new SubTransitionOptimizer(semanticTransition, subTransition).optimize();
      transition.subTransitions.add(subTransition);
    }

    private class SubTransitionOptimizer {
      private final SemanticTransition semanticTransition;
      private final SubTransition subTransition;

      public SubTransitionOptimizer(SemanticTransition semanticTransition, SubTransition subTransition) {
        this.semanticTransition = semanticTransition;
        this.subTransition = subTransition;
      }

      public void optimize() {
        subTransition.event = semanticTransition.event;
        subTransition.nextState = semanticTransition.nextState.name;
        addExitActions(currentState);
        addEntryActions(semanticTransition.nextState);
        subTransition.actions.addAll(semanticTransition.actions);
      }

      private void addEntryActions(SemanticState entryState) {
        LinkedHashSet<SemanticState> hierarchy = new LinkedHashSet<>();
        addAllStatesInHierarchyLeafFirst(entryState, hierarchy);
        for (SemanticState superState : hierarchy)
          if (!isSuperStateOf(superState, currentState))
            subTransition.actions.addAll(superState.entryActions);
      }

      private void addExitActions(SemanticState exitState) {
        LinkedHashSet<SemanticState> hierarchy = new LinkedHashSet<>();
        addAllStatesInHierarchyLeafFirst(exitState, hierarchy);
        List<SemanticState> superStates = new ArrayList<>(hierarchy);
        Collections.reverse(superStates);
        for (SemanticState superState : superStates)
            if (!isSuperStateOf(superState, semanticTransition.nextState))
              subTransition.actions.addAll(superState.exitActions);
      }
    } // SubTransitionOptimizer
  } // StateOptimizer

  private boolean isSuperStateOf(SemanticState superState, SemanticState state) {
    LinkedHashSet<SemanticState> hierarchy = new LinkedHashSet<>();
    addAllStatesInHierarchyLeafFirst(state, hierarchy);
    return hierarchy.contains(superState);
  }
  
  private void addAllStatesInHierarchyLeafFirst(SemanticState state, LinkedHashSet<SemanticState> hierarchy) {
    for (SemanticState superState : state.superStates)
        addAllStatesInHierarchyLeafFirst(superState, hierarchy);
    hierarchy.add(state);
  }

  private void addHeader(SemanticStateMachine ast) {
    optimizedStateMachine.header = new Header();
    optimizedStateMachine.header.fsm = ast.fsmName;
    optimizedStateMachine.header.initial = ast.initialState.name;
    optimizedStateMachine.header.actions = ast.actionClass;
  }

  private void addLists() {
    addStates();
    addEvents();
    addActions();
  }

  private void addStates() {
    for (SemanticState s : semanticStateMachine.states.values())
      if (!s.abstractState)
        optimizedStateMachine.states.add(s.name);
  }

  private void addEvents() {
    optimizedStateMachine.events.addAll(semanticStateMachine.events);
  }

  private void addActions() {
    optimizedStateMachine.actions.addAll(semanticStateMachine.actions);
  }
}
