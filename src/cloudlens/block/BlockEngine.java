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

import javax.script.ScriptContext;

public interface BlockEngine {
  public enum BlockTypes {
    JS
  }

  BlockObject newObject();

  BlockObject newObject(Object o);

  boolean isUndefined(Object o);

  boolean isUndefined(BlockObject o);

  BlockObject newArray();

  BlockObject newArray(ArrayList<BlockObject> array);

  boolean isArray(Object array);

  boolean isArray(BlockObject array);

  BlockObject push(BlockObject array, BlockObject entry);

  String typeof(Object obj);

  BlockObject merge(BlockObject acc, BlockObject entry, String groupLbl);

  BlockObject eval(String script);

  BlockObject eval(InputStreamReader script);

  ScriptContext getContext();

  CompiledBlock compile(String script);

  void bind(String name, Object value);

  void bind(String name, BlockObject value);

  void link(String var1, String var2);

}
