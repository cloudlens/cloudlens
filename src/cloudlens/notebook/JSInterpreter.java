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

package cloudlens.notebook;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.InterpreterUtils;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cloudlens.block.BlockObject;
import cloudlens.engine.CL;

/**
 * JavaScript interpreter for Zeppelin.
 */
public class JSInterpreter extends Interpreter {
  static {
    Interpreter.register("js", "cloudlens", JSInterpreter.class.getName(),
        new InterpreterPropertyBuilder().add("zeppelin.cloudlens.maxResult",
            "10", "Max number of results to display.").build());
  }

  int maxResult;
  Map<String, CL> map;

  /**
   * Creates a JSInterpreter instance.
   *
   * @param property
   *          properties
   */
  public JSInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {
    maxResult = Integer.parseInt(getProperty("zeppelin.cloudlens.maxResult"));
    map = new HashMap<>();
  }

  @Override
  public void close() {
  }

  synchronized CL getCL(InterpreterContext context) {
    final String id = context.getRunners().get(0).getNoteId();
    if (!map.containsKey(id)) {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final ByteArrayOutputStream err = new ByteArrayOutputStream();
      map.put(id, new CL(out, err, false, true));
    }
    return map.get(id);
  }

  @Override
  public InterpreterResult interpret(String st, InterpreterContext context) {
    final CL cl = getCL(context);
    return interpret(() -> cl.engine.eval(st), cl);
  }

  public InterpreterResult interpret(Callable<BlockObject> task, CL cl) {
    if (cl.out instanceof ByteArrayOutputStream) {
      ((ByteArrayOutputStream) cl.out).reset();
    }
    if (cl.err instanceof ByteArrayOutputStream) {
      ((ByteArrayOutputStream) cl.err).reset();
    }
    final ExecutorService executor = Executors
        .newCachedThreadPool(new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            return new Thread(r) {
              @Override
              public void interrupt() {
                stop();
              }
            };
          }
        });
    cl.future = executor.submit(task);
    final Gson gson = new GsonBuilder().create();
    try {
      final BlockObject obj = cl.future.get();
      cl.outWriter.flush();
      cl.errWriter.flush();
      if (obj instanceof InterpreterResult) {
        return (InterpreterResult) obj;
      }
      if (cl.out instanceof ByteArrayOutputStream
          && ((ByteArrayOutputStream) cl.out).size() == 0) {
        if (null != obj && obj.isMapArray()) {
          final Map<String, Map<String, Object>> entries = obj.asMapArray();
          cl.outWriter.print("%table\n");
          int i = 0;
          for (final Map<?, ?> entry : entries.values()) {
            cl.outWriter.print("\n");
            if (++i > maxResult) {
              cl.outWriter.println(
                  "%html <font color=red>Results are limited by zeppelin.cloudlens.maxResult = "
                      + maxResult + ".</font>");
              break;
            }
            for (final Map.Entry<?, ?> field : entry.entrySet()) {
              cl.outWriter.print("%html <font color=blue>"
                  + StringEscapeUtils.escapeHtml4(field.getKey().toString())
                  + "</font>:" + StringEscapeUtils.escapeHtml4(
                      gson.toJson(field.getValue()).toString())
                  + "\t");
            }
          }
        } else {
          cl.engine.bind("__Result__", obj);
          cl.engine.eval(
              "print(JSON.stringify(__Result__, function(key, val) { if (typeof val === 'function') return val + ''; return val; }, 2))");
        }
      }
      // }
    } catch (final InterruptedException |

        ExecutionException e) {
      return new InterpreterResult(Code.ERROR,
          InterpreterUtils.getMostRelevantMessage(e));
    } finally {
      cl.outWriter.flush();
      cl.errWriter.flush();
      executor.shutdownNow();
    }
    return new InterpreterResult(Code.SUCCESS, cl.out.toString());
  }

  @Override
  public void cancel(InterpreterContext context) {
    map.get(context.getRunners().get(0).getNoteId()).future.cancel(true);
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public List<String> completion(String buf, int cursor) {
    return null;
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton().createOrGetFIFOScheduler("cloudlens");
  }
}
