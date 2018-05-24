package com.cm55.recLucene;

import java.util.*;

class Misc {

  static Map<Class<?>, Class<?>>map = new HashMap<Class<?>, Class<?>>() {{
    put(byte.class, Byte.class);
    put(short.class, Short.class);
    put(int.class, Integer.class);
    put(long.class, Long.class);
    put(char.class, Character.class);
    put(float.class, Float.class);
    put(double.class, Double.class);
    put(boolean.class, Boolean.class);
  }};
  
  /**
   * 指定されたプリミティブ型に対応するオブジェクト型を取得する
   * @param input
   * @return
   */
  static Class<?>getReferenceClass(Class<?>input) {
    if (!input.isPrimitive()) return input;
    Class<?>clazz = map.get(input);
    if (clazz == null) throw new InternalError();
    return clazz;
  }
}
