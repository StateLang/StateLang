package smc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// This is the final output of the finite state machine parser.
// Code generators will use this format as their input.

public class OptimizedStateMachine {
  public List<String> states = new ArrayList<>();
  public List<String> events = new ArrayList<>();
  public List<String> actions = new ArrayList<>();
  public Header header;
  public List<Transition> transitions = new ArrayList<>();

  public String transitionsToString() {
    StringBuilder result = new StringBuilder();
    for (Transition t : transitions)
      result.append(t);
    return result.toString();
  }

  public String toString() {
    String transitionsString = transitionsToString().replaceAll("\n", "\n  ");
    transitionsString = transitionsString.substring(0, transitionsString.length()-2);
    return String.format("Initial: %s\nFsm: %s\nActions:%s\n{\n  %s}\n",
      header.initial, header.fsm, header.actions, transitionsString);
  }

  public static class Header {
    public String initial;
    public String fsm;
    public String actions;
  }

  public static class Transition {
    public String toString() {
      StringBuilder result = new StringBuilder(String.format("%s {\n", currentState));
      for (SubTransition st : subTransitions)
        result.append(st.toString());
      result.append("}\n");
      return result.toString();
    }

    public String currentState;
    public List<SubTransition> subTransitions = new ArrayList<>();
  }

  public static class SubTransition {
    public String toString() {
      return String.format("  %s %s {%s}\n", event, nextState, actionsToString());
    }

    private String actionsToString() {
      StringBuilder result = new StringBuilder();
      if (actions.size() == 0)
        return result.toString();
      for (String action : actions)
        result.append(action).append(" ");
      return result.substring(0, result.length()-1);
    }

    public String event;
    public String nextState;
    public List<String> actions = new ArrayList<>();
  }
	
	public void build_optimized_tree() {
		StringBuilder tree = new StringBuilder();
		tree.append("<FSM>\n");
		tree.append("├── <header>\n");
		tree.append("│   ├── <fsm> \"").append(header.fsm).append("\"\n");
		tree.append("│   ├── <initial> \"").append(header.initial).append("\"\n");
		tree.append("│   └── <actions> \"").append(header.actions).append("\"\n");
		tree.append("├── \"{\"\n");
		tree.append("├── <transitions>*\n");
		
		for (Transition transition : transitions) {
			tree.append(formatTransitionTree(transition));
		}
		tree.append("└── \"}\"\n");
		
		try (BufferedWriter writer = new BufferedWriter(new
				FileWriter(SMC.SmcCompiler.outputDirectory + "/parse_tree.md"))) {
			writer.write("```\n");
			writer.write(tree.toString());
			writer.write("\n```\n");
		} catch (IOException e) {
			System.err.println("Error writing parse_tree.md file: " + e.getMessage());
		}
	}
	
	private String formatTransitionTree(Transition transition) {
		StringBuilder transitionTree = new StringBuilder();
		
		transitionTree.append("│   ├── <transition>\n");
		transitionTree.append("│   │   ├── <currentState> \"").append(transition.currentState).append("\"\n");
		
		transitionTree.append("│   │   ├── \"{\"\n");
		for (SubTransition subtransition : transition.subTransitions) {
			transitionTree.append("│   │   ├── <subTransition>\n");
			transitionTree.append("│   │   │   ├── <event> \"").append(subtransition.event).append("\"\n");
			transitionTree.append("│   │   │   ├── <nextState> \"").append(subtransition.nextState).append("\"\n");
			transitionTree.append("│   │   │   └── <actions>\n");
			
			transitionTree.append("│   │   │       ├── \"{\n");
			for (String action : subtransition.actions) {
				transitionTree.append("│   │   │       ├── <action> \"").append(action).append("\"\n");
			}
			transitionTree.append("│   │   │       └── \"}\n");
		}
		
		transitionTree.append("│   │   └── \"}\n");
		
		return transitionTree.toString();
	}
}
