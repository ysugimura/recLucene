package com.cm55.recLucene;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

/**
 * Javaクラスから作成するテーブル定義
 */
public class RlClassTable<T> implements RlTable<T> {

  /** 対象とするレコードクラス */
  private final Class<T> recordClass;

  /** フィールド名/{@link RlField}マップ */
  private final RlAnyTable anyTable;

  /**
   * クラスを指定してマッピングを作成する
   * @param recordClass マッピング対象クラス
   */
  public RlClassTable(Class<T> recordClass) {

    this.recordClass = recordClass;

    // このクラスで宣言されたすべてのフィールドを取得する。static/transientを除く
    @SuppressWarnings("rawtypes")
    List<RlField<?>>fieldsFromClass = Arrays.stream(recordClass.getDeclaredFields())
      .filter(javaField-> {
        int mod = javaField.getModifiers();
        if (Modifier.isTransient(mod))
          return false;
        if (Modifier.isStatic(mod))
          return false;
        return true;
      })
      .map(javaField->(RlField<?>)new RlField.Builder(javaField).build())
      .collect(Collectors.toList());

    anyTable = new RlAnyTable(fieldsFromClass);
  }
  
  public Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers() {
    return anyTable.getFieldAnalyzers();
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
  @Override
  public Stream<RlField<?>> getFields() {
    return anyTable.getFields();
  }

  /**
   * プライマリキーフィールドを取得する。
   * 
   * @return 唯一のプライマリキーフィールド。無い場合はnull。
   */
  @Override
  public RlField<?> getPkField() {
    return anyTable.getPkField();
  }

  /**
   * 全フィールド名称（static,transient以外のjavaフィールド名）の集合を取得する
   * 
   * @return 全フィールド名称の集合
   */
  @Override
  public Set<String> getFieldNames() {
    return anyTable.getFieldNames();
  }

  /**
   * フィールド名称から対応する{@link RlField}を取得する。存在しない場合はnullを返す。
   * 
   * @param fieldName フィールド名称
   * @return {@link RlField}
   */
  @Override
  public RlField<?> getFieldByName(String fieldName) {
    return anyTable.getFieldByName(fieldName);
  }
  
  /**
   * 指定されたレコードオブジェクトのプライマリキー{@link Term}を取得する。
   */
  public Term getPkTerm(Object object) {
    RlField<?> pkField = anyTable.getPkField();
    if (pkField == null) return null;
    return anyTable.getPkTerm(convertToValues(object));
  }

  /** レコードオブジェクトから{@link Document}オブジェクトを作成 */
  @Override
  public Document getDocument(T object) {
    return anyTable.getDocument(convertToValues(object));
  }

  /** {@link Document}オブジェクトからレコードオブジェクトを作成 */
  @Override
  public T fromDocument(Document doc) {
    return (T)convertFromValues(anyTable.fromDocument(doc));    
  }

  /** レコードオブジェクトから{@link RlValue}オブジェクトを作成 */
  private RlValues convertToValues(Object o) {
    if (o instanceof RlValues) return (RlValues)o;
    RlValues result = new RlValues();
    anyTable.getFields().forEach(f-> {
      try {
        result.put(f.getName(), f.getJavaField().get(o));
      } catch (Exception ex) {
        throw new RlException(ex);
      }
    });
    return result;
  }

  /** {@link RlValues}オブジェクトからレコードオブジェクトを作成 */
  @SuppressWarnings("unchecked")
  private T convertFromValues(RlValues values) {
    if (recordClass == null)
      return (T) values;
    try {
      T object = (T) recordClass.newInstance();
      anyTable.getFields().forEach(f -> {
        try {
          f.getJavaField().set(object, values.get(f.getName()));
        } catch (Exception ex) {
          throw new RlException(ex);
        }
      });
      return object;
    } catch (Exception ex) {
      throw new RlException(ex);
    }
  }
  
  /** テーブル名称を取得する */
  @Override
  public String getTableName() {
    return recordClass.getName();
  }
}
