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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockObject;
import cloudlens.block.JSEngine;
import cloudlens.parser.ASTElement;
import cloudlens.parser.ASTLens;
import cloudlens.parser.FileReader;

public class CL {
  public boolean stream;
  public final BlockEngine engine;
  public final OutputStream out;
  public final PrintWriter outWriter;
  public final OutputStream err;
  public final PrintWriter errWriter;
  public final BlockObject cl;
  public Future<BlockObject> future;

  public Iterator<BlockObject> log;
  public List<CLIterator> heapIt;
  public boolean executed;
  private final boolean withHistory;

  public CL(OutputStream out, OutputStream err, boolean stream,
      boolean withHistory) {
    this.stream = stream;
    this.withHistory = withHistory;
    this.executed = false;
    this.out = out;
    outWriter = new PrintWriter(out);
    this.err = err;
    errWriter = new PrintWriter(err);
    engine = new JSEngine();
    engine.getContext().setWriter(outWriter);
    engine.getContext().setErrorWriter(errWriter);
    engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE).put("CLx",
        this);
    engine.eval(
        "(function() { var f = __noSuchProperty__; __noSuchProperty__ = function(name) { try { return f(name); } catch(err) { return undefined; } } })()");
    engine.eval("var CL = {result:null,"
        + "log: (function() { var v = CLx; return function() v.log() }) (),"
        + "loadjs:(function() { var v = CLx; return function(url) v.loadjs(url) })(),"
        + "export:(function() { var v = CLx; return function(file) v.export(file) })(),"
        + "findRegex:(function() { var v = CLx; return function(regex, input) v.findRegex(regex, input) })(),"
        + "run:(function(){var v = CLx; return function(lens, stream, jsArgs){return v.run(lens, stream, jsArgs)} })(),"
        + " }");
    engine.eval(
        "CL.log.toString = function() 'function log() { [native code] }'");
    engine.eval(
        "CL.loadjs.toString = function() 'function loadjs() { [native code] }'");
    engine.eval(
        "CL.export.toString = function() 'function export() { [native code] }'");
    engine.eval(
        "CL.findRegex.toString = function() 'function findRegex() { [native code] }'");
    engine.eval(
        "CL.run.toString = function() 'function run() { [native code] }'");
    engine.eval(
        "Object.defineProperty(Function('return this')(), 'CL', {configurable:false, writable:false})");
    this.cl = engine.eval("CL");
    this.heapIt = new ArrayList<>();
    heapIt.add(0, new CLIterator(engine, engine.newArray(), withHistory));
  }

  public void launch(List<ASTElement> astElements) {
    engine.eval("var CLDEV__LOG__ = {content: CL.log};");
    final String code = CLBuilder.compileTop(astElements);
    // System.out.println(code);
    final CLElement top = new CLElement();
    top.closure = engine.eval(code);
    CLBuilder.spawn(top, astElements);
    final BlockObject closures = top.closure.call();
    final List<RuntimeElement> runtimes = CLBuilder.build(this, top, closures);

    for (final RuntimeElement child : runtimes) {
      heapIt.set(0, child.run(this, heapIt.get(0), withHistory));
    }

    if (!executed) {
      errWriter.println("Warning: no commands executed!");
    }
  }

  public Object run(Object l, Object jsArray, Object logStream) {
    final BlockObject jsLens = engine.newObject(l);
    final BlockObject jsStream = engine.newObject(logStream);
    final ASTLens astLens = (ASTLens) jsLens.get("ast").asAst();
    final CLElement element = new CLElement(astLens);
    element.closure = jsLens.get("closure");

    CLBuilder.spawn(element, astLens.astElements);
    final BlockObject closures = element.closure.call(jsArray);
    final List<RuntimeElement> runtimes = CLBuilder.build(this, element,
        closures);
    if (!engine.isUndefined(jsStream)) {
      heapIt.add(0, new CLIterator(engine, jsStream, true));
    } else {
      heapIt.add(0, new CLIterator(engine, heapIt.get(0).history, true));
    }

    for (final RuntimeElement child : runtimes) {
      heapIt.set(0, child.run(this, heapIt.get(0), true));
    }

    final CLIterator res = heapIt.get(0);
    heapIt.remove(0);
    return engine.newArray(res.history).internalObject();
  }

  public Object log() {
    final BlockObject l = engine.newArray(heapIt.get(0).history);
    return l.internalObject();
  }

  public boolean findRegex(String regex, String input) {
    final Pattern pat = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    final Matcher m = pat.matcher(input);
    return m.find();
  }

  public void loadjs(String urlString) {
    final InputStream inputStream = FileReader.fetchFile(urlString);
    engine.eval(new InputStreamReader(inputStream));
  }

  public Object source(final InputStream inputStream) {
    heapIt.set(0, CLIterator.source(engine, inputStream, withHistory));
    return engine.newArray(heapIt.get(0).history).internalObject();
  }

  public Object json(final InputStream inputStream, String path) {
    heapIt.set(0, CLIterator.json(engine, inputStream, path, withHistory));
    return engine.newArray(heapIt.get(0).history).internalObject();
  }

  public void export(String path) throws IOException {
    final BlockObject log = cl.get("log");
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final String prettyOutput = gson.toJson(log);
    final PrintWriter writer = new PrintWriter(path);
    writer.println(prettyOutput);
    writer.close();
  }

  public Object post(String urlString, String jsonData) throws IOException {
    final URL url = new URL(urlString);

    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setDoInput(true);
    final Matcher matcher = Pattern.compile("//([^@]+)@").matcher(urlString);
    if (matcher.find()) {
      final String encoding = Base64.getEncoder()
          .encodeToString(matcher.group(1).getBytes());
      conn.setRequestProperty("Authorization", "Basic " + encoding);
    }
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Accept", "*/*");
    conn.setRequestMethod("POST");

    try (final OutputStreamWriter wr = new OutputStreamWriter(
        conn.getOutputStream())) {
      wr.write(jsonData);
      wr.flush();
    }
    try (final BufferedReader rd = new BufferedReader(
        new InputStreamReader(conn.getInputStream()))) {
      String line;
      final StringBuilder sb = new StringBuilder();
      while ((line = rd.readLine()) != null) {
        sb.append(line);
      }
      final String s = StringEscapeUtils.escapeEcmaScript(sb.toString());
      final Object log = engine.eval("CL.log=JSON.parse(\"" + s
          + "\").hits.hits.map(function (entry) { return entry._source})");
      return log;
    }
  }
}
