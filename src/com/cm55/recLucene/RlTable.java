package com.cm55.recLucene;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;

/**
 * Luceneの{@link Document}とJavaクラスのマッピングオブジェクト
 * <p>
 * Luceneでの、いわゆるレコード、内部的には{@link Document}だが、これは、任意のフィールド名の任意の組み合わせ
 * のものがいつでも書き込めてしまう。
 * これでは扱いにくいので、ある程度の制限をかける。通常のORマッピングのように、Java側のクラスにあるフィールド名と
 * その値のみを{@link Document}として書き込む。
 * この目的のため、{@link RlTable}では、一つのJavaクラスを指定して、そのstatic/transientでないフィールドを
 * 拾い上げ書き込み・読み込み対象とする。
 * </p>
 * <p>
 * さらに、Luceneでは、そのままでは全く同じレコードを複数書き込んでしまうことができる。これを避けるため、Javaクラスの
 * フィールドにプライマリキー指定をつけ、常にその値が唯一になるように書き込みを行うようにさせる。必要なければ、
 * プライマリキーを非指定も可能、
 * </p>
 * <p>
 * 具体的なJavaクラス側のフィールド指定については{@link RlField}に詳細がある。
 * </p>
 * <p>
 * また、ごく稀にだが、動的にフィールドを決めたい場合もある。この場合にはJavaクラスを指定せずに、動的に作成した
 * {@link RlField}の配列を指定することもできる。
 * </p>
 * <p>
 * 注意点としては、このラッパーライブラリでは、一つのLuceneデータベース内において、一つのフィールド名は唯一でなければならないこと。
 * もともとLuceneデータベースでは、「任意のフィールドの任意の組み合わせ」が可能であるため、この制限が無いと、RDB的に言えば、
 * 「あちらのテーブルのフィールドと、こちらのテーブルのフィールドが一つのレコードとして格納されてしまいかねない」ためだ。
 * これについては、一つの{@link RlDatabase}に格納可能なテーブルのセット{@link RlTableSet}を参照のこと。
 * </p>
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
   * @param clazz マッピング対象クラス
   */
  public RlTable(Class<?> recordClass) {

    this.recordClass = recordClass;

    // このクラスで宣言されたすべてのフィールドを取得する。static/transientを除く
    List<RlField>fieldsFromClass = Arrays.stream(recordClass.getDeclaredFields())
      .filter(javaField-> {
        int mod = javaField.getModifiers();
        if (Modifier.isTransient(mod))
          return false;
        if (Modifier.isStatic(mod))
          return false;
        return true;
      })
      .map(javaField->new RlField(javaField))
      .collect(Collectors.toList());

    init(fieldsFromClass.toArray(new RlField[0]));
  }

  /**
   * フィールドを指定する
   * 
   * @param fields フィールド指定
   * @return
   */
  public RlTable(RlField... fields) {
    init(fields);
  }

  /**
   * フィールドリストを指定する
   * @param fields フィールド指定
   */
  public RlTable(List<RlField> fields) {
    init(fields.toArray(new RlField[0]));
  }

  /** 作成時の初期化 */
  private void init(RlField... fields) {
    
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
  public Stream<RlField> getFields() {
    return fieldMap.values().stream();
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
   * @param fieldName フィールド名称
   * @return {@link RlField}
   */
  public RlField getFieldByName(String fieldName) {
    return fieldMap.get(fieldName);
  }
  
  /**
   * 指定されたオブジェクトのプライマリキー{@link Term}を取得する。
   * @param rec
   * @return
   */
  public Term getPkTerm(Object object) {
    if (pkField == null)
      return null;
    
    RlValues values;
    if (recordClass == null) {
      // 自由形式
      if (object == null || !(object instanceof RlValues)) {
        throw new RlException("getPkTermの引数オブジェクトのクラスが違います");
      }
      values = (RlValues)object;
    } else {
      // レコード形式
      if (object == null || object.getClass() != recordClass) {
        throw new RlException("getPkTermの引数オブジェクトのクラスが違います");
      }
      values = this.convertToValues(object);
    }
    String value = pkField.getStringValue(values);
    if (value == null) {
      throw new RlException("プライマリキーがnullです");
    }
    Term pkTerm = new Term(pkField.getName(), value);
    return pkTerm;
  }

  public <T>Document getDocumentFromRecord(T object) {
    return getDocument(object);
  }
  
  public Document getDocumentFromValues(RlValues values) {
    return getDocument(values);
  }
  
  /**
   * 指定されたオブジェクトの内容から{@link Document}を作成する
   * 
   * @param object オブジェクト。クラスは{@link #recordClass}であること
   * @return luceneの{@link Document}
   */
  private <T> Document getDocument(T object) {
    RlValues values;
    if (recordClass == null) {
      // レコードクラスが無い場合。自由形式
      if (object == null || !(object instanceof RlValues)) {
        throw new RlException("getDocumentの引数オブジェクトのクラスが違います");
      }
      values = (RlValues)object;
    } else {
      // レコードクラスがある場合
      if (object == null || object.getClass() != recordClass) {
        throw new RlException("getDocumentの引数オブジェクトのクラスが違います");
      }
      values = this.convertToValues(object);
    }

    Document doc = new Document();
    for (RlField field : fieldMap.values()) {
      Field lField = field.getLuceneField(values);
      if (lField == null)
        continue; // 値がnullの場合はnullのフィールドが返る。登録しない。
      doc.add(lField);
    }
    return doc;
  }

  @SuppressWarnings("unchecked")
  public <T>T recordFromDocument(Document doc) {
    return (T)fromDocument(doc);    
  }

  public RlValues valuesFromDocument(Document doc) {
    return (RlValues)fromDocument(doc);
  }
  
  /**
   * Documentの内容からオブジェクトを作成する。自由形式の場合は{@link RlValues}を作成する
   * 
   * @param document
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> T fromDocument(Document doc) {
    RlValues result = new RlValues();

    for (Map.Entry<String, RlField> e : fieldMap.entrySet()) {
      String fieldName = e.getKey();
      RlField field = e.getValue();
      field.setStringValue(result, doc.get(fieldName));
    }
    
    if (recordClass == null) return (T)result;
    return this.convertFromValues(result);
  }
  
  public RlValues convertToValues(Object o) {
    if (o instanceof RlValues) return (RlValues)o;
    RlValues result = new RlValues();
    fieldMap.values().forEach(f-> {
      try {
        result.put(f.getName(), f.getJavaField().get(o));
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    return result;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T convertFromValues(RlValues values) {
    if (recordClass == null)
      return (T) values;
    try {
      T object = (T) recordClass.newInstance();
      fieldMap.values().forEach(f -> {
        try {
          f.getJavaField().set(object, values.get(f.getName()));
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
      return object;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
