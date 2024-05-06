package smc.implementers;

import smc.generators.diagramGenerator.DiagramNode;
import smc.generators.diagramGenerator.DiagramNodeVisitor;

import java.util.Map;
import java.util.StringJoiner;

public class PlantUmlDiagramImplementer implements DiagramNodeVisitor {
	private final StringBuilder output = new StringBuilder();

	public PlantUmlDiagramImplementer(Map<String, String> flags) {
		}

	public String getOutput() {
			return output.toString();
		}

	@Override
		public void visit(DiagramNode.FSMNode fsmNode) {
			output.append("@startuml\nhide empty description\n");
			fsmNode.generateStates(this);
			output.append("[*] --> ").append(fsmNode.initialState).append("\n");
			fsmNode.generateTransitions(this);
			output.append("\n@enduml");
		}

	@Override
		public void visit(DiagramNode.StateNode stateNode) {
			if (!stateNode.parentName.isEmpty()) {
				output.append("state ")
					.append(stateNode.parentName)
					.append(" {\n\t")
					.append("state ")
					.append(stateNode.name);
				if (!stateNode.entryActions.isEmpty() && !stateNode.exitActions.isEmpty()) {
					output.append(": ");
					appendEntryAndExitActions(stateNode);
				}
				output.append("\n}\n\n");
			}
			else if (!stateNode.entryActions.isEmpty() && !stateNode.exitActions.isEmpty()){
				output.append("state ").append(stateNode.name).append(": ");
				appendEntryAndExitActions(stateNode);
				output.append("\n\n");
			}
		}

	private void appendEntryAndExitActions(DiagramNode.StateNode stateNode) {
			if (!stateNode.entryActions.isEmpty()) {
				output.append("entry / ");
				appendActions(stateNode.entryActions);
			}
			
			if (!stateNode.exitActions.isEmpty()) {
				output.append("\\nexit / ");
				appendActions(stateNode.exitActions);
			}
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

	private void appendActions(Iterable<String> actions) {
			StringJoiner joiner = new StringJoiner(", ");
			actions.forEach(action -> joiner.add(action + "()"));
			output.append(joiner);
		}
}
