package smc.semanticAnalyzer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import smc.lexer.Lexer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;

import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static smc.parser.FsmSyntax.Header;
import static smc.parser.ParserEvent.EOF;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError.ID.*;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisWarning.ID.*;

@RunWith(HierarchicalContextRunner.class)
public class SemanticAnalyzerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
  }

  private SemanticStateMachine produceAst(String s) {
    lexer.lex(s);
    parser.handleEvent(EOF, -1, -1);
    return analyzer.analyze(builder.getFsm());
  }

  private void assertSemanticResult(String s, String expected) {
    SemanticStateMachine semanticStateMachine = produceAst(s);
    assertEquals(expected, semanticStateMachine.toString());
  }

  public class SemanticErrors {
    public class HeaderErrors {
      @Test
      public void noHeaders() throws Exception {
        List<AnalysisError> errors = produceAst("{}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void missingActions() throws Exception {
        List<AnalysisError> errors = produceAst("FSM:f Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL))));
      }

      @Test
      public void missingFsm() throws Exception {
        List<AnalysisError> errors = produceAst("actions:a Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL))));
        assertThat(errors, hasItems(new AnalysisError(NO_FSM)));
      }

      @Test
      public void missingInitial() throws Exception {
        List<AnalysisError> errors = produceAst("Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(NO_FSM))));
        assertThat(errors, hasItems(new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void nothingMissing() throws Exception {
        List<AnalysisError> errors = produceAst("Initial: f Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL),
          new AnalysisError(NO_FSM))));
      }

      @Test
      public void unexpectedHeader() throws Exception {
        List<AnalysisError> errors = produceAst("X: x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(INVALID_HEADER, new Header("X", "x"))));
      }

      @Test
      public void duplicateHeader() throws Exception {
        List<AnalysisError> errors = produceAst("fsm:f fsm:x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(EXTRA_HEADER_IGNORED, new Header("fsm", "x"))));
      }

      @Test
      public void initialStateMustBeDefined() throws Exception {
        List<AnalysisError> errors = produceAst("initial: i {s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(UNDEFINED_STATE, "initial: i")));
      }
    } // Header Errors

    public class StateErrors {
      @Test
      public void nullNextStateIsNotUndefined() throws Exception {
        List<AnalysisError> errors = produceAst("{s - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, null))));
      }

      @Test
      public void undefinedState() throws Exception {
        List<AnalysisError> errors = produceAst("{s - s2 -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_STATE, "s2")));
      }

      @Test
      public void noUndefinedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s - s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, "s2"))));
      }

      @Test
      public void undefinedSuperState() throws Exception {
        List<AnalysisError> errors = produceAst("{s:ss - - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "ss")));
      }

      @Test
      public void superStateDefined() throws Exception {
        List<AnalysisError> errors = produceAst("{ss - - - s:ss - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "s2"))));
      }

      @Test
      public void unusedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s e n -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNUSED_STATE, "s")));
      }

      @Test
      public void noUnusedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void nextStateNullIsImplicitUse() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void usedAsBaseIsValidUsage() throws Exception {
        List<AnalysisError> errors = produceAst("{b e n - s:b e2 s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void usedAsInitialIsValidUsage() throws Exception {
        List<AnalysisError> errors = produceAst("initial: b {b e n -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void errorIfSuperStatesHaveConflictingTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 -" +
            "  (ss2) e1 s2 -" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s2 e s -" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1")));
      }

      @Test
      public void noErrorForOverriddenTransition() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 -" +
            "  s :ss1 e1 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1"))));
      }

      @Test
      public void noErrorIfSuperStatesHaveIdenticalTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 ax" +
            "  (ss2) e1 s1 ax" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1"))));
      }

      @Test
      public void errorIfSuperstatesHaveDifferentActionsInSameTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 a1" +
            "  (ss2) e1 s1 a2" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1")));

      }
    } // State Errors

    public class TransitionErrors {
      @Test
      public void duplicateTransitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - - s e - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)")));
      }

      @Test
      public void noDuplicateTransitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)"))));
      }

      @Test
      public void abstractStatesCantBeTargets() throws Exception {
        List<AnalysisError> errors = produceAst("{(as) e - - s e as -}").errors;
        assertThat(errors, hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->as")));
      }

      @Test
      public void abstractStatesCanBeUsedAsSuperStates() throws Exception {
        List<AnalysisError> errors = produceAst("{(as) e - - s:as e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->s"))));
      }

      @Test
      public void entryAndExitActionsNotMultiplyDefined() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "{" +
            "  s - - - " +
            "  s - - -" +
            "  es - - -" +
            "  es <x - - - " +
            "  es <x - - -" +
            "  xs >x - - -" +
            "  xs >{x} - - -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "s"))));
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "es"))));
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "xs"))));
      }

      @Test
      public void errorIfStateHasMultipleEntryActionDefinitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s - - - ds <x - - - ds <y - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "s"))));
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultipleExitActionDefinitions() throws Exception {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds >y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultiplyDefinedEntryAndExitActions() throws Exception {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds <y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }
    } // Transition Errors
  }// Semantic Errors.

  public class Warnings {
    @Test
    public void warnIfStateUsedAsBothAbstractAndConcrete() throws Exception {
      List<SemanticStateMachine.AnalysisWarning> warnings = produceAst("{(ias) e - - ias e - - (cas) e - -}").warnings;
      assertThat(warnings, not(hasItems(new SemanticStateMachine.AnalysisWarning(INCONSISTENT_ABSTRACTION, "cas"))));
      assertThat(warnings, hasItems(new SemanticStateMachine.AnalysisWarning(INCONSISTENT_ABSTRACTION, "ias")));
    }

    @Test
    public void warnIfImplicitSuperState() throws Exception {
      List<SemanticStateMachine.AnalysisWarning> warnings = produceAst("" +
              "FSM: f Actions: act Initial: i " +
              "{" +
              "  (s) >n <t e0 - - " +
              "  (m) >u e1 - - " +
              "  a:m:s e3 d - " +
              "  d:m:s e2 a - " +
              "  i:s e4 d -" +
              "}"
      ).warnings;
      assertThat(warnings, hasItems(new SemanticStateMachine.AnalysisWarning(IMPLICIT_SUPERSTATE, "m:s")));
    }

    @Test
    public void warnIfRedundantSuperState() throws Exception {
      List<SemanticStateMachine.AnalysisWarning> warnings = produceAst("" +
              "FSM: f Actions: act Initial: i " +
              "{" +
              "  (s) >n <t e0 - - " +
              "  (m):s >u e1 - - " +
              "  a:m:s e3 d - " +
              "  d:m:s e2 a - " +
              "  i:s e4 d -" +
              "}"
      ).warnings;
      assertThat(warnings, hasItems(new SemanticStateMachine.AnalysisWarning(REDUNDANT_SUPERSTATE, "a,d:s")));
    }
  } // Warnings

  public class Lists {
    @Test
    public void oneState() throws Exception {
      SemanticStateMachine ast = produceAst("{s - - -}");
      assertThat(ast.states.values(), contains(new SemanticStateMachine.SemanticState("s")));
    }

    @Test
    public void manyStates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.values(), hasItems(
        new SemanticStateMachine.SemanticState("s1"),
        new SemanticStateMachine.SemanticState("s2"),
        new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void statesAreKeyedByName() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.get("s1"), equalTo(new SemanticStateMachine.SemanticState("s1")));
      assertThat(ast.states.get("s2"), equalTo(new SemanticStateMachine.SemanticState("s2")));
      assertThat(ast.states.get("s3"), equalTo(new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void manyEvents() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e3 - -}");
      assertThat(ast.events, hasItems("e1", "e2", "e3"));
      assertThat(ast.events, hasSize(3));
    }

    @Test
    public void manyEventsButNoDuplicates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e1 - -}");
      assertThat(ast.events, hasItems("e1", "e2"));
      assertThat(ast.events, hasSize(2));
    }

    @Test
    public void noNullEvents() throws Exception {
      SemanticStateMachine ast = produceAst("{(s1) - - -}");
      assertThat(ast.events, hasSize(0));
    }

    @Test
    public void manyActionsButNoDuplicates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - {a1 a2} s2 e2 - {a3 a1}}");
      assertThat(ast.actions, hasItems("a1", "a2", "a3"));
      assertThat(ast.actions, hasSize(3));
    }

    @Test
    public void entryAndExitActionsAreCountedAsActions() throws Exception {
      SemanticStateMachine ast = produceAst("{s <ea >xa - - a}");
      assertThat(ast.actions, hasItems("ea", "xa"));
    }
  } // Lists

  public class Logic {
    private String addHeader(String s) {
      return "initial: s fsm:f actions:a " + s;
    }

    private void assertSyntaxToAst(String syntax, String ast) {
      String states = produceAst(addHeader(syntax)).statesToString();
      assertThat(states, equalTo(ast));
    }

    @Test
    public void oneTransition() throws Exception {
      assertSyntaxToAst("{s e s a}",
              """
                      {
                        s {
                          e s {a}
                        }
                      }
                      """);
    }

    @Test
    public void twoTransitionsAreAggregated() throws Exception {
      assertSyntaxToAst("{s e1 s a s e2 s a}",
              """
                      {
                        s {
                          e1 s {a}
                          e2 s {a}
                        }
                      }
                      """);
    }

    @Test
    public void superStatesAreAggregated() throws Exception {
      assertSyntaxToAst("" +
                      "{" +
                      "s:b1 e1 s a " +
                      "s:b2 e2 s a " +
                      "(b1) e s - " +
                      "(b2) e s -" +
                      "}",
              """
                      {
                        (b1) {
                          e s {}
                        }

                        (b2) :b1 {
                          e s {}
                        }

                        s :b2 {
                          e1 s {a}
                          e2 s {a}
                        }
                      }
                      """);
    }

    @Test
    public void nullNextStateRefersToSelf() throws Exception {
      assertSyntaxToAst("{s e - a}",
              """
                      {
                        s {
                          e s {a}
                        }
                      }
                      """
      );
    }

    @Test
    public void actionsRemainInOrder() throws Exception {
      assertSyntaxToAst("{s e s {the quick brown fox jumped over the lazy dogs back}}",
              """
                      {
                        s {
                          e s {the quick brown fox jumped over the lazy dogs back}
                        }
                      }
                      """);
    }

    @Test
    public void entryAndExitActionsRemainInOrder() throws Exception {
      assertSyntaxToAst("{s <{d o} <g >{c a} >t e s a}",
              """
                      {
                        s <d <o <g >c >a >t {
                          e s {a}
                        }
                      }
                      """);
    }
  } //Logic

  public class AcceptanceTests {
    @Test
    public void subwayTurnstileOne() throws Exception {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: OneCoinTurnstile
                      Initial: Locked
                      {
                        Locked\tCoin\tUnlocked\t{alarmOff unlock}
                        Locked \tPass\tLocked\t\talarmOn
                        Unlocked\tCoin\tUnlocked\tthankyou
                        Unlocked\tPass\tLocked\t\tlock
                      }""");
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: OneCoinTurnstile
                      Initial: Locked{
                        Unlocked {
                          Coin Unlocked {thankyou}
                          Pass Locked {lock}
                        }
                        
                        Locked {
                          Coin Unlocked {alarmOff unlock}
                          Pass Locked {alarmOn}
                        }
                      }
                      """));
    }

    @Test
    public void subwayTurnstileTwo() throws Exception {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked
                      {
                      \tLocked {
                      \t\tPass\tAlarming\talarmOn
                      \t\tCoin\tFirstCoin\t-
                      \t\tReset\tLocked\t{lock alarmOff}
                      \t}
                      \t
                      \tAlarming\tReset\tLocked {lock alarmOff}
                      \t
                      \tFirstCoin {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tUnlocked\tunlock
                      \t\tReset\tLocked {lock alarmOff}
                      \t}
                      \t
                      \tUnlocked {
                      \t\tPass\tLocked\tlock
                      \t\tCoin\t-\t\tthankyou
                      \t\tReset\tLocked {lock alarmOff}
                      \t}
                      }"""
      );
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked{
                        Alarming {
                          Reset Locked {lock alarmOff}
                        }

                        FirstCoin {
                          Pass Alarming {}
                          Coin Unlocked {unlock}
                          Reset Locked {lock alarmOff}
                        }

                        Unlocked {
                          Pass Locked {lock}
                          Coin Unlocked {thankyou}
                          Reset Locked {lock alarmOff}
                        }
                        
                        Locked {
                          Pass Alarming {alarmOn}
                          Coin FirstCoin {}
                          Reset Locked {lock alarmOff}
                        }
                      }
                      """));
    }

    @Test
    public void subwayTurnstileThree() throws Exception {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked
                      {
                          (Base)\tReset\tLocked\tlock

                      \tLocked : Base {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tFirstCoin\t-
                      \t}
                      \t
                      \tAlarming : Base\t<alarmOn >alarmOff -\t-\t-
                      \t
                      \tFirstCoin : Base {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tUnlocked\tunlock
                      \t}
                      \t
                      \tUnlocked : Base {
                      \t\tPass\tLocked\tlock
                      \t\tCoin\t-\t\tthankyou
                      \t}
                      }"""
      );
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked{
                        (Base) {
                          Reset Locked {lock}
                        }
                                            
                        Unlocked :Base {
                          Pass Locked {lock}
                          Coin Unlocked {thankyou}
                        }
                                            
                        Locked :Base {
                          Pass Alarming {}
                          Coin FirstCoin {}
                        }
                                            
                        FirstCoin :Base {
                          Pass Alarming {}
                          Coin Unlocked {unlock}
                        }
                                            
                        Alarming :Base <alarmOn >alarmOff {
                          null Alarming {}
                        }
                      }
                      """));
    }

    @Test
    public void complexInheritance() throws Exception {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Complex
                      FSM: Complex
                      Initial: n{
                        j <jn >jx {
                        }
                        c <cn >cx {
                          ec l a1
                        }
                        d {
                        }
                        f {
                        }
                        y <yn >yx {
                          ey f ay
                        }
                        u <un >ux{
                          eu d au
                        }
                        v {
                          ev b av
                        }
                        s <sn >sx {
                          es z as2
                        }
                        s3 :s1 :x :j {
                          es3 q as3
                        }
                        s2 : s1 :y {
                          es2 s3 as2
                        }
                        s1 :x :c :j <n1 >x1 {
                        }
                        x :j :c <xn >xx {
                        }
                        q :x <qn >qx{
                          eq l aq
                        }
                        a :c :m {
                          ea b aa
                        }
                        n :m :c {
                          en a an
                        }
                        m :c <mn >mx {
                          em f {am1 am2}
                        }
                        b: c {
                          eb r {ab1 ab2}
                        }
                        r :s :c {
                          er i ar
                        }
                        i :c :s {
                          ei g ai
                          ei2 s2 ai2
                        }
                        g :c :y {
                          eg w ag
                        }
                        w :y {
                          ew d aw
                          ew2 k aw2
                        }
                        k :y :u :v {
                          ek s2 ak
                        }
                        l :v {
                          el q al
                        }
                        z :v {
                          ez l a2
                        }
                      }
                 """
      );
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Complex
                      FSM: Complex
                      Initial: n{
                        s2 {
                          es2 s3 {yx as2}
                          ey f {x1 xx jx cx yx ay}
                          ec l {x1 xx jx cx yx a1}
                        }
                      
                        k {
                          ek s2 {ux ak}
                          ev b {yx ux av}
                          eu d {yx ux au}
                          ey f {yx ux ay}
                        }
                      
                        g {
                          eg w {cx ag}
                          ey f {cx yx ay}
                          ec l {cx yx a1}
                        }
                      
                        c <cn >cx {
                          ec l {a1}
                        }
                      
                        s :c <sn >sx {
                          es z {as2}
                        }
                      
                        r :s {
                          er i {ar}
                        }
                      
                        i :s {
                          ei g {yn ai}
                          ei2 s2 {yn jn xn n1 ai2}
                        }
                      
                        m :c <mn >mx {
                          em f {am1 am2}
                        }
                      
                        n :m {
                          en a {an}
                        }
                      
                        a :m {
                          ea b {aa}
                        }
                      
                        j :c <jn >jx {
                        }
                       
                        x :j <xn >xx {
                        }
                       
                        s1 :x <n1 >x1 {
                        }
                       
                        s3 :s1 {
                          es3 q {as3}
                        }
                       
                        q :x <qn >qx {
                          eq l {aq}
                        }
                      
                        b :c {
                          eb r {ab1 ab2}
                        }
                      
                        y <yn >yx {
                          ey f {ay}
                        }
                      
                        w :y {
                          ew d {aw}
                          ew2 k {un aw2}
                        }
                      
                        v {
                          ev b {av}
                        }
                       
                        z :v {
                          ez l {a2}
                        }
                       
                        l :v {
                          el q {al}
                        }
                       
                        f {
                        }
                       
                        d {
                        }
                      }
                      """));
    }
  }
}
