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

import java.util.Iterator;
import java.util.Map;

import cloudlens.parser.ASTElement;

public interface BlockObject {

  public Object internalObject();

  public boolean isUndefined();

  public boolean isMap();

  public Map<String, Object> asMap();

  public boolean isMapArray();

  public Map<String, Map<String, Object>> asMapArray();

  public boolean isIterator();

  public Iterator<BlockObject> asIterator();

  public boolean asBoolean();

  public String asString();

  public ASTElement asAst();

  public BlockObject get(String field);

  public void put(String field, Object o);

  public void put(String field, BlockObject o);

  public BlockObject call(Object... args);

  public BlockObject call(BlockObject... args);

  public boolean containsKey(String key);

  public boolean checkpath(String[] path, int pos);

  public BlockObject getpath(String[] path, int pos);

  public long size();

  public BlockObject get(int i);

  public void set(int i, BlockObject o);

  void push(BlockObject o);

  public Iterator<BlockObject> asList();

}
