package smc.semanticAnalyzer;

import java.util.*;

public class SemanticStateMachine {
  public List<AnalysisError> errors = new ArrayList<>();
  public List<AnalysisWarning> warnings = new ArrayList<>();
  public Map<String, SemanticState> states = new TreeMap<>();
  public Set<String> events = new HashSet<>();
  public Set<String> actions = new HashSet<>();
  public SemanticState initialState;
  public String actionClass;
  public String fsmName;

  public String toString() {
    return String.format(
            """
                    Actions: %s
                    FSM: %s
                    Initial: %s%s""",
      actionClass, fsmName, initialState.name, statesToString());

  }

  public void addError(AnalysisError analysisError) {
    errors.add(analysisError);
  }

  public String statesToString() {
    StringBuilder statesString = new StringBuilder("{");
    for (SemanticState s : states.values())
      statesString.append(s.toString());
    statesString.append("}\n");
    return statesString.toString();
  }

  public static class SemanticState implements Comparable<SemanticState> {
    public String name;
    public List<String> entryActions = new ArrayList<>();
    public List<String> exitActions = new ArrayList<>();
    public boolean abstractState = false;
    public SortedSet<SemanticState> superStates = new TreeSet<>();
    public List<SemanticTransition> transitions = new ArrayList<>();

    public SemanticState(String name) {
      this.name = name;
    }

    public boolean equals(Object obj) {
      if (obj instanceof SemanticState other) {
        return
          Objects.equals(other.name, name) &&
            Objects.equals(other.entryActions, entryActions) &&
            Objects.equals(other.exitActions, exitActions) &&
            Objects.equals(other.superStates, superStates) &&
            Objects.equals(other.transitions, transitions) &&
            other.abstractState == abstractState;
      } else
        return false;
    }

    public String toString() {
      return
        String.format("\n  %s {\n%s  }\n",
          makeStateNameWithAdornments(),
          makeTransitionStrings());
    }

    private String makeTransitionStrings() {
      StringBuilder transitionStrings = new StringBuilder();
      for (SemanticTransition st : transitions)
        transitionStrings.append(makeTransitionString(st));

      return transitionStrings.toString();
    }

    private String makeTransitionString(SemanticTransition st) {
      return String.format("    %s %s {%s}\n", st.event, makeNextStateName(st), makeActions(st));
    }

    private String makeActions(SemanticTransition st) {
      StringBuilder actions = new StringBuilder();
      boolean firstAction = true;
      for (String action : st.actions) {
        actions.append(firstAction ? "" : " ").append(action);
        firstAction = false;
      }
      return actions.toString();
    }

    private String makeNextStateName(SemanticTransition st) {
      return st.nextState == null ? "null" : st.nextState.name;
    }

    private String makeStateNameWithAdornments() {
      StringBuilder stateName = new StringBuilder();
      stateName.append(abstractState ? ("(" + name + ")") : name);
      for (SemanticState superState : superStates)
        stateName.append(" :").append(superState.name);
      for (String entryAction : entryActions)
        stateName.append(" <").append(entryAction);
      for (String exitAction : exitActions)
        stateName.append(" >").append(exitAction);
      return stateName.toString();
    }

    public int compareTo(SemanticState s) {
      return name.compareTo(s.name);
    }
  }

  public static class AnalysisWarning {
    public enum ID {
      INCONSISTENT_ABSTRACTION,
      IMPLICIT_SUPERSTATE,
      REDUNDANT_SUPERSTATE,
      SUPERSTATES_INTERSECTION,
      SUPERSTATE_TO_ITS_SUBSTATES_TRANSITION
    }

    private final ID id;
    private Object extra;

    public AnalysisWarning(ID id) {
      this.id = id;
    }

    public AnalysisWarning(ID id, Object extra) {
      this(id);
      this.extra = extra;
    }

    public String toString() {
      return String.format("Semantic Warning: %s(%s)", id.name(), extra);
    }

    public int hashCode() {
      return Objects.hash(id, extra);
    }

    public boolean equals(Object obj) {
      if (obj instanceof AnalysisWarning other)
        return id == other.id && Objects.equals(extra, other.extra);

      return false;
    }
  }

  public static class AnalysisError {
    public enum ID {
      NO_FSM,
      NO_INITIAL,
      INVALID_HEADER,
      EXTRA_HEADER_IGNORED,
      UNDEFINED_STATE,
      UNDEFINED_SUPER_STATE,
      UNUSED_STATE,
      DUPLICATE_TRANSITION,
      ABSTRACT_STATE_USED_AS_NEXT_STATE,
      STATE_ACTIONS_MULTIPLY_DEFINED,
      CONFLICTING_SUPERSTATES,
    }

    private final ID id;
    private Object extra;

    public AnalysisError(ID id) {
      this.id = id;
    }

    public AnalysisError(ID id, Object extra) {
      this(id);
      this.extra = extra;
    }

    public String toString() {
      return String.format("Semantic Error: %s(%s)", id.name(), extra);
    }

    public int hashCode() {
      return Objects.hash(id, extra);
    }

    public boolean equals(Object obj) {
      if (obj instanceof AnalysisError other)
        return id == other.id && Objects.equals(extra, other.extra);

      return false;
    }
  }

  public static class SemanticTransition {
    public String event;
    public SemanticState nextState;
    public List<String> actions = new ArrayList<>();

    public SemanticTransition(SemanticTransition st) {
      this.event = st.event;
      this.nextState = st.nextState;
      this.actions.addAll(st.actions);
    }

    public SemanticTransition() {
    }
  }
}
