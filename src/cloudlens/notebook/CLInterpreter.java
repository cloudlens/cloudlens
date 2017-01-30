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

import java.util.List;
import java.util.Properties;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.LazyOpenInterpreter;
import org.apache.zeppelin.interpreter.WrappedInterpreter;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;

import cloudlens.engine.CL;
import cloudlens.parser.ASTBuilder;
import cloudlens.parser.ASTElement;

/**
 * Cloudlens interpreter for Zeppelin.
 */
public class CLInterpreter extends Interpreter {
  static {
    Interpreter.register("cl", "cloudlens", CLInterpreter.class.getName(),
        new InterpreterPropertyBuilder().build());
  }

  /**
   * Creates an interpreter instance.
   *
   * @param property
   *          properties
   */
  public CLInterpreter(Properties property) {
    super(property);
  }

  private JSInterpreter getInterpreter() {
    final InterpreterGroup intpGroup = getInterpreterGroup();
    LazyOpenInterpreter lazy = null;
    JSInterpreter js = null;
    synchronized (intpGroup) {
      for (final Interpreter intp : getInterpreterGroup()) {
        if (intp.getClassName().equals(JSInterpreter.class.getName())) {
          Interpreter p = intp;
          while (p instanceof WrappedInterpreter) {
            if (p instanceof LazyOpenInterpreter) {
              lazy = (LazyOpenInterpreter) p;
            }
            p = ((WrappedInterpreter) p).getInnerInterpreter();
          }
          js = (JSInterpreter) p;
        }
      }
    }
    if (lazy != null) {
      lazy.open();
    }
    return js;
  }

  @Override
  public void open() {
  }

  @Override
  public void close() {
  }

  @Override
  public InterpreterResult interpret(String st, InterpreterContext context) {
    final JSInterpreter js = getInterpreter();
    final CL cl = js.getCL(context);

    final List<ASTElement> top = ASTBuilder.parse(st);
    return js.interpret(() -> {
      cl.launch(top);
      return cl.engine.newObject(cl.log());
    }, cl);
  }

  @Override
  public void cancel(InterpreterContext context) {
    getInterpreter().cancel(context);
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return getInterpreter().getProgress(context);
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
