/*
 *  This file is part of the CloudLens project.
 *
 * Copyright omitted for blind review
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloudlens.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import cloudlens.block.BlockObject;
import cloudlens.parser.ASTAfter;
import cloudlens.parser.ASTBlock;
import cloudlens.parser.ASTDeclaration;
import cloudlens.parser.ASTElement;
import cloudlens.parser.ASTLens;
import cloudlens.parser.ASTMatch;
import cloudlens.parser.ASTProcess;
import cloudlens.parser.ASTRun;

public class CLBuilder {
  private static boolean blockGuard = false;
  private static List<CLElement> nextList = new ArrayList<>();
  private static List<PipelineStage> pipeline = new ArrayList<>();

  public static void reset() {
    blockGuard = false;
    nextList = new ArrayList<>();
    pipeline = new ArrayList<>();
  }

  private static String header(List<ASTElement> astElements) {
    String header = "";
    for (final ASTElement e : astElements) {
      switch (e.type) {
      case Declaration:
        header += "var " + ((ASTDeclaration) e).name + ";";
        break;
      case Lens:
        header += "var " + ((ASTLens) e).name + ";";
        break;
      default:
        break;
      }
    }
    return header;
  }

  public static String compileTop(List<ASTElement> astElements) {
    String code = header(astElements);
    code += "function (){ " + compile(astElements) + "}";
    return code;
  }

  public static String compile(List<ASTElement> astElements) {
    String code = "return [";
    String closing = "]";
    for (final ASTElement e : astElements) {
      switch (e.type) {
      case Declaration:
        final ASTDeclaration declSec = (ASTDeclaration) e;
        code += " function () {" + declSec.body + "; return [";
        closing = " ]}" + closing;
        break;
      case Lens:
        code += "function () {" + compile((ASTLens) e) + "; return [";
        closing = " ]}" + closing;
        break;
      case Run:
        final ASTRun runSec = (ASTRun) e;
        code += "function () { var run = " + runSec.name + ".closure("
            + runSec.args + "); run.ast = " + runSec.name
            + ".ast; return run},";
        break;
      case Block:
        final ASTBlock blockSec = (ASTBlock) e;
        code += " function () {" + blockSec.script + "; return [";
        closing = "]}" + closing;
        break;
      case Process:
        final ASTProcess processSec = (ASTProcess) e;
        code += processSec.script + ",";
        break;
      case After:
        final ASTAfter afterSec = (ASTAfter) e;
        code += afterSec.script + ",";
        break;
      case Match:
        final ASTMatch match = (ASTMatch) e;
        code += "function (){ return [" + StringUtils.join(match.rules, ',')
            + "]},";
        break;
      case Source:
        code += "{},";
        break;
      }
    }
    return code += closing;
  }

  public static String compile(ASTLens lens) {
    final List<String> logArgs = new ArrayList<>(lens.args);
    logArgs.add("log");
    final String funArgs = StringUtils.join(logArgs, ',');
    final String args = StringUtils.join(lens.args, ',');
    String code = "";
    code += lens.name + " = function(" + funArgs + "){return CL.run("
        + lens.name + ", [" + args + "], log) };";
    code += lens.name + ".closure = function (";
    if (!lens.args.isEmpty()) {
      code += "jsArray){";
      for (int i = 0; i < lens.args.size(); i++) {
        code += "var " + lens.args.get(i) + " = jsArray[" + i + "];";
      }
    } else {
      code += "){";
    }
    code += header(lens.astElements);
    code += compile(lens.astElements) + "};";
    code += lens.name + ".ast = CLDEV__LENS__;";
    return code;
  }

  public static void spawn(CLElement element, List<ASTElement> astElements) {
    if (!astElements.isEmpty()) {
      final ASTElement e = astElements.get(0);
      final List<ASTElement> astTail = astElements.subList(1,
          astElements.size());
      switch (e.type) {
      case Declaration:
      case Block:
      case Lens:
        final CLElement dbl = new CLElement(e);
        element.children.add(dbl);
        spawn(dbl, astTail);
        break;
      case Process:
      case After:
      case Run:
      case Match:
      case Source:
        final CLElement srgmrs = new CLElement(e);
        element.children.add(srgmrs);
        spawn(element, astTail);
        break;
      }
    }
  }

  private static List<CLElement> getElements(List<CLElement> elements) {
    final List<CLElement> res = new ArrayList<>();
    int i = 0;
    while (i < elements.size() && !blockGuard) {
      final CLElement child = elements.get(i++);
      switch (child.ast.type) {
      case Block:
        res.add(child);
        blockGuard = true;
        break;
      case Process:
      case After:
      case Match:
      case Source:
        res.add(child);
        break;
      case Run:
        final CLElement run = child;
        final List<CLElement> runElements = getElements(run.children);
        res.addAll(runElements);
        break;
      case Declaration:
      case Lens:
        final CLElement dl = child;
        final List<CLElement> lensElements = getElements(dl.children);
        res.addAll(lensElements);
        break;
      }
    }
    nextList.addAll(elements.subList(i, elements.size()));
    return res;
  }

  private static List<CLElement> extractElements(List<CLElement> elements) {
    elements.addAll(nextList);
    nextList = new ArrayList<>();
    blockGuard = false;
    return getElements(elements);
  }

  private static void populate(CL cl, CLElement element, BlockObject closures) {
    for (int i = 0; i < closures.size(); i++) {
      final CLElement child = element.children.get(i);
      final BlockObject closure = closures.get(i);
      switch (child.ast.type) {
      case Block:
      case Process:
      case After:
      case Match:
      case Source:
        final CLElement bsgmrs = child;
        bsgmrs.closure = closure;
        break;
      case Declaration:
        final CLElement decl = child;
        decl.closure = closure;
        final BlockObject declClosures = closure.call();
        populate(cl, decl, declClosures);
        break;
      case Lens:
        final CLElement lens = child;
        lens.closure = closure;
        cl.engine.bind("CLDEV__LENS__", lens.lens());
        final BlockObject lensClosures = closure.call();
        populate(cl, lens, lensClosures);
        break;
      case Run:
        final CLElement run = child;
        run.closure = closure;
        final BlockObject runClosures = closure.call();
        final ASTLens runLens = (ASTLens) runClosures.get("ast").asAst();
        spawn(run, runLens.astElements);
        populate(cl, run, runClosures);
        break;
      }
    }
  }

  private static void addRuntimePipeline(List<RuntimeElement> runtimeElements) {
    if (!pipeline.isEmpty()) {
      runtimeElements.add(new RuntimePipeline(pipeline));
      pipeline = new ArrayList<>();
    }
  }

  public static List<RuntimeElement> build(CL cl, CLElement element,
      BlockObject closures) {
    populate(cl, element, closures);
    final List<RuntimeElement> runtimes = new ArrayList<>();
    final List<CLElement> elements = extractElements(element.children);
    for (final CLElement child : elements) {
      switch (child.ast.type) {
      case Block:
        addRuntimePipeline(runtimes);
        final RuntimeBlock block = new RuntimeBlock(child);
        runtimes.add(block);
        break;
      case Source:
        addRuntimePipeline(runtimes);
        final RuntimeSource source = new RuntimeSource(child);
        runtimes.add(source);
        break;
      case Process:
        final PipelineStage process = new PipelineStageProcess(child);
        pipeline.add(process);
        break;
      case After:
        final PipelineStage after = new PipelineStageAfter(child);
        pipeline.add(after);
        break;
      case Match:
        final PipelineStage match = new PipelineStageMatch(child);
        pipeline.add(match);
        break;
      case Declaration:
      case Lens:
      case Run:
        throw new CLException("Runtime Error!");

      }
    }
    addRuntimePipeline(runtimes);
    return runtimes;
  }

}
