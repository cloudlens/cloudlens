/*
 *  This file is part of the CloudLens project.
 *
 * Copyright 2015-2016 IBM Corporation
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cloudlens.block.BlockObject;
import cloudlens.parser.ASTBlock;
import cloudlens.parser.ASTDeclaration;
import cloudlens.parser.ASTElement;
import cloudlens.parser.ASTGroup;
import cloudlens.parser.ASTLens;
import cloudlens.parser.ASTMatch;
import cloudlens.parser.ASTRun;
import cloudlens.parser.ASTStream;

public class CLBuilder {
  private static boolean blockGuard = false;
  private static List<CLElement> nextList = new ArrayList<>();
  private static Pipeline pipeline = new Pipeline("log");
  private static String lastStream = "log";
  private static Set<String> streams = new HashSet<>();

  public static void reset() {
    blockGuard = false;
    nextList = new ArrayList<>();
    pipeline = new Pipeline("log");
    lastStream = "log";
    streams = new HashSet<>();
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
      case Stream:
        final ASTStream streamSec = (ASTStream) e;
        code += " function (" + streamSec.var + "){" + streamSec.script
            + "; return " + streamSec.var + "},";
        break;
      case Match:
        final ASTMatch match = (ASTMatch) e;
        code += "function (){ return [" + StringUtils.join(match.rules, ',')
            + "]},";
        break;
      case Group:
        final ASTGroup group = (ASTGroup) e;
        code += "function (){return [" + StringUtils.join(group.rules, ',')
            + "]},";
        break;
      case Restart:
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
      case Stream:
      case Run:
      case Group:
      case Match:
      case Restart:
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
      case Stream:
      case Match:
      case Group:
      case Restart:
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
      case Stream:
      case Group:
      case Match:
      case Restart:
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

  private static void checkStream(CL cl, String file, int line, String stream) {
    if (!lastStream.startsWith(stream) && streams.contains(stream)) {
      cl.errWriter.println("Warning: " + file + " line " + line
          + ", implicit restart of stream " + stream + "!");
    }
    streams.add(stream);
    lastStream = stream;
  }

  private static void addRuntimePipeline(List<RuntimeElement> runtimeElements) {
    if (!pipeline.isEmpty()) {
      runtimeElements.add(new RuntimePipeline(pipeline));
      pipeline = new Pipeline("log");
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
        lastStream = "top";
        runtimes.add(block);
        break;
      case Source:
        addRuntimePipeline(runtimes);
        final RuntimeSource source = new RuntimeSource(child);
        lastStream = "log";
        runtimes.add(source);
        break;
      case Stream:
        final PipelineStage stream = new PipelineStageStream(child);
        final ASTStream s = child.stream();
        checkStream(cl, s.file, s.line, s.stream);
        pipeline.add(stream);
        break;
      case Group:
        final PipelineStage group = new PipelineStageGroup(child);
        final ASTGroup g = child.group();
        checkStream(cl, g.file, g.line, g.stream);
        pipeline.add(group);
        break;
      case Match:
        final PipelineStage match = new PipelineStageMatch(child);
        final ASTMatch m = child.match();
        checkStream(cl, m.file, m.line, m.stream);
        pipeline.add(match);
        break;
      case Restart:
        final String restartStream = child.restart().stream;
        lastStream = child.restart().stream;
        if (restartStream.equals("log")) {
          addRuntimePipeline(runtimes);
        } else {
          pipeline.addNode(child.restart().stream);
        }
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
