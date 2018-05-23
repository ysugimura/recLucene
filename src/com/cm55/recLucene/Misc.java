package com.cm55.recLucene;

class Misc {

  static Class<?>getReferenceClass(Class<?>input) {
    if (!input.isPrimitive()) return input;
    if (input == byte.class) return Byte.class; 
    if (input == short.class) return Short.class;
    if (input == int.class) return Integer.class; 
    if (input == long.class) return Long.class;
    if (input == char.class) return Character.class;
    if (input == float.class) return Float.class; 
    if (input == double.class) return Double.class;
    if (input == boolean.class) return Boolean.class;
    throw new RuntimeException("not supported");
  }
}
