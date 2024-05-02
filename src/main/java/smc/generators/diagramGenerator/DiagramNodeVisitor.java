package smc.generators.diagramGenerator;

public interface DiagramNodeVisitor {
  void visit(DiagramNode.StateNode stateNode);
  void visit(DiagramNode.TransitionNode transitionNode);
  void visit(DiagramNode.FSMNode fsmNode);
}
