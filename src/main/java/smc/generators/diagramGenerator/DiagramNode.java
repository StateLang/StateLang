package smc.generators.diagramGenerator;

import java.util.List;

public interface DiagramNode {
  void accept(DiagramNodeVisitor visitor);

  class FSMNode implements DiagramNode {
    public List<StateNode> stateNodes;
    public List<TransitionNode> transitionNodes;

    public FSMNode(List<StateNode> stateNodes, List<TransitionNode> transitionNodes) {
      this.stateNodes = stateNodes;
      this.transitionNodes = transitionNodes;
    }

    @Override
    public void accept(DiagramNodeVisitor visitor) {
      visitor.visit(this);
    }

    public void generateStates(DiagramNodeVisitor visitor) {
      for (StateNode stateNode : stateNodes)
        stateNode.accept(visitor);
    }

    public void generateTransitions(DiagramNodeVisitor visitor) {
      for (TransitionNode transitionNode : transitionNodes)
        transitionNode.accept(visitor);
    }
  }

  class StateNode implements DiagramNode {
    public String name;
    public String parentName;
    public List<String> entryActions;
    public List<String> exitActions;

    public StateNode(String name) { this.name = name; }

    public StateNode(String name, String parentName, List<String> entryActions, List<String> exitActions) {
      this.name = name;
      this.parentName = parentName;
      this.entryActions = entryActions;
      this.exitActions = exitActions;
    }

    @Override
    public void accept(DiagramNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  class TransitionNode implements DiagramNode {
    public String currentState;
    public String event;
    public String nextState;
    public List<String> actions;

    public TransitionNode(String currentState, String event, String nextState, List<String> actions) {
      this.currentState = currentState;
      this.event = event;
      this.nextState = nextState;
      this.actions = actions;
    }

    @Override
    public void accept(DiagramNodeVisitor visitor) {
      visitor.visit(this);
    }
  }
}
