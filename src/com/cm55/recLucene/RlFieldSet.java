package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

public interface RlFieldSet<T> {
  public Set<String> getFieldNames();
  public RlField getFieldByName(String fieldName);
  public Stream<RlField> getFields();
}
