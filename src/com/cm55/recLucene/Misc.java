package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.util.*;

public class Misc {

  public static boolean equals(Object a, Object b) {
    if (a == null)
      return b == null;
    return a.equals(b);
  }

  public static String[] getExpanded(Tokenizer tokenizer) {
    try {
      List<String> list = new ArrayList<String>();

      while (tokenizer.incrementToken()) {
        Iterator<AttributeImpl> it = tokenizer.getAttributeImplsIterator();
        while (it.hasNext()) {
          AttributeImpl o = it.next();
          list.add(o.toString());
        }
      }
      return list.toArray(new String[0]);
    } catch (IOException ex) {
      throw new RlException(ex);
    }
  }
  
  public static Class<?>getReferenceClass(Class<?>input) {
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
