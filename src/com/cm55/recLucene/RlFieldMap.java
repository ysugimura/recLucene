package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

public class RlFieldMap implements RlFieldSet<RlValues> {

  /** フィールド名/{@link RlField}マップ */
  private Map<String, RlField> fieldMap = new HashMap<>();
  
  /** プライマリキーフィールド 。プライマリキーの無い場合にはnull */
  private RlField pkField;
  
  public RlFieldMap(Collection<RlField>fields) {
    this(fields.toArray(new RlField[0]));
  }
  
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
  
  public Document getDocument(RlValues values) {
    Document doc = new Document();
    getFields().forEach(field-> {
    
      Field lField = field.getLuceneField(values);
      if (lField == null)
        return; // 値がnullの場合はnullのフィールドが返る。登録しない。
      doc.add(lField);
    });
    return doc;
  }
  
  public RlValues fromDocument(Document doc) {
    RlValues result = new RlValues();

    getEntries().forEach(e-> {
      String fieldName = e.getKey();
      RlField field = e.getValue();
      field.setStringValue(result, doc.get(fieldName));
    });
    
    return result;
  }
  
  /**
   * 指定されたオブジェクトのプライマリキー{@link Term}を取得する。
   * @param rec
   * @return
   */
  public Term getPkTerm(RlValues values) {

    if (pkField == null)
      return null;
    

    String value = pkField.getStringValue(values);
    if (value == null) {
      throw new RlException("プライマリキーがnullです");
    }
    Term pkTerm = new Term(pkField.getName(), value);
    return pkTerm;
  }
  
}
