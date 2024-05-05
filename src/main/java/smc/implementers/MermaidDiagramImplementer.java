package smc.implementers;

import smc.generators.diagramGenerator.DiagramNode;
import smc.generators.diagramGenerator.DiagramNodeVisitor;

import java.util.Map;
import java.util.StringJoiner;

public class MermaidDiagramImplementer implements DiagramNodeVisitor {
	private final StringBuilder output = new StringBuilder();

	public MermaidDiagramImplementer(Map<String, String> flags) {
	}

	public String getOutput() {
		return output.toString();
	}

	@Override
	public void visit(DiagramNode.FSMNode fsmNode) {
		output.append("stateDiagram-v2\n");
		fsmNode.generateStates(this);
		output.append("[*] --> ").append(fsmNode.initialState).append("\n");
		fsmNode.generateTransitions(this);
	}

	@Override
	public void visit(DiagramNode.StateNode stateNode) {
		if (!stateNode.parentName.isEmpty()) {
			output.append("state ")
					.append(stateNode.parentName)
					.append(" {\n\t")
					.append(stateNode.name).append(": ").append(stateNode.name);
			appendEntryAndExitActions(stateNode);
			output.append("\n}\n\n");
		}
		else if (!stateNode.entryActions.isEmpty() && !stateNode.exitActions.isEmpty()){
			output.append(stateNode.name).append(": ").append(stateNode.name);
			appendEntryAndExitActions(stateNode);
			output.append("\n\n");
		}
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

	private void appendActions(Iterable<String> actions) {
		StringJoiner joiner = new StringJoiner(", ");
		actions.forEach(action -> joiner.add(action + "()"));
		output.append(joiner);
	}

	@Override
	public void visit(DiagramNode.TransitionNode transitionNode) {
		output.append(transitionNode.currentState).append(" --> ").append(transitionNode.nextState);

		if (transitionNode.event != null && !transitionNode.event.isEmpty()) {
			output.append(" : ").append(transitionNode.event);
			if (transitionNode.actions != null && !transitionNode.actions.isEmpty()) {
				output.append(" / ");
				appendActions(transitionNode.actions);
			}
		}

		output.append("\n");
	}
}
