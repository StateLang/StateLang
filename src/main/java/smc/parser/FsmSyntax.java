package smc.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FsmSyntax {
  public List<Header> headers = new ArrayList<>();
  public List<Transition> logic = new ArrayList<>();
  public List<SyntaxError> errors = new ArrayList<>();
  public boolean done = false;

  public static class Header {
    public String name;
    public String value;

    public static Header NullHeader() {
      return new Header(null, null);
    }

    public Header() {
    }

    public Header(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public int hashCode() {
      return Objects.hash(name, value);
    }

    public boolean equals(Object obj) {
      if (obj instanceof Header other) {
        return Objects.equals(other.name, name) && Objects.equals(other.value, value);
      }
      return false;
    }

  }

  public static class Transition {
    public StateSpec state;
    public List<SubTransition> subTransitions = new ArrayList<>();
  }

  public static class StateSpec {
    public String name;
    public List<String> superStates = new ArrayList<>();
    public List<String> entryActions = new ArrayList<>();
    public List<String> exitActions = new ArrayList<>();
    public boolean abstractState;
  }

  public static class SubTransition {
    public String event;
    public String nextState;
    public List<String> actions = new ArrayList<>();

    public SubTransition(String event) {
      this.event = event;
    }
  }

  public static class SyntaxError {
    public Type type;
    public String msg;
    public int lineNumber;
    public int position;

    public SyntaxError(Type type, String msg, int lineNumber, int position) {
      this.type = type;
      this.msg = msg;
      this.lineNumber = lineNumber;
      this.position = position;
    }

    public String toString() {
      return String.format("Syntax Error Line: %d, Position: %d.  (%s) %s", lineNumber, position, type.name(), msg);
    }

    public enum Type {HEADER, STATE, TRANSITION, TRANSITION_GROUP, END, SYNTAX}
  }

  public String toString() {
    return
      formatHeaders() +
        formatLogic() +
        (done ? ".\n" : "") +
        formatErrors();
  }

  private String formatHeaders() {
    String formattedHeaders = "";
    for (Header header : headers)
      formattedHeaders += formatHeader(header);
    return formattedHeaders;
  }

  private String formatHeader(Header header) {
    return String.format("%s:%s\n", header.name, header.value);
  }

  private String formatLogic() {
    if (logic.size() > 0)
      return String.format("{\n%s}\n", formatTransitions());
    else
      return "";
  }

  private String formatTransitions() {
    String transitions = "";
    for (Transition transition : logic)
      transitions += formatTransition(transition);
    return transitions;
  }

  private String formatTransition(Transition transition) {
    return
      String.format("  %s %s\n",
        formatStateName(transition.state),
        formatSubTransitions(transition));
  }

  private String formatStateName(StateSpec stateSpec) {
    StringBuilder stateName = new StringBuilder(String.format(stateSpec.abstractState ? "(%s)" : "%s", stateSpec.name));
    for (String superState : stateSpec.superStates)
      stateName.append(":").append(superState);
    for (String entryAction : stateSpec.entryActions)
      stateName.append(" <").append(entryAction);
    for (String exitAction : stateSpec.exitActions)
      stateName.append(" >").append(exitAction);
    return stateName.toString();
  }

  private String formatSubTransitions(Transition transition) {
    if (transition.subTransitions.size() == 1)
      return formatSubTransition(transition.subTransitions.get(0));
    else {
      StringBuilder formattedSubTransitions = new StringBuilder("{\n");

      for (SubTransition subtransition : transition.subTransitions)
        formattedSubTransitions.append("    ").append(formatSubTransition(subtransition)).append("\n");

      formattedSubTransitions.append("  }");

      return formattedSubTransitions.toString();
    }
  }

  private String formatSubTransition(SubTransition subtransition) {
    return String.format(
      "%s %s %s",
      subtransition.event,
      subtransition.nextState,
      formatActions(subtransition));
  }

  private String formatActions(SubTransition subtransition) {
    if (subtransition.actions.size() == 1)
      return subtransition.actions.get(0);
    else {
      StringBuilder actions = new StringBuilder("{");
      boolean first = true;
      for (String action : subtransition.actions) {
        actions.append(first ? "" : " ").append(action);
        first = false;
      }

      actions.append("}");

      return actions.toString();
    }
  }

  private String formatErrors() {
    if (errors.size() > 0)
      return formatError(errors.get(0));
    else
      return "";
  }

  private String formatError(SyntaxError error) {
    return String.format(
      "Syntax error: %s. %s. line %d, position %d.\n",
      error.type.toString(),
      error.msg,
      error.lineNumber,
      error.position);
  }

  public String getError() {
    return formatErrors();
  }

}
