package cloudlens.parser;

import java.util.Collection;

import cloudlens.block.BlockException;
import cloudlens.block.JSEngine;
import cloudlens.engine.CLException;
import cloudlens.engine.PipelineStep;

public class ASTAfter extends ASTStreamingSection {
  public Collection<Collection<String>> clauses;
  public PipelineStep step;
  public String script;

  public ASTAfter(String file, int line, ASTArgs args, String script) {
    super(file, line, ASTType.After, args);

    final String afterScript = " function (){" + script + "}";

    try {
      JSEngine.checkSyntax(file, line, afterScript);
    } catch (final BlockException e) {
      throw new CLException(e.getMessage());
    }

    this.script = afterScript;
  }

}
