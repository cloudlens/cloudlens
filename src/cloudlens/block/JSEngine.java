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

package cloudlens.block;

import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class JSEngine implements BlockEngine {
  private final ScriptEngine scriptEngine;

  public JSEngine() {
    System.setProperty("nashorn.args", "--no-java");
    scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    scriptEngine.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
  }

  private CompiledBlock newObject;

  @Override
  public JSObject newObject() {
    if (newObject == null) {
      newObject = compile("new Object()");
    }
    return new JSObject(newObject.eval());
  }

  @Override
  public JSObject newObject(Object o) {
    return new JSObject(o);
  }

  private CompiledBlock isUndefined;

  @SuppressWarnings("restriction")
  @Override
  public boolean isUndefined(Object o) {
    if (isUndefined == null) {
      isUndefined = compile("function(o) { return o == undefined; }");
    }

    return (boolean) ((jdk.nashorn.api.scripting.JSObject) isUndefined.eval())
        .call(null, o);
  }

  @Override
  public boolean isUndefined(BlockObject o) {
    return isUndefined(o.internalObject());
  }

  private CompiledBlock newArray;

  @Override
  public JSObject newArray() {
    if (newArray == null) {
      newArray = compile("[]");
    }
    return new JSObject(newArray.eval());
  }

  @Override
  public JSObject newArray(ArrayList<BlockObject> array) {
    final JSObject res = newArray();
    for (final BlockObject entry : array) {
      res.push(entry);
    }
    return res;
  }

  private CompiledBlock isArray;

  @SuppressWarnings("restriction")
  @Override
  public boolean isArray(Object array) {
    if (isArray == null) {
      isArray = compile(
          "function(array) { return Array.isArray(array) === true; }");
    }

    return (boolean) ((jdk.nashorn.api.scripting.JSObject) isArray.eval())
        .call(null, array);
  }

  @Override
  public boolean isArray(BlockObject array) {
    return isArray(array.internalObject());
  }

  private CompiledBlock push;

  @SuppressWarnings("restriction")
  private Object push(Object array, Object entry) {
    if (push == null) {
      push = compile("function(array, entry) array.push(entry)");
    }

    return ((jdk.nashorn.api.scripting.JSObject) push.eval()).call(null, array,
        entry);
  }

  @Override
  public BlockObject push(BlockObject array, BlockObject entry) {
    return new JSObject(push(array.internalObject(), entry.internalObject()));
  }

  private CompiledBlock merge;

  @SuppressWarnings("restriction")
  private Object merge(Object acc, Object entry, String groupLbl) {
    if (merge == null) {
      merge = compile("(function (acc, e, groupLbl) {"
          + "    function mergeBoolean(b1, b2) {" + "        return b1 || b2;"
          + "    }" + " " + "    function mergeNumber(n1, n2) {"
          + "        return n2;" + "    }" + " "
          + "    function mergeString(s1, s2) {"
          + "        return s1.concat('\\n', s2);" + "    }" + " "
          + "    function mergeArray(a1, a2) {"
          + "        return a1.concat(a2);" + "    }" + " "
          + "    function mergeObject (acc, e) {"
          + "        for (var property in e) {"
          + "            if (!e.hasOwnProperty(property)) {"
          + "                break;" + "            }"
          + "            if (acc[property] === undefined) {"
          + "                acc[property] = e[property];"
          + "            } else if (typeof acc[property] !== typeof e[property]) {"
          + "                acc[property] = e[property];"
          + "            } else if (typeof e[property] === 'boolean') {"
          + "                acc[property] = mergeBoolean(acc[property], e[property]);"
          + "            } else if (typeof e[property] === 'number') {"
          + "                acc[property] = mergeNumber(acc[property], e[property]);"
          + "            } else if (typeof e[property] === 'string') {"
          + "                acc[property] = mergeString(acc[property], e[property]);"
          + "            } else if (Array.isArray(e[property])) {"
          + "                acc[property] = mergeArray(acc[property], e[property]);"
          + "            } else if (typeof e[property] === 'object') {"
          + "                mergeObject(acc[property], e[property]);"
          + "            } else {"
          + "                acc[property] = e[property];" + "            }"
          + "        }" + "    }" + " " + "    if (groupLbl === undefined) {"
          + "        groupLbl = 'group';" + "    }"
          + "    if (acc[groupLbl] === undefined) {"
          + "        acc[groupLbl] = [];" + "    }" + "    mergeObject(acc, e);"
          + "    acc[groupLbl].push(e);" + "})");
    }

    return ((jdk.nashorn.api.scripting.JSObject) merge.eval()).call(null, acc,
        entry, groupLbl);
  }

  @Override
  public BlockObject merge(BlockObject acc, BlockObject entry,
      String groupLbl) {
    return new JSObject(
        merge(acc.internalObject(), entry.internalObject(), groupLbl));
  }

  @Override
  public String typeof(Object obj) {
    return eval("typeof " + obj).toString();
  }

  @Override
  public CompiledBlock compile(String script) {
    if (scriptEngine instanceof Compilable) {
      final Compilable compiler = (Compilable) scriptEngine;
      try {
        final CompiledScript compiledScript = compiler.compile(script);
        return new JSCompiledBlock(compiledScript);
      } catch (final ScriptException exn) {
        throw new BlockException(exn.getMessage());
      }
    }
    final JSEngine self = this;
    return new CompiledBlock() {
      @Override
      public Object eval() {
        return self.eval(script);
      }
    };
  }

  @Override
  public JSObject eval(String script) {
    try {
      return new JSObject(scriptEngine.eval(script));
    } catch (final ScriptException e) {
      throw new BlockException(e.getMessage());
    }
  }

  @Override
  public JSObject eval(InputStreamReader script) {
    try {
      return new JSObject(scriptEngine.eval(script));
    } catch (final ScriptException e) {
      throw new BlockException(e.getMessage());
    }
  }

  @Override
  public ScriptContext getContext() {
    return scriptEngine.getContext();
  }

  @Override
  public void bind(String name, Object value) {
    getContext().getBindings(ScriptContext.ENGINE_SCOPE).put(name, value);
  }

  @Override
  public void bind(String name, BlockObject value) {
    bind(name, value.internalObject());
  }

  private CompiledBlock link;

  @Override
  public void link(String var1, String var2) {
    link = compile("var " + var1 + " = " + var2);
    link.eval();
  }

  @SuppressWarnings("restriction")
  public static void checkSyntax(String file, int line, String script) {
    final jdk.nashorn.internal.runtime.options.Options options = new jdk.nashorn.internal.runtime.options.Options(
        "nashorn");
    options.set("anon.functions", true);
    options.set("parse.only", true);
    options.set("scripting", true);

    final jdk.nashorn.internal.runtime.ErrorManager errors = new jdk.nashorn.internal.runtime.ErrorManager();
    final jdk.nashorn.internal.runtime.Context contextm = new jdk.nashorn.internal.runtime.Context(
        options, errors, Thread.currentThread().getContextClassLoader());
    jdk.nashorn.internal.runtime.Context.setGlobal(contextm.createGlobal());

    try {
      jdk.nashorn.api.scripting.ScriptUtils.parse(script, "<parse>", false);
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException("Syntax Error: " + file + " block starting line "
          + line + ":\n" + e.getMessage().substring(13));
    }
  }

}
