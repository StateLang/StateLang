package smc.generators;

import smc.OptimizedStateMachine;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public abstract class CodeGenerator {
  protected final String outputDirectory;
  protected final Map<String, String> flags;
  protected OptimizedStateMachine optimizedStateMachine;
  protected SemanticStateMachine semanticStateMachine;

  public CodeGenerator(OptimizedStateMachine optimizedStateMachine, String outputDirectory, Map<String, String> flags) {
    this.optimizedStateMachine = optimizedStateMachine;
    this.outputDirectory = outputDirectory;
    this.flags = flags;
  }

  public CodeGenerator(SemanticStateMachine semanticStateMachine, String outputDirectory, Map<String, String> flags) {
    this.semanticStateMachine = semanticStateMachine;
    this.outputDirectory = outputDirectory;
    this.flags = flags;
  }

  protected Path getOutputPath(String outputFileName) {
    Path outputPath;
    if (outputDirectory == null) outputPath = FileSystems.getDefault().getPath(outputFileName);
    else outputPath = FileSystems.getDefault().getPath(outputDirectory, outputFileName);
    return outputPath;
  }

  public void generate() throws IOException {
    if (flags.containsKey("isOptimized")) generate(Boolean.parseBoolean(flags.get("isOptimized")));
    else generate(true);
    writeFiles();
  }

  protected abstract void generate(boolean isOptimized);

  protected abstract void writeFiles() throws IOException;

}
