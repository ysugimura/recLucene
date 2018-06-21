package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

public class RlAnyTable implements RlTable<RlValues> {

  /** フィールド名/{@link RlField}マップ */
  private final Map<String, RlField<?>> fieldMap = new HashMap<>();
  
  /** プライマリキーフィールド 。プライマリキーの無い場合にはnull */
  private final RlField<?> pkField;
  
  private final Map<String, Analyzer>fieldAnalyzers;
  
  
  public RlAnyTable(Collection<RlField<?>>fields) {
    this(fields.toArray(new RlField[0]));
  }
  
  public RlAnyTable(RlField<?>... fields) {

    RlField<?> _pkField = null;
    
    for (RlField<?> field : fields) {

      // フィールド名重複チェック
      if (fieldMap.containsKey(field.getName())) {
        throw new RlException("フィールド名が重複しています:" + field.getName());
      }
      fieldMap.put(field.getName(), field);

      // プライマリキーの重複チェック
      if (field.isPk()) {

        // プライマリキーフィールドの重複チェック
        if (_pkField != null) {
          throw new RlException("プライマリキー指定が複数あります");
        }
        _pkField = field;
      }
    }
    
    pkField = _pkField;
    fieldAnalyzers = createFieldAnalyzers(fieldMap.values());
  }
  
  public Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers() {
    return fieldAnalyzers.entrySet().stream();
  }
  
  public RlField<?> getPkField() {
    return pkField;
  }
  
  @Override
  public Stream<RlField<?>> getFields() {
    return fieldMap.values().stream();
  }
 
  public Stream<Map.Entry<String, RlField<?>>>getEntries() {
    return fieldMap.entrySet().stream();
  }
  
  @Override
  public Set<String> getFieldNames() {
    return new HashSet<String>(fieldMap.keySet());
  }
  
  @Override
  public RlField<?> getFieldByName(String fieldName) {
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
      RlField<?> field = e.getValue();
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
  
  /** {@link RlClassTable}からフィールド名/{@link Analyzer}のマップエントリストリームを作成する */
  static Map<String, Analyzer>createFieldAnalyzers(Collection<RlField<?>>fields) {
    return fields.stream()
      .filter(f->f.isTokenized())
      .collect(Collectors.toMap(
        f->f.getName(),
        f->(Analyzer)new Analyzer() {
          protected TokenStreamComponents createComponents(String fieldName) {                  
            return f.getAnalyzer().createComponents();
          }
          @Override
          public String toString() {
            return "Analyzer";
          }
        }
      ));
  }
  
  /** テーブル名称を取得する */
  @Override
  public String getTableName() {
    return RlAnyTable.class.getSimpleName() + 
      this.fieldMap.keySet().stream().collect(Collectors.joining(","));
  }
}
