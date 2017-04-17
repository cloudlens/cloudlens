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

package cloudlens.parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import cloudlens.parser.CloudLensParser.ClauseContext;
import cloudlens.parser.CloudLensParser.ElementContext;
import cloudlens.parser.CloudLensParser.RegexContext;

public class ASTBuilder extends CloudLensBaseVisitor<Void> {
  private List<ASTElement> astElements;
  private final Map<String, ASTLens> lenses = new HashMap<>();
  private static String fileName = "";

  public static List<ASTElement> parseFiles(String[] files)
      throws ASTException, IOException {
    final List<ASTElement> res = new ArrayList<>();
    for (final String file : files) {
      res.addAll(parseFile(file));
    }
    return res;
  }

  public static List<ASTElement> parseFile(String file)
      throws IOException, ASTException {
    fileName = file;
    final ANTLRFileStream script = new ANTLRFileStream(file);
    return parse(script);
  }

  public static List<ASTParagraph> parseZeppelinExport(String file)
      throws FileNotFoundException, IOException {
    final List<ASTParagraph> res = new ArrayList<>();
    try (final FileReader rd = new FileReader(file);) {
      final JsonObject notebook = new JsonParser().parse(rd).getAsJsonObject();
      final JsonArray paragraphs = notebook.getAsJsonArray("paragraphs");

      final Iterator<JsonElement> it = paragraphs.iterator();
      while (it.hasNext()) {
        final JsonObject p = it.next().getAsJsonObject();
        final JsonPrimitive textOpt = p.getAsJsonPrimitive("text");
        if (textOpt != null) {
          final String text = textOpt.getAsString();
          if (text.startsWith("%cl")) {
            final String content = text.substring(3);
            final List<ASTElement> elements = parse(content);
            res.add(new ASTParagraph(elements));
          } else {
            res.add(new ASTParagraph(text));
          }
        }
      }
    }
    return res;

  }

  public static List<ASTElement> parse(String st) {
    final ANTLRInputStream script = new ANTLRInputStream(st);
    return parse(script);
  }

  public static List<ASTElement> parse(ANTLRInputStream script) {
    final CloudLensLexer clLexer = new CloudLensLexer(script);
    clLexer.removeErrorListeners();
    clLexer.addErrorListener(ASTError.INSTANCE);
    final CommonTokenStream token = new CommonTokenStream(clLexer);
    final CloudLensParser clParser = new CloudLensParser(token);
    clParser.removeErrorListeners();
    clParser.addErrorListener(ASTError.INSTANCE);
    final ASTBuilder clAST = new ASTBuilder();
    clAST.visit(clParser.top());
    return clAST.astElements;
  }

  @Override
  public Void visitTop(CloudLensParser.TopContext ctx) {
    visitChildren(ctx);
    astElements = ctx.script().ast;
    return null;
  }

  @Override
  public Void visitScript(CloudLensParser.ScriptContext ctx) {
    visitChildren(ctx);
    ctx.ast = new ArrayList<>();
    for (final ElementContext e : ctx.element()) {
      ctx.ast.add(e.ast);
    }
    return null;
  }

  @Override
  public Void visitElement(CloudLensParser.ElementContext ctx) {
    visitChildren(ctx);
    if (ctx.declaration() != null) {
      ctx.ast = ctx.declaration().ast;
    } else if (ctx.block() != null) {
      ctx.ast = ctx.block().ast;
    } else if (ctx.process() != null) {
      ctx.ast = ctx.process().ast;
    } else if (ctx.after() != null) {
      ctx.ast = ctx.after().ast;
    } else if (ctx.match() != null) {
      ctx.ast = ctx.match().ast;
    } else if (ctx.lens() != null) {
      ctx.ast = ctx.lens().ast;
    } else if (ctx.run() != null) {
      ctx.ast = ctx.run().ast;
    } else if (ctx.source() != null) {
      ctx.ast = ctx.source().ast;
    }
    return null;
  }

  @Override
  public Void visitBlock(CloudLensParser.BlockContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    ctx.ast = new ASTBlock(fileName, line, ctx.body().ast);
    return null;
  }

  @Override
  public Void visitProcess(CloudLensParser.ProcessContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String var = (ctx.IDENT() != null) ? ctx.IDENT().getText() : null;
    ctx.ast = new ASTProcess(fileName, line, var, ctx.conditions().ast,
        ctx.body().ast);
    return null;
  }

  @Override
  public Void visitAfter(CloudLensParser.AfterContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    ctx.ast = new ASTAfter(fileName, line, ctx.body().ast);
    return null;
  }

  @Override
  public Void visitMatch(CloudLensParser.MatchContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String upon = (ctx.IDENT() != null) ? ctx.IDENT().getText()
        : "entry.message";
    ctx.ast = new ASTMatch(fileName, line, ctx.rules().ast, upon);
    return null;
  }

  @Override
  public Void visitLens(CloudLensParser.LensContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String name = ctx.IDENT().getText();
    ctx.ast = new ASTLens(fileName, line, name, ctx.identList().ast,
        ctx.lensBody().ast);
    lenses.put(name, ctx.ast);
    return null;
  }

  @Override
  public Void visitRun(CloudLensParser.RunContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String args = ctx.argList().ast;
    ctx.ast = new ASTRun(fileName, line, ctx.IDENT().getText(), args);
    return null;
  }

  @Override
  public Void visitSource(CloudLensParser.SourceContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String format = (ctx.format() != null) ? ctx.format().ast : null;
    final String path = (ctx.path() != null) ? ctx.path().ast : null;
    ctx.ast = new ASTSource(fileName, line, ctx.url().ast, format, path);
    return null;
  }

  @Override
  public Void visitDeclaration(CloudLensParser.DeclarationContext ctx) {
    visitChildren(ctx);
    if (ctx.varDecl() != null) {
      ctx.ast = ctx.varDecl().ast;
    } else if (ctx.funDecl() != null) {
      ctx.ast = ctx.funDecl().ast;
    }
    return null;
  }

  @Override
  public Void visitVarDecl(CloudLensParser.VarDeclContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String ident = ctx.IDENT().getText();
    final int a = ctx.start.getStartIndex();
    final int b = ctx.stop.getStopIndex();
    final Interval interval = new Interval(a, b);
    final CharStream source = ctx.start.getInputStream();
    final String body = source.getText(interval);
    ctx.ast = new ASTDeclaration(fileName, line, ident, body);
    return null;
  }

  @Override
  public Void visitFunDecl(CloudLensParser.FunDeclContext ctx) {
    visitChildren(ctx);
    final int line = ctx.getStart().getLine();
    final String ident = ctx.IDENT().getText();
    final List<String> args = ctx.identList().ast;
    final String funBody = ctx.body().ast;
    final String jsArgs = StringUtils.join(args, ',');
    final String body = ident + " = function (" + jsArgs + "){" + funBody + "}";
    ctx.ast = new ASTDeclaration(fileName, line, ident, body);
    return null;
  }

  @Override
  public Void visitConditions(CloudLensParser.ConditionsContext ctx) {
    ctx.ast = new ArrayList<>();
    if (ctx.clause() != null) {
      visitChildren(ctx);
      for (final ClauseContext clause : ctx.clause()) {
        ctx.ast.add(clause.ast);
      }
    }
    return null;
  }

  @Override
  public Void visitClause(CloudLensParser.ClauseContext ctx) {
    ctx.ast = new ArrayList<>();
    for (final TerminalNode cond : ctx.IDENT()) {
      ctx.ast.add(cond.getText());
    }
    return null;
  }

  @Override
  public Void visitBody(CloudLensParser.BodyContext ctx) {
    final int a = ctx.start.getStartIndex();
    final int b = ctx.stop.getStopIndex();
    // remove opening and closing {.}
    final Interval interval = new Interval(a + 1, b - 1);
    final CharStream source = ctx.start.getInputStream();
    ctx.ast = source.getText(interval);
    return null;
  }

  @Override
  public Void visitRules(CloudLensParser.RulesContext ctx) {
    visitChildren(ctx);
    ctx.ast = new ArrayList<>();
    for (final RegexContext regex : ctx.regex()) {
      ctx.ast.add(regex.ast);
    }
    return null;
  }

  @Override
  public Void visitRegex(CloudLensParser.RegexContext ctx) {
    visitChildren(ctx);
    final List<String> rg = new ArrayList<>();
    if (ctx.IDENT() != null) {
      rg.add(ctx.IDENT().getText());
    } else if (ctx.STRING() != null) {
      final int a = ctx.start.getStartIndex();
      final int b = ctx.stop.getStopIndex();
      // remove opening and closing "."
      final Interval interval = new Interval(a + 1, b - 1);
      final CharStream source = ctx.start.getInputStream();
      final String r = source.getText(interval);
      rg.add("\"" + StringEscapeUtils.escapeEcmaScript(r) + "\"");
    } else if (ctx.regex() != null) {
      for (final RegexContext r : ctx.regex()) {
        rg.add(r.ast);
      }
    }
    ctx.ast = StringUtils.join(rg, "+");
    return null;
  }

  @Override
  public Void visitIdentList(CloudLensParser.IdentListContext ctx) {
    ctx.ast = new ArrayList<>();
    for (final TerminalNode arg : ctx.IDENT()) {
      ctx.ast.add(arg.getText());
    }
    return null;
  }

  @Override
  public Void visitArgList(CloudLensParser.ArgListContext ctx) {
    final int a = ctx.start.getStartIndex();
    final int b = ctx.stop.getStopIndex();
    // remove opening and closing (.)
    final Interval interval = new Interval(a + 1, b - 1);
    final CharStream source = ctx.start.getInputStream();
    ctx.ast = "[" + source.getText(interval) + "]";
    return null;
  }

  @Override
  public Void visitLensBody(CloudLensParser.LensBodyContext ctx) {
    visitChildren(ctx);
    ctx.ast = ctx.script().ast;
    return null;
  }

  @Override
  public Void visitUrl(CloudLensParser.UrlContext ctx) {
    final int a = ctx.start.getStartIndex();
    final int b = ctx.stop.getStopIndex();
    // remove opening and closing "."
    final Interval interval = new Interval(a + 1, b - 1);
    final CharStream source = ctx.start.getInputStream();
    ctx.ast = source.getText(interval);
    return null;
  }

  @Override
  public Void visitFormat(CloudLensParser.FormatContext ctx) {
    ctx.ast = ctx.IDENT().getText();
    return null;
  }

  @Override
  public Void visitPath(CloudLensParser.PathContext ctx) {
    ctx.ast = ctx.IDENT().getText();
    return null;
  }
}
