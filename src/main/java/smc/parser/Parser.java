package smc.parser;

/*
<FSM> ::= <header>* <logic>
<header> ::= "Actions:" <name> | "FSM:" <name> | "Initial:" <name>

<logic> ::= "{" <transition>* "}"

<transition> ::= <state-spec> <subtransition>
             |   <state-spec> "{" <subtransition>* "}"

<subtransition>   ::= <event-spec> <next-state> <action-spec>
<action-spec>     ::= <action> | "{" <action>* "}" | "-"
<state-spec>      ::= <state> <state-modifiers>
<state-modifiers> ::= "" | <state-modifier> | <state-modifier> <state-modifiers>
<state-modifier>  ::= ":" <state>
                  |   "<" <action-spec>
                  |   ">" <action-spec>

<next-state> ::= <state> | "-"
<event-spec> :: <event> | "-"
<action> ::= <name>
<state> ::= <name>
<event> ::= <name>
*/

import smc.lexer.TokenCollector;

import java.util.function.Consumer;

import static smc.parser.ParserEvent.*;
import static smc.parser.ParserState.*;

public class Parser implements TokenCollector {
  private ParserState state = HEADER;
  private Builder builder;

  public Parser(Builder builder) {
    this.builder = builder;
  }

  public void openBrace(int line, int pos) {
    handleEvent(OPEN_BRACE, line, pos);
  }

  public void closedBrace(int line, int pos) {
    handleEvent(CLOSED_BRACE, line, pos);
  }

  public void openParen(int line, int pos) {
    handleEvent(OPEN_PAREN, line, pos);
  }

  public void closedParen(int line, int pos) {
    handleEvent(CLOSED_PAREN, line, pos);
  }

  public void openAngle(int line, int pos) {
    handleEvent(OPEN_ANGLE, line, pos);
  }

  public void closedAngle(int line, int pos) {
    handleEvent(CLOSED_ANGLE, line, pos);
  }

  public void dash(int line, int pos) {
    handleEvent(DASH, line, pos);
  }

  public void colon(int line, int pos) {
    handleEvent(COLON, line, pos);
  }

  public void name(String name, int line, int pos) {
    builder.setName(name);
    handleEvent(NAME, line, pos);
  }

  public void error(int line, int pos) {
    builder.syntaxError(line, pos);
  }

  class Transition {
    Transition(ParserState currentState, ParserEvent event,
               ParserState newState, Consumer<Builder> action) {
      this.currentState = currentState;
      this.event = event;
      this.newState = newState;
      this.action = action;
    }

    public ParserState currentState;
    public ParserEvent event;
    public ParserState newState;
    public Consumer<Builder> action;
  }

  Transition[] transitions = new Transition[]{
    new Transition(HEADER, NAME, HEADER_COLON, Builder::newHeaderWithName),
    new Transition(HEADER, OPEN_BRACE, STATE_SPEC, null),
    new Transition(HEADER_COLON, COLON, HEADER_VALUE, null),
    new Transition(HEADER_VALUE, NAME, HEADER, Builder::addHeaderWithValue),
    new Transition(STATE_SPEC, OPEN_PAREN, SUPER_STATE_NAME, null),
    new Transition(STATE_SPEC, NAME, STATE_MODIFIER, Builder::setStateName),
    new Transition(STATE_SPEC, CLOSED_BRACE, END, Builder::done),
    new Transition(SUPER_STATE_NAME, NAME, SUPER_STATE_CLOSE, Builder::setSuperStateName),
    new Transition(SUPER_STATE_CLOSE, CLOSED_PAREN, STATE_MODIFIER, null),
    new Transition(STATE_MODIFIER, OPEN_ANGLE, ENTRY_ACTION, null),
    new Transition(STATE_MODIFIER, CLOSED_ANGLE, EXIT_ACTION, null),
    new Transition(STATE_MODIFIER, COLON, STATE_BASE, null),
    new Transition(STATE_MODIFIER, NAME, SINGLE_EVENT, Builder::setEvent),
    new Transition(STATE_MODIFIER, DASH, SINGLE_EVENT, Builder::setNullEvent),
    new Transition(STATE_MODIFIER, OPEN_BRACE, SUBTRANSITION_GROUP, null),
    new Transition(ENTRY_ACTION, NAME, STATE_MODIFIER, Builder::setEntryAction),
    new Transition(ENTRY_ACTION, OPEN_BRACE, MULTIPLE_ENTRY_ACTIONS, null),
    new Transition(MULTIPLE_ENTRY_ACTIONS, NAME, MULTIPLE_ENTRY_ACTIONS, Builder::setEntryAction),
    new Transition(MULTIPLE_ENTRY_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
    new Transition(EXIT_ACTION, NAME, STATE_MODIFIER, Builder::setExitAction),
    new Transition(EXIT_ACTION, OPEN_BRACE, MULTIPLE_EXIT_ACTIONS, null),
    new Transition(MULTIPLE_EXIT_ACTIONS, NAME, MULTIPLE_EXIT_ACTIONS, Builder::setExitAction),
    new Transition(MULTIPLE_EXIT_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
    new Transition(STATE_BASE, NAME, STATE_MODIFIER, Builder::setStateBase),
    new Transition(SINGLE_EVENT, NAME, SINGLE_NEXT_STATE, Builder::setNextState),
    new Transition(SINGLE_EVENT, DASH, SINGLE_NEXT_STATE, Builder::setNullNextState),
    new Transition(SINGLE_NEXT_STATE, NAME, STATE_SPEC, Builder::transitionWithAction),
    new Transition(SINGLE_NEXT_STATE, DASH, STATE_SPEC, Builder::transitionNullAction),
    new Transition(SINGLE_NEXT_STATE, OPEN_BRACE, SINGLE_ACTION_GROUP, null),
    new Transition(SINGLE_ACTION_GROUP, NAME, SINGLE_ACTION_GROUP_NAME, Builder::addAction),
    new Transition(SINGLE_ACTION_GROUP, CLOSED_BRACE, STATE_SPEC, Builder::transitionNullAction),
    new Transition(SINGLE_ACTION_GROUP_NAME, NAME, SINGLE_ACTION_GROUP_NAME, Builder::addAction),
    new Transition(SINGLE_ACTION_GROUP_NAME, CLOSED_BRACE, STATE_SPEC, Builder::transitionWithActions),
    new Transition(SUBTRANSITION_GROUP, CLOSED_BRACE, STATE_SPEC, null),
    new Transition(SUBTRANSITION_GROUP, NAME, GROUP_EVENT, Builder::setEvent),
    new Transition(SUBTRANSITION_GROUP, DASH, GROUP_EVENT, Builder::setNullEvent),
    new Transition(GROUP_EVENT, NAME, GROUP_NEXT_STATE, Builder::setNextState),
    new Transition(GROUP_EVENT, DASH, GROUP_NEXT_STATE, Builder::setNullNextState),
    new Transition(GROUP_NEXT_STATE, NAME, SUBTRANSITION_GROUP, Builder::transitionWithAction),
    new Transition(GROUP_NEXT_STATE, DASH, SUBTRANSITION_GROUP, Builder::transitionNullAction),
    new Transition(GROUP_NEXT_STATE, OPEN_BRACE, GROUP_ACTION_GROUP, null),
    new Transition(GROUP_ACTION_GROUP, NAME, GROUP_ACTION_GROUP_NAME, Builder::addAction),
    new Transition(GROUP_ACTION_GROUP, CLOSED_BRACE, SUBTRANSITION_GROUP, Builder::transitionNullAction),
    new Transition(GROUP_ACTION_GROUP_NAME, NAME, GROUP_ACTION_GROUP_NAME, Builder::addAction),
    new Transition(GROUP_ACTION_GROUP_NAME, CLOSED_BRACE, SUBTRANSITION_GROUP, Builder::transitionWithActions),
    new Transition(END, ParserEvent.EOF, END, null)
  };

  public void handleEvent(ParserEvent event, int line, int pos) {
    for (Transition t : transitions) {
      if (t.currentState == state && t.event == event) {
        state = t.newState;
        if (t.action != null)
          t.action.accept(builder);
        return;
      }
    }
    handleEventError(event, line, pos);
  }

  private void handleEventError(ParserEvent event, int line, int pos) {
    switch (state) {
      case HEADER, HEADER_COLON, HEADER_VALUE -> builder.headerError(state, event, line, pos);
      case STATE_SPEC, SUPER_STATE_NAME, SUPER_STATE_CLOSE, STATE_MODIFIER, EXIT_ACTION, ENTRY_ACTION, STATE_BASE ->
              builder.stateSpecError(state, event, line, pos);
      case SINGLE_EVENT, SINGLE_NEXT_STATE, SINGLE_ACTION_GROUP, SINGLE_ACTION_GROUP_NAME ->
              builder.transitionError(state, event, line, pos);
      case SUBTRANSITION_GROUP, GROUP_EVENT, GROUP_NEXT_STATE, GROUP_ACTION_GROUP, GROUP_ACTION_GROUP_NAME ->
              builder.transitionGroupError(state, event, line, pos);
      case END -> builder.endError(state, event, line, pos);
    }
  }
}
