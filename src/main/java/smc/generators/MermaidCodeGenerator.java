package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.diagramGenerator.NonOptimizedDiagramGenerator;
import smc.generators.diagramGenerator.OptimizedDiagramGenerator;
import smc.implementers.MermaidDiagramImplementer;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class MermaidCodeGenerator extends CodeGenerator {
  private final MermaidDiagramImplementer implementer;
  private final String fsmName;

  public MermaidCodeGenerator(OptimizedStateMachine optimizedStateMachine,
                              String outputDirectory,
                              Map<String, String> flags) {
    super(optimizedStateMachine, outputDirectory, flags);
    implementer = new MermaidDiagramImplementer(flags);
    fsmName = optimizedStateMachine.header.fsm;
  }

  public MermaidCodeGenerator(SemanticStateMachine nonOptimizedStateMachine,
                              String outputDirectory,
                              Map<String, String> flags) {
    super(nonOptimizedStateMachine, outputDirectory, flags);
    implementer = new MermaidDiagramImplementer(flags);
    fsmName = nonOptimizedStateMachine.fsmName;
  }

  @Override
  protected void generate(boolean isOptimized) {
    if (isOptimized) {
      OptimizedDiagramGenerator optimizedDiagramGenerator = new OptimizedDiagramGenerator();
      optimizedDiagramGenerator.generate(optimizedStateMachine).accept(implementer);
    } else {
      NonOptimizedDiagramGenerator nonOptimizedDiagramGenerator = new NonOptimizedDiagramGenerator();
      nonOptimizedDiagramGenerator.generate(semanticStateMachine).accept(implementer);
    }
  }

  @Override
  protected void writeFiles() throws IOException {
    String outputFileName = fsmName + ".mmd";
    Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
  }

}
