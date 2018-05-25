package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

public class RlFieldMap {

  /** フィールド名/{@link RlField}マップ */
  private Map<String, RlField> fieldMap = new HashMap<>();
  
  /** プライマリキーフィールド 。プライマリキーの無い場合にはnull */
  private RlField pkField;
  
  public RlFieldMap(RlField... fields) {

    for (RlField field : fields) {

      // フィールド名重複チェック
      if (fieldMap.containsKey(field.getName())) {
        throw new RlException("フィールド名が重複しています:" + field.getName());
      }
      fieldMap.put(field.getName(), field);

      // プライマリキーの重複チェック
      if (field.isPk()) {

        // プライマリキーフィールドの重複チェック
        if (pkField != null) {
          throw new RlException("プライマリキー指定が複数あります");
        }
        pkField = field;
      }
    }
  }
  
  public RlField getPkField() {
    return pkField;
  }
  
  public Stream<RlField> getFields() {
    return fieldMap.values().stream();
  }
 
  public Stream<Map.Entry<String, RlField>>getEntries() {
    return fieldMap.entrySet().stream();
  }
  
  public Set<String> getFieldNames() {
    return new HashSet<String>(fieldMap.keySet());
  }
  
  public RlField getFieldByName(String fieldName) {
    return fieldMap.get(fieldName);
  }
  
  
}
