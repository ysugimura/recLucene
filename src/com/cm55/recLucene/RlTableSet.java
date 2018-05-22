package com.cm55.recLucene;

import java.util.*;

import com.google.inject.*;

/**
 * テーブルの集合を扱う。
 * <p>
 * 通常のRDBとは異なり、異なるテーブル内に同じフィールド名が存在してはならない.
 * </p>
 * @author ysugimura
 */
@ImplementedBy(RlTableSet.Impl.class)
public interface RlTableSet {

  /** 
   * 指定クラスのマッピングを取得する。存在しなければnullを返す。
   * 自由形式の場合はレコードクラス自体が存在しないことに注意
   * @param clazz レコードクラス
   * @return {@link RlTable}
   */
  public <T> RlTable getTable(Class<T>clazz);

  /** 
   * フィールド名からLxFieldを取得する。存在しなければnullを返す。
   * {@link RlTableSet}中のすべてのテーブルのフィールドは一意名称であるため、フィールド名を
   * 指定すれば、テーブルが決まり、フィールドも決まる。\
   * @param fieldName
   * @return
   */
  public RlField getFieldByName(String fieldName);

  /**
   * テーブルコレクションを取得する
   * @return
   */
  public Collection<RlTable>getTables();
  
  /** {@link RlTableSet}のファクトリ */
  @Singleton
  public static class Factory {
    
    @Inject private Provider<RlTableSet.Impl>provider;
    @Inject private RlTable.Factory tableFactory;

    /**
     * 複数の指定されたクラスから{@link RlTableSet}を作成する
     * @param classes
     * @return
     */
    public RlTableSet create(Class<?>...classes) {
      RlTable[]tables = new RlTable[classes.length];
      for (int i = 0; i < tables.length; i++) {
        tables[i] = tableFactory.create(classes[i]);
      }
      return create(tables);
    }
    
    public RlTableSet create(RlTable...tables) {
      Impl impl = provider.get();
      impl.setup(tables);
      return impl;
    }
  }
  
   
  /**
   * {@link RlTableSet}の実装
   * @author ysugimura
   */
  public static class Impl implements RlTableSet {

    /** 全テーブル */
    private RlTable[]tables;
    
    /** フィールド名/テーブルマップ */
    private Map<String, RlTable>fieldToTable;
    
    /** レコードクラス/テーブルマップ */
    private Map<Class<?>, RlTable>recordToTable;

    public Impl() {
    }

    /**
     * クラス/{@link RlTable}マップを指定する
     * @param map
     */
    private void setup(RlTable...tables) {
      
      this.tables = tables;

      fieldToTable = new HashMap<String, RlTable>();
      recordToTable = new HashMap<Class<?>, RlTable>();

      for (RlTable table: tables) {
        
        // フィールド名/テーブル名マップを作成
        for (String fieldName: table.getFieldNames()) {
          if (fieldToTable.containsKey(fieldName)) {
            throw new RlException("フィールド名が重複しています：" + fieldName);
          }
          fieldToTable.put(fieldName,  table);
        }

        // レコードクラス/テーブルマップに格納する。ただしレコードクラスがあるもののみ。
        Class<?>recordClass = table.getRecordClass();
        if (recordClass != null) {
          if (recordToTable.containsKey(recordClass)) {
            throw new RlException("レコードクラスが重複しています:" + recordClass);
          }
          recordToTable.put(recordClass,  table);
        }        
      }      
    }

    /** {@inheritDoc} */
    @Override
    public <T>RlTable getTable(Class<T>clazz) {
      return recordToTable.get(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public RlField getFieldByName(String fieldName) {
      RlTable table = fieldToTable.get(fieldName);
      if (table == null) return null;
      return table.getFieldByName(fieldName);
    }
    
    /** {@inheritDoc} */
    @Override
    public Collection<RlTable>getTables() {
      return Arrays.asList(tables);
    }
  }
}
