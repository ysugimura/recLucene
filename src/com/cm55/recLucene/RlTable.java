package com.cm55.recLucene;

import java.lang.reflect.*;
import java.util.*;

import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;

/**
 * アノテーション付きのJavaクラスを元にテーブルが作成される。
 * <ul>
 * <li>すべてのフィールドはString型のみとする。
 * <li>Javaの直列化と同様にstatic,transientは無視される。
 * <li>ただ一つのプライマリキーフィールドが存在しなくてはならない。これは{@link RlFieldAttr}で指定される。
 * </ul>
 * 
 * @author ysugimura
 */
public class RlTable {

  /** 対象とするレコードクラス。ただし、自由フィールドの場合はnull */
  private Class<?> recordClass;

  /** フィールド名/{@link RlField}マップ */
  private Map<String, RlField> fieldMap;

  /** プライマリキーフィールド 。プライマリキーの無い場合にはnull */
  private RlField pkField;

  /**
   * クラスを指定してマッピングを作成する
   * 
   * @param clazz
   *          マッピング対象クラス
   */
  public RlTable(Class<?> recordClass) {

    this.recordClass = recordClass;

    List<RlField> fieldsFromClass = new ArrayList<RlField>();

    // このクラスで宣言されたすべてのフィールドを調査する。ただし、static/transientは除く
    Arrays.stream(recordClass.getDeclaredFields()).forEach(javaField -> {

      // フィールドモディファイアの調査。transient,staticを無視する。
      {
        int mod = javaField.getModifiers();
        if (Modifier.isTransient(mod))
          return;
        if (Modifier.isStatic(mod))
          return;
      }

      // このフィールドについてのRlFieldを作成して追加
      fieldsFromClass.add(new RlField(javaField));
    });

    init(fieldsFromClass.toArray(new RlField[0]));
  }

  /**
   * フィールド配列を内部に格納する。プライマリキー重複をチェックする
   * 
   * @param fields
   * @return
   */
  public RlTable(RlField... fields) {
    init(fields);
  }

  public RlTable(List<RlField> fields) {
    init(fields.toArray(new RlField[0]));
  }

  private void init(RlField... fields) {
    // フィールド配列を内部に格納する
    fieldMap = new HashMap<String, RlField>();
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

    // プライマリキーが指定されていなければならない。
    // ※指定がなくてもいいことにした
    /*
     * if (pkField == null) { throw new RlException("プライマリキー指定がありません"); }
     */
  }

  /**
   * レコードクラスを取得する。自由形式の場合はnullが返る。
   */
  public Class<?> getRecordClass() {
    return recordClass;
  }

  /**
   * すべてのフィールドを取得する
   */
  public Collection<RlField> getFields() {
    return fieldMap.values();
  }

  /**
   * プライマリキーフィールドを取得する。
   * 
   * @return 唯一のプライマリキーフィールド。無い場合はnull。
   */
  public RlField getPkField() {
    return pkField;
  }

  /**
   * 全フィールド名称（static,transient以外のjavaフィールド名）の集合を取得する
   * 
   * @return 全フィールド名称の集合
   */
  public Set<String> getFieldNames() {
    return new HashSet<String>(fieldMap.keySet());
  }

  /**
   * フィールド名称から対応する{@link RlField}を取得する。存在しない場合はnullを返す。
   * 
   * @param fieldName
   *          フィールド名称
   * @return {@link RlField}
   */
  public RlField getFieldByName(String fieldName) {
    return fieldMap.get(fieldName);
  }

  /**
   * 指定されたオブジェクトのプライマリキー{@link Term}を取得する
   * 
   * @param rec
   * @return
   */
  public Term getPkTerm(Object object) {
    if (pkField == null)
      return null;
    if (recordClass == null) {
      // 自由形式
      if (object == null || !(object instanceof RlValues)) {
        throw new RlException("getPkTermの引数オブジェクトのクラスが違います");
      }
    } else {
      // レコード形式
      if (object == null || object.getClass() != recordClass) {
        throw new RlException("getPkTermの引数オブジェクトのクラスが違います");
      }
    }
    String value = pkField.getStringValue(object);
    if (value == null) {
      throw new RlException("プライマリキーがnullです");
    }
    Term pkTerm = new Term(pkField.getName(), value);
    return pkTerm;
  }

  /**
   * 指定されたオブジェクトの内容から{@link Document}を作成する
   * 
   * @param object
   *          オブジェクト。クラスは{@link #recordClass}であること
   * @return luceneの{@link Document}
   */
  public <T> Document getDocument(T object) {
    if (recordClass == null) {
      // レコードクラスが無い場合。自由形式
      if (object == null || !(object instanceof RlValues)) {
        throw new RlException("getDocumentの引数オブジェクトのクラスが違います");
      }
    } else {
      // レコードクラスがある場合
      if (object == null || object.getClass() != recordClass) {
        throw new RlException("getDocumentの引数オブジェクトのクラスが違います");
      }
    }

    Document doc = new Document();
    for (RlField field : fieldMap.values()) {
      Field lField = field.getLuceneField(object);
      if (lField == null)
        continue; // 値がnullの場合はnullのフィールドが返る。登録しない。
      doc.add(lField);
    }
    return doc;
  }

  /**
   * Documentの内容からオブジェクトを作成する。自由形式の場合は{@link RlValues}を作成する
   * 
   * @param document
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> T fromDocument(Document doc) {
    Object result;
    if (recordClass == null) {
      // レコードクラスが無い場合、自由形式
      result = new RlValues();
    } else {
      // レコードクラスがある場合
      try {
        result = recordClass.newInstance();
      } catch (Exception ex) {
        throw new RlException(recordClass + "のインスタンスを作成できません:" + ex.getMessage());
      }
    }
    for (Map.Entry<String, RlField> e : fieldMap.entrySet()) {
      String fieldName = e.getKey();
      RlField field = e.getValue();
      field.setStringValue(result, doc.get(fieldName));
    }
    return (T) result;
  }
}
