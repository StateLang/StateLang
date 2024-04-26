package smc.implementers;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticAnalyzer;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

@RunWith(HierarchicalContextRunner.class)
public class CppNestedSwitchCaseImplementerTests {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;
  private CppNestedSwitchCaseImplementer implementer;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
    generator = new NSCGenerator();
  }

  private OptimizedStateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    SemanticStateMachine ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  private void assertWhitespaceEquivalent(String generatedCode, String expected) {
    assertThat(compressWhiteSpace(generatedCode), equalTo(compressWhiteSpace(expected)));
  }

  public class TestsWithNoFlags {

    @Before
    public void setup() {
      implementer = new CppNestedSwitchCaseImplementer(new HashMap<>());
    }

    @Test
    public void noActions_shouldBeError() throws Exception {
      OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "{" +
        "  I E I A" +
        "}");
      NSCNode generatedFsm = generator.generate(sm);
      generatedFsm.accept(implementer);
      assertThat(implementer.getErrors().size(), is(1));
      assertThat(implementer.getErrors().get(0), is(CppNestedSwitchCaseImplementer.Error.NO_ACTIONS));
    }
    @Test
    public void oneTransition() throws Exception {
      OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "Actions: acts\n" +
        "{" +
        "  I E I A" +
        "}");
      NSCNode generatedFsm = generator.generate(sm);
      generatedFsm.accept(implementer);

      assertWhitespaceEquivalent(implementer.getOutput(), """
              #ifndef FSM_H
              #define FSM_H
              #include "acts.h"
              class fsm : public acts {
              public:
                fsm()
                : state(State_I)
                {}
                void E() {processEvent(Event_E, "E");}
              private:
                enum State {State_I};
                State state;
                void setState(State s) {state=s;}
                enum Event {Event_E};
                void processEvent(Event event, const char* eventName) {
                  switch (state) {
                    case State_I:
                      switch (event) {
                        case Event_E:
                          setState(State_I);
                          A();
                          break;
                        default:
                          unexpected_transition("I", eventName);
                          break;
                      }
                      break;
                  }
                }
              };
              #endif
              """);
    }
  } // no flags
}
