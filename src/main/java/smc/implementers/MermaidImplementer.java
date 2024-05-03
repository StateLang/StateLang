package smc.implementers;

import smc.generators.diagramGenerator.DiagramNode;
import smc.generators.diagramGenerator.DiagramNodeVisitor;

import java.util.List;
import java.util.StringJoiner;

public class MermaidImplementer implements DiagramNodeVisitor {
	private StringBuilder output = new StringBuilder();

	@Override
	public void visit(DiagramNode.StateNode stateNode) {
		appendStateNode(stateNode);
	}

	@Override
	public void visit(DiagramNode.TransitionNode transitionNode) {
		appendTransitionNode(transitionNode);
	}

	@Override
	public void visit(DiagramNode.FSMNode fsmNode) {
		output.append("stateDiagram-v2\n");
		appendStates(fsmNode);
		fsmNode.transitionNodes.forEach(this::appendTransitionNode);
	}

	private void appendStates(DiagramNode.FSMNode fsmNode){
		DiagramNode.StateNode firstNode = fsmNode.stateNodes.get(0);
		if (!firstNode.parentName.isEmpty()) {
			output.append("state ").append(firstNode.parentName).append(" {\n");
			appendChildStates(firstNode.parentName, fsmNode.stateNodes);
			output.append("}\n");
		}
		else
			fsmNode.stateNodes.forEach(this::appendStateNode);

		output.append("\n");
	}

	private void appendChildStates(String parentName, List<DiagramNode.StateNode> stateNodes) {
		for (DiagramNode.StateNode stateNode: stateNodes)
			if (stateNode.parentName.equals(parentName)){
				output.append("\t");
				appendStateNode(stateNode);
			}
	}

	private void appendStateNode(DiagramNode.StateNode stateNode) {
		output.append("state ").append(stateNode.name).append(": ").append(stateNode.name);
		appendEntryAndExitActions(stateNode);
		output.append("\n");
	}

	private void appendEntryAndExitActions(DiagramNode.StateNode stateNode) {
		if (!stateNode.entryActions.isEmpty()) {
			output.append("\\nentry / ");
			appendActions(stateNode.entryActions);
		}

		if (!stateNode.exitActions.isEmpty()) {
			output.append("\\nexit / ");
			appendActions(stateNode.exitActions);
		}
	}

	private void appendTransitionNode(DiagramNode.TransitionNode transitionNode) {
		output.append(transitionNode.currentState).append(" --> ").append(transitionNode.nextState);
		
		if (transitionNode.event != null && !transitionNode.event.isEmpty()) {
			output.append(" : ").append(transitionNode.event);
		}
		
		if (transitionNode.actions != null && !transitionNode.actions.isEmpty()) {
			output.append(" / ");
			appendActions(transitionNode.actions);
		}

		output.append("\n");
	}

	private void appendActions(Iterable<String> actions) {
		StringJoiner joiner = new StringJoiner(", ");
		actions.forEach(action -> joiner.add(action + "()"));
		output.append(joiner);
	}

	public String getOutput() {
		return output.toString();
	}
}
