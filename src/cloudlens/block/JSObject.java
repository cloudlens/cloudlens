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

public class JSObject implements BlockObject {
  private final Object nashornObject;

  protected JSObject(Object nashornObject) {
    this.nashornObject = nashornObject;
  }

  @Override
  public Object internalObject() {
    return nashornObject;
  }

  @SuppressWarnings("restriction")
  @Override
  public boolean isUndefined() {
    return (jdk.nashorn.api.scripting.ScriptObjectMirror
        .isUndefined(nashornObject));
  }

  @Override
  public boolean isMap() {
    return (nashornObject instanceof Map);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> asMap() {
    if (nashornObject instanceof Map) {
      return (Map<String, Object>) nashornObject;
    } else {
      throw new BlockException("JSObject cannot be cast into a Map");
    }
  }

  @Override
  public boolean isMapArray() {
    return (nashornObject instanceof Map
        && ((asMap().get("0") != null && asMap().get("0") instanceof Map)
            || asMap().isEmpty()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Map<String, Object>> asMapArray() {
    if (isMapArray()) {
      return (Map<String, Map<String, Object>>) nashornObject;
    } else {
      throw new BlockException("JSObject cannot be cast into a MapArray");
    }
  }

  @Override
  public boolean isIterator() {
    return (nashornObject instanceof Iterator);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<BlockObject> asIterator() {
    if (isIterator()) {
      return (Iterator<BlockObject>) nashornObject;
    } else {
      throw new BlockException("JSObject cannot be cast into an Iterator");
    }
  }

  @Override
  public String asString() {
    try {
      return (String) nashornObject;
    } catch (final Exception e) {
      throw new BlockException("JSObject cannot be cast into a String");
    }
  }

  @Override
  public boolean asBoolean() {
    try {
      return (boolean) nashornObject;
    } catch (final Exception e) {
      throw new BlockException("JSObject cannot be cast into a Boolean");
    }
  }

  @Override
  public ASTElement asAst() {
    if (nashornObject instanceof ASTElement) {
      return (ASTElement) nashornObject;
    } else {
      throw new BlockException("JSObject cannot be cast into an ASTElement");
    }
  }

  @Override
  public JSObject get(String field) {
    final Map<String, Object> map = asMap();
    return new JSObject(map.get(field));
  }

  @Override
  public void put(String field, Object o) {
    final Map<String, Object> map = asMap();
    map.put(field, o);
  }

  @Override
  public void put(String field, BlockObject o) {
    put(field, o.internalObject());
  }

  @Override
  @SuppressWarnings("restriction")
  public BlockObject call(Object... args) {
    final jdk.nashorn.api.scripting.JSObject nash = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    try {
      return new JSObject(nash.call(null, args));
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @SuppressWarnings("restriction")
  @Override
  public JSObject call(BlockObject... args) {
    final jdk.nashorn.api.scripting.JSObject nash = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    final Object[] nashargs = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      nashargs[i] = args[i].internalObject();
    }
    try {
      return new JSObject(nash.call(null, nashargs));
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @Override
  public boolean containsKey(String key) {
    return asMap().containsKey(key);
  }

  @Override
  public boolean checkpath(String[] path, int pos) {
    try {
      if (pos == path.length - 1) {
        return containsKey(path[pos]);
      } else {
        return get(path[pos]).checkpath(path, pos + 1);
      }
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public JSObject getpath(String[] path, int pos) {
    try {
      if (pos == path.length - 1) {
        return get(path[pos]);
      } else {
        return get(path[pos]).getpath(path, pos + 1);
      }
    } catch (final Exception e) {
      return null;
    }
  }

  @Override
  @SuppressWarnings("restriction")
  public long size() {
    final jdk.nashorn.api.scripting.JSObject nashornArray = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    try {
      final Number size = (Number) nashornArray.getMember("length");
      return size.longValue();
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @Override
  @SuppressWarnings("restriction")
  public JSObject get(int i) {
    final jdk.nashorn.api.scripting.JSObject nashornArray = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    try {
      return new JSObject(nashornArray.getSlot(i));
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @Override
  @SuppressWarnings("restriction")
  public void set(int i, BlockObject o) {
    final jdk.nashorn.api.scripting.JSObject nashornArray = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    try {
      nashornArray.setSlot(i, o.internalObject());
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @Override
  @SuppressWarnings("restriction")
  public void push(BlockObject o) {
    final jdk.nashorn.api.scripting.JSObject nashornArray = (jdk.nashorn.api.scripting.JSObject) nashornObject;
    try {
      final long length = size();
      nashornArray.setSlot((int) length, o.internalObject());
    } catch (final jdk.nashorn.api.scripting.NashornException e) {
      throw new BlockException(e);
    }
  }

  @Override
  public Iterator<BlockObject> asList() {
    return new Iterator<BlockObject>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public JSObject next() {
        return get(i++);
      }
    };
  }
}
