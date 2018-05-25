package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;

public interface RlTable<T> {
  public RlField<?> getPkField();
  public Set<String> getFieldNames();
  public RlField<?> getFieldByName(String fieldName);
  public Stream<RlField<?>> getFields();
  public T fromDocument(Document doc);
  public Document getDocument(T values);
  public Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers();
  
}
