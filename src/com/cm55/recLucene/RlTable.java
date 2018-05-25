package com.cm55.recLucene;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.lucene.document.*;
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
  private RlFieldMap fieldMap = new RlFieldMap();

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
    fieldMap = new RlFieldMap(fields);
  }

 public RlFieldMap getFieldMap() {
   return fieldMap;
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
    return fieldMap.getFields();
  }

  /**
   * プライマリキーフィールドを取得する。
   * 
   * @return 唯一のプライマリキーフィールド。無い場合はnull。
   */
  public RlField getPkField() {
    return fieldMap.getPkField();
  }

  /**
   * 全フィールド名称（static,transient以外のjavaフィールド名）の集合を取得する
   * 
   * @return 全フィールド名称の集合
   */
  public Set<String> getFieldNames() {
    return fieldMap.getFieldNames();
  }

  /**
   * フィールド名称から対応する{@link RlField}を取得する。存在しない場合はnullを返す。
   * 
   * @param fieldName フィールド名称
   * @return {@link RlField}
   */
  public RlField getFieldByName(String fieldName) {
    return fieldMap.getFieldByName(fieldName);
  }
  
  /**
   * 指定されたレコードオブジェクトのプライマリキー{@link Term}を取得する。
   */
  public Term getPkTerm(Object object) {
    RlField pkField = fieldMap.getPkField();
    if (pkField == null) return null;
    return fieldMap.getPkTerm(convertToValues(object));
  }

  /** レコードオブジェクトから{@link Document}オブジェクトを作成 */
  public <T>Document getDocument(T object) {
    return fieldMap.getDocument(convertToValues(object));
  }

  /** {@link Document}オブジェクトからレコードオブジェクトを作成 */
  @SuppressWarnings("unchecked")
  public <T>T fromDocument(Document doc) {
    return (T)convertFromValues(fieldMap.fromDocument(doc));    
  }

  /** レコードオブジェクトから{@link RlValue}オブジェクトを作成 */
  private RlValues convertToValues(Object o) {
    if (o instanceof RlValues) return (RlValues)o;
    RlValues result = new RlValues();
    fieldMap.getFields().forEach(f-> {
      try {
        result.put(f.getName(), f.getJavaField().get(o));
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    return result;
  }

  /** {@link RlValues}オブジェクトからレコードオブジェクトを作成 */
  @SuppressWarnings("unchecked")
  private<T> T convertFromValues(RlValues values) {
    if (recordClass == null)
      return (T) values;
    try {
      T object = (T) recordClass.newInstance();
      fieldMap.getFields().forEach(f -> {
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
