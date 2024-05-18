package smc;

import com.cleancoder.args.Args;
import com.cleancoder.args.ArgsException;
import smc.generators.CodeGenerator;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.FsmSyntax;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticAnalyzer;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static smc.parser.ParserEvent.EOF;

public class SMC {
  public static void main(String[] args) throws Exception {
    String argSchema = "o*,l*,f&";

    try {
      Args argParser = new Args(argSchema, args);
      new SmcCompiler(args, argParser).run();
    } catch (ArgsException e) {
      System.out.println("usage: " + argSchema + " file");
      System.out.println(e.errorMessage());
      System.exit(0);
    }
  }

  public static class SmcCompiler {
    private final String[] args;
    private final Args argParser;
    Map<String, String> flags = new HashMap<>();
    public static String outputDirectory = null;
    private String language = "Java";

    public SmcCompiler(String[] args, Args argParser) {
      this.args = args;
      this.argParser = argParser;
    }

    public void run() throws IOException {
      extractCommandLineArguments();

      FsmSyntax fsm = compile(getSourceCode());
	    fsm.build_parse_tree();

      int syntaxErrorCount = reportSyntaxErrors(fsm);

      if (syntaxErrorCount == 0) if (flags.containsKey("isOptimized") && flags.get("isOptimized").equals("false")) {
        generateCode(analyze(fsm));
      } else {
        generateCode(optimize(fsm));
      }
    }

    private void generateCode(SemanticStateMachine ast) throws IOException {
      String generatorClassName = String.format("smc.generators.%sCodeGenerator", language);
      CodeGenerator generator = createGenerator(generatorClassName, ast, outputDirectory, flags);
      generator.generate();
    }

    private CodeGenerator createGenerator(String generatorClassName, SemanticStateMachine ast, String outputDirectory, Map<String, String> flags) {
      try {
        Class<?> generatorClass = Class.forName(generatorClassName);
        Constructor<?> constructor = generatorClass.getConstructor(SemanticStateMachine.class, String.class, Map.class);
        return (CodeGenerator) constructor.newInstance(ast, outputDirectory, flags);
      } catch (ClassNotFoundException e) {
        System.out.printf("The class %s was not found.\n", generatorClassName);
        System.exit(0);
      } catch (NoSuchMethodException e) {
        System.out.printf("Appropriate constructor for %s not found", generatorClassName);
        System.exit(0);
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        e.printStackTrace();
        System.exit(0);
      }
      return null;
    }

    private SemanticStateMachine analyze(FsmSyntax fsm) {
      SemanticStateMachine sm = new SemanticAnalyzer().analyze(fsm);
	    // TODO: Build Semantic Tree
			// sm.build_semantic_tree();
      reportSemanticErrors(sm);
      reportSemanticWarnings(sm);
      return sm;
    }

    private void reportSemanticErrors(SemanticStateMachine sm) {
      System.out.printf("Compiled with %d semantic error%s.%n", sm.errors.size(), (sm.errors.size() == 1 ? "" : "s"));

      for (SemanticStateMachine.AnalysisError error : sm.errors)
        System.out.println(error.toString());
    }

    private void reportSemanticWarnings(SemanticStateMachine sm) {
      System.out.printf("Compiled with %d semantic warning%s.%n", sm.warnings.size(), (sm.warnings.size() == 1 ? "" : "s"));

      for (SemanticStateMachine.AnalysisWarning warning : sm.warnings)
        System.out.println(warning.toString());
    }

    private void extractCommandLineArguments() {
      if (argParser.has('o')) outputDirectory = argParser.getString('o');
      if (argParser.has('l')) language = argParser.getString('l');
      if (argParser.has('f')) flags = argParser.getMap('f');
    }

    private FsmSyntax compile(String smContent) {
      SyntaxBuilder syntaxBuilder = new SyntaxBuilder();
      Parser parser = new Parser(syntaxBuilder);
      Lexer lexer = new Lexer(parser);
      lexer.lex(smContent);
      parser.handleEvent(EOF, -1, -1);

      return syntaxBuilder.getFsm();
    }

    private String getSourceCode() throws IOException {
      String sourceFileName = args[argParser.nextArgument()];
      return new String(Files.readAllBytes(Paths.get(sourceFileName)));
    }

    private int reportSyntaxErrors(FsmSyntax fsm) {
      int syntaxErrorCount = fsm.errors.size();
      System.out.printf("Compiled with %d syntax error%s.%n", syntaxErrorCount, (syntaxErrorCount == 1 ? "" : "s"));

      for (FsmSyntax.SyntaxError error : fsm.errors)
        System.out.println(error.toString());
      return syntaxErrorCount;
    }

    private OptimizedStateMachine optimize(FsmSyntax fsm) {
      return new Optimizer().optimize(analyze(fsm));
    }

    private void generateCode(OptimizedStateMachine optimizedStateMachine) throws IOException {
      String generatorClassName = String.format("smc.generators.%sCodeGenerator", language);
      CodeGenerator generator = createGenerator(generatorClassName, optimizedStateMachine, outputDirectory, flags);
      generator.generate();
    }

    private CodeGenerator createGenerator(String generatorClassName, OptimizedStateMachine optimizedStateMachine, String outputDirectory, Map<String, String> flags) {
      try {
        Class<?> generatorClass = Class.forName(generatorClassName);
        Constructor<?> constructor = generatorClass.getConstructor(OptimizedStateMachine.class, String.class, Map.class);
        return (CodeGenerator) constructor.newInstance(optimizedStateMachine, outputDirectory, flags);
      } catch (ClassNotFoundException e) {
        System.out.printf("The class %s was not found.\n", generatorClassName);
        System.exit(0);
      } catch (NoSuchMethodException e) {
        System.out.printf("Appropriate constructor for %s not found", generatorClassName);
        System.exit(0);
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        e.printStackTrace();
        System.exit(0);
      }
      return null;
    }

  }

}
