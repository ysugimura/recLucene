package com.cm55.recLucene;



import org.apache.lucene.document.*;

import com.google.inject.*;

/**
 * フィールド定義
 * <p>
 * luceneデータベースのレコードにマッピングされるJavaオブジェクトの一つのフィールドを表現する。
 * </p>
 * @author ysugimura
 */
public interface RlField {

  /**
   * フィールドのJavaタイプを取得する
   * @return フィールドのJavaタイプ
   */
  public Class<?>getType();
  
  /** 
   * フィールド名称を取得する
   * @return フィールド名称
   */
  public String getName();
  
  /** 
   * このフィールドの所属するテーブルを返す。
   * @return　このフィールドの所属する{@link RlTable}
   */
  public RlTable getTable();
  
  /** 
   * プライマリーキーであるか
   * @return　true:プライマリキー、false:プライマリキーでない。
   */
  public boolean isPk();

  /** 
   * ストアするかを取得する
   * @return true:元データがLucene側にもストアされる。false:元データは保持されず、インデックスだけが
   * Luceneに格納される。
   */
  public boolean isStore();
  
  /** 
   * トークン化されるか
   * @return true:トークン分割されてインデックスが作成される。false:トークン分割されず、元データが
   * そのままインデックスデータになる。
   */
  public boolean isTokenized();

  /** 
   * 指定されたオブジェクトの中の、このフィールドの値をLucene用のフィールドオブジェクトにして返す。
   * 値がnullだった場合にはnullを返す。
   * 
   * @param object オブジェクト
   * @return Luecene用フィールド
   */
  public <T>Field getLuceneField(T object);
  
  /**
   * 指定オブジェクト中の、「このフィールド」の値を取得する
   * @param object オブジェクト
   * @return フィールド値
   */
  public <T,V>V getValue(T object);

  /**
   * 指定オブジェクトにある「このフィールド」の値を設定する
   * @param object オブジェクト
   * @param value 設定する値
   */
  public <T,V>void setValue(T object, V value);
  
  /** 
   * フィールド値をLucene格納用のStringに変換する
   * @param value
   * @return
   */
  public <V>String toString(V value);

  /** Lucene格納用のStringからフィールド値を取得する */
  public <V>V fromString(String string);
  
  /**
   * 指定されたオブジェクトの中の、「このフィールド」の値を取得する。
   * @param object 
   * @return
   */
  public <T>String getStringValue(T object);

  /**
   * 指定されたオブジェクトの、このフィールドの値を設定する。
   * 現在のところ値はString型のみをサポートしている。
   * @param object オブジェクト
   * @param value 値
   */
  public <T>void setStringValue(T object, String value);

  /** アナライザを取得する */
  public RlAnalyzer getAnalyzer();

  /** 
   * {@link RlField}のファクトリ
   * @author ysugimura
   */
  @Singleton
  public static class Factory {
    
    @Inject private Provider<Impl> provider;

    /**
     * Javaフィールドから{@link RlField}を作成する
     * @param field
     * @return
     */
    public RlField create(java.lang.reflect.Field field) {
      return provider.get().setup(field);
    }
    
    /**
     * 自由形式の{@link RlField}を作成する
     * @param fieldName フィールド名称
     * @param type オブジェクトの型
     * @param fieldAttr フィールド属性。nullでもよい。
     */
    public RlField create(String fieldName,  RlFieldAttr fieldAttr) {
      return provider.get().setup(fieldName, fieldAttr);
    }
  }

  /**
   * {@link RlField}の実装
   * 
   * @author ysugimura
   */
  public static class Impl implements RlField {

    @Inject private Injector injector;
    
    /** javaフィールド。自由形式の場合はnull */
    private java.lang.reflect.Field javaField;

    /** 値の型 */
    private Class<?>type;
    
    /** フィールドの名称 */
    private String name;

    /** このフィールドがプライマリキーフィールドであることを示す */
    private boolean isPk;

    /** ストアするか */
    private boolean store;

    /** トークン化するか */
    private boolean tokenized;

    /** フィールドコンバータ クラス*/
    private Class<? extends RlFieldConverter<?>>converterClass;
    
    /** コンバータ　*/
    private RlFieldConverter fieldConverter;
    
    /** analyzerクラス */
    private Class<? extends RlAnalyzer>analyzerClass;

    /**
     * 文字列化。デバッグ用
     */
    @Override
    public String toString() {
      return 
          "java:" + (javaField == null? "none":javaField.toString()) + 
          ",type:" + type.getName() + 
          ",name:" + name + 
          ",pk:" + isPk + 
          ",sto:" + store + 
          ",tok:" + tokenized + 
          ",conv:" + (converterClass == null? "none":converterClass.getName())+ 
          ",analy:" + (analyzerClass == null? "none": analyzerClass.getName());
    }
    
    /** 
     * Javaのフィールドを指定して作成する。
     * 名称はJavadフィールド名称、タイプはJavaフィールドタイプ、フィールド属性はアノテーションから取得する。
     * 
     * @param javaField 対象とするJavaフィールド
     * @return
     */
    Impl setup(java.lang.reflect.Field javaField) {

      // Javaのフィールドを取得してaccessibleにしておく
      this.javaField = javaField;
      javaField.setAccessible(true);

      this.name = javaField.getName();
      this.type = javaField.getType();
      
      setupFieldAttr(javaField.getAnnotation(RlFieldAttr.class));
      
      checkConverter();
      return this;
    }
    
    /**
     * フィールド名称、{@link RlFieldAttr}オブジェクトを指定して作成する。
     * タイプはコンバータから取得する。
     * 
     * @param fieldName
     * @param fieldAttr
     * @return
     */
    Impl setup(String fieldName, RlFieldAttr fieldAttr) {      
      this.name = fieldName;            
      type = String.class;
      setupFieldAttr(fieldAttr);
      if (fieldConverter != null) type = fieldConverter.getType();      
      checkConverter();      
      return this;
    }
    
    private void checkConverter() {
      // 文字列の場合はコンバータ無しでよい
      if (type == String.class) return;
      
      // 文字列以外の場合はコンバータが必須。
      if (converterClass == null) {
        throw new RlException("フィールドがString以外の場合にはコンバータが必要：" + name);
      }

      // コンバータのタイプと設定されているタイプが一致すること
      // ただし、このフィールドタイプがプリミティブの場合には参照型を考慮する。
      Class<?>converterType = fieldConverter.getType();
      Class<?>refType = null;
      if (type.isPrimitive()) {
        refType = Misc.getReferenceClass(type);
      }
      if (!converterType.equals(type) && !converterType.equals(refType)) { 
        throw new RlException("フィールドタイプがコンバータタイプと一致しません:" + type + "," + fieldConverter.getType());
      }
    }
    
    private void setupFieldAttr(RlFieldAttr fieldAttr) {
      if (fieldAttr == null) {
        setupWithoutAttr();
      } else {
        setupWithAttr(fieldAttr);
      }
    }
    
    /**
     * {@link RlFieldAttr}アノテーションの無いときのセットアップ
     */
    private void setupWithoutAttr() {
      isPk = false;
      store = false;
      tokenized = true;    
      analyzerClass = null;
      converterClass = null;
      fieldConverter = null;
    }

    /**
     * {@link RlFieldAttr}アノテーションのあるときのセットアップ
     * @param fieldAttr
     */
    private void setupWithAttr(RlFieldAttr fieldAttr) {
      
      // フラグを取得
      isPk = fieldAttr.pk();
      if (isPk) {
        // プライマリキーの場合にはフラグを無視する
        store = true;
        tokenized = false;
        
        // プライマリキーの場合にはアナライザ無し
        analyzerClass = null;
        
      } else {
        // プライマリキーでない場合
        store = fieldAttr.store();
        tokenized = fieldAttr.tokenized();
        
        // デフォルトが指定されていたらnullにする。それ以外はそのまま格納
        analyzerClass = fieldAttr.analyzer();
        if (analyzerClass == RlAnalyzer.Default.class) {
          analyzerClass = null;
        }
      } 
      
      // フィールド値コンバータを取得する
      converterClass = fieldAttr.converter();
      if (converterClass == RlFieldConverter.None.class) {
        converterClass = null;
      }
      if (converterClass != null) {
        fieldConverter = injector.getInstance(converterClass);
      }
    }

    /** {@inheritDoc} */
    @Override
    public Class<?>getType() {
      return type;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isPk() {
      return isPk;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isStore() {
      return store;
    }

    /** {@inheritDoc} */
    public boolean isTokenized() {
      return tokenized;
    }
    
    /** {@inheritDoc} */
    @Override
    public RlTable getTable() {
      throw new RuntimeException();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T,V>V getValue(T object) {      
      // 通常のオブジェクトの場合
      if (javaField != null) {
        if (object instanceof RlValues) 
          throw new RlException("不適当なオブジェクトです");
        try {
          return (V)this.javaField.get(object);
        } catch (IllegalAccessException ex) {
          throw new RlException(ex);
        }
      }
      
      // 自由形式の場合
      if (!(object instanceof RlValues)) 
        throw new RlException("不適当なオブジェクトです");
      return (V)((RlValues)object).get(name);
    }
    
    /** {@inheritDoc} */
    @Override    
    @SuppressWarnings("unchecked")
    public <T>String getStringValue(T object) {
      return toString(getValue(object));
    }

    /** {@inheritDoc} */
    @Override
    public <T,V>void setValue(T object, V value) {
      // 通常のオブジェクトの場合      
      if (javaField != null) {
        if (object instanceof RlValues) 
          throw new RlException("不適当なオブジェクトです");
        try {
          javaField.set(object, value);
          return;
        } catch (Exception ex) {
          throw new RlException(ex);
        }
      }
      
      // 自由形式の場合
      if (!(object instanceof RlValues)) {
        throw new RlException("不適当なオブジェクトです");
      }
      ((RlValues)object).put(name,  value);
    }
    
    /** {@inheritDoc} */
    @Override
    public <T>void setStringValue(T object, String value) {      
      setValue(object, fromString(value));
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <V>String toString(V value) {
      if (value == null) return null;
      if (getFieldConverter() == null) return (String)value;
      return getFieldConverter().toString(value);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <V>V fromString(String string) {
      if (string == null) return null;
      if (getFieldConverter() == null) return (V)string;
      return (V)getFieldConverter().fromString(string);
    }
    
    /** {@inheritDoc} */
    @Override
    public <T>Field getLuceneField(T object) {
      String value = getStringValue(object);
      if (value == null) return null;
      if (!tokenized) {
        // トークン化されない場合、StringFieldを使用する
        return new StringField(name, value, 
            store?Field.Store.YES:Field.Store.NO);
      } else {
        // トークン化される場合、TextFieldを使用する
        return new TextField(name, value,
            store?Field.Store.YES:Field.Store.NO);
      }
    }
    
    /** フィールド名を取得する */
    @Override
    public String getName() {
      return name;
    }

    
    /** フィールドコンバータを取得する */
    private RlFieldConverter getFieldConverter() {
      return fieldConverter;
    }
    
    @Inject private Defaults defaults;
    private RlAnalyzer cachedAnalyzer;
    
    @Override
    public RlAnalyzer getAnalyzer() {
      if (!tokenized) {
        throw new RlException("トークン化されないフィールドについてgetAnalyzer()が呼び出された");
      }
      if (cachedAnalyzer != null) {
        return cachedAnalyzer;
      }
      if (analyzerClass != null) 
        return cachedAnalyzer = this.injector.getInstance(analyzerClass);
      return cachedAnalyzer = injector.getInstance(Defaults.analyzerClass);
    }
  }
}