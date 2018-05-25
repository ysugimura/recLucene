package com.cm55.recLucene;

import org.apache.lucene.document.*;

/**
 * フィールド定義
 * @author ysugimura
 *
 * @param <T> フィールドの値の型
 */
public class RlField<T> {
  
  /** 対象とするJavaフィールド。Javaフィールドが対象でない場合はnull */
  private java.lang.reflect.Field javaField;

  /** 値の型。JavaFieldが存在する場合はjavaField.getType() */
  private Class<T> type;

  /** フィールドの名称 */
  private String name;

  /** 
   * このフィールドがプライマリキーフィールドであることを示す。
   * プライマリキーは一つの{@link RlTable}に０個か１個。
   * pk=trueの場合は、store=true、tokenized=falseに強制される。
   */
  private boolean pk;

  /** 
   * ストアするか
   * falseの場合には、値をLuceneデータベースに保存しない。インデックス情報だけが保存される。
   */
  private boolean store;

  /** 
   * トークン化するか
   * falseの場合には、そのままの値、trueの場合はAnalyzerによって分析された値がインデックス情報になる。
   */
  private boolean tokenized;

  /** 
   * フィールドコンバータ 。typeがString以外の場合に、type/Stringの相互変換を行う。
   * LuceneデータベースにはStringしか格納しないため、他のtypeの場合にはStringとの相互変換が必要
   */
  private RlFieldConverter<T> fieldConverter;

  /** 
   * analyzerクラス
   * tokenized=trueのときには必須
   */
  private Class<? extends RlAnalyzer> analyzerClass;

  private RlField() {    
  }
  
  /** 対応するJavaフィールドを取得する */
  public java.lang.reflect.Field getJavaField() {
    return javaField;
  }
  
  /**
   * フィールドのJavaタイプを取得する
   * 
   * @return フィールドのJavaタイプ
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * フィールド名称を取得する
   * 
   * @return フィールド名称
   */
  public String getName() {
    return name;
  }

  /**
   * プライマリーキーであるか
   * 
   * @return true:プライマリキー、false:プライマリキーでない。
   */

  public boolean isPk() {
    return pk;
  }

  /**
   * ストアするかを取得する
   * 
   * @return true:元データがLucene側にもストアされる。false:元データは保持されず、インデックスだけが Luceneに格納される。
   */
  public boolean isStore() {
    return store;
  }

  /**
   * トークン化されるか
   * 
   * @return true:トークン分割されてインデックスが作成される。false:トークン分割されず、元データが そのままインデックスデータになる。
   */
  public boolean isTokenized() {
    return tokenized;
  }

  /**
   * 値セットの中の、「この」フィールド値をLucene用のフィールドオブジェクトにして返す。 値がnullだった場合にはnullを返す。
   * 
   * @param object
   *          オブジェクト
   * @return Luecene用フィールド
   */  
  public Field getLuceneField(RlValues object) {
    String value = getStringValue(object);
    if (value == null) return null;
    if (!tokenized) {
      // トークン化されない場合、StringFieldを使用する
      return new StringField(name, value, store ? Field.Store.YES : Field.Store.NO);
    } else {
      // トークン化される場合、TextFieldを使用する
      return new TextField(name, value, store ? Field.Store.YES : Field.Store.NO);
    }
  }

  /**
   * フィールド値をLucene格納用のStringに変換する
   * @param value
   * @return
   */
  public String toString(T value) {
    if (value == null) return null;
    if (fieldConverter == null) return (String)value;
    return fieldConverter.toString(value);
  }

  /** Lucene格納用のStringからフィールド値を取得する */
  @SuppressWarnings("unchecked")
  public T fromString(String string) {
    if (string == null)
      return null;
    if (fieldConverter == null)
      return (T)string;
    return (T)fieldConverter.fromString(string);
  }

  /**
   * 値セットの中の、「このフィールド」の値を文字列として取得する。
   * @param values 値セット
   * @return 文字列として取得した「このフィールド」の値
   */
  public String getStringValue(RlValues values) {
    return toString(values.get(name));
  }
  
  /**
   * 指定されたオブジェクトの、このフィールドの値を設定する。 現在のところ値はString型のみをサポートしている。
   * @param values  オブジェクト
   * @param value  値
   */  
  public void setStringValue(RlValues values, String value) {
    values.put(name, fromString(value));
  }

  /** アナライザを取得する */
  private RlAnalyzer cachedAnalyzer;

  public RlAnalyzer getAnalyzer() {
    if (!tokenized) {
      throw new RlException("トークン化されないフィールドについてgetAnalyzer()が呼び出された");
    }
    if (cachedAnalyzer != null) {
      return cachedAnalyzer;
    }
    try {
      if (analyzerClass != null)
        return cachedAnalyzer = analyzerClass.newInstance();
      return cachedAnalyzer = RlDefaults.analyzerClass.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException();
    }
  }
  
  /**
   * 文字列化。デバッグ用
   */
  @Override
  public String toString() {
    return "java:" + (javaField == null ? "none" : javaField.toString()) + ",type:" + type.getName() + ",name:" + name
        + ",pk:" + pk + ",sto:" + store + ",tok:" + tokenized +  ",analy:"
        + (analyzerClass == null ? "none" : analyzerClass.getName());
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * {@link RlField}のビルダ
   * 
   * @param <T>
   */
  public static class Builder<T> {

    private java.lang.reflect.Field javaField;
    
    private final Class<T>type;
    
    private String name;
    private boolean pk = false;
    private boolean store = false;
    private boolean tokenized = true;
    private Class<? extends RlFieldConverter<T>>converter = null;
    private Class<? extends RlAnalyzer>analyzer = null;

    /** デフォルト値で作成する */
    public Builder(Class<T>type) {
      this.type = type;
    }

    /** 指定されたJavaフィールドの{@link RlFieldAttr}アノテーションから作成する */
    @SuppressWarnings("unchecked")
    public Builder(java.lang.reflect.Field javaField) {
      this.javaField = javaField;
      javaField.setAccessible(true);
      this.name = javaField.getName();
      this.type = (Class<T>) javaField.getType();
      //ystem.out.println("" + type);
      RlFieldAttr attr = javaField.getAnnotation(RlFieldAttr.class);
      if (attr != null) {
        this.pk = attr.pk();
        this.store = attr.store();
        this.tokenized = attr.tokenized();
        if (attr.converter() != RlFieldConverter.None.class)
          this.converter = (Class<? extends RlFieldConverter<T>>)attr.converter();
        if (attr.analyzer() != RlAnalyzer.Default.class)
          this.analyzer = attr.analyzer();
      }
    }
    
    public Builder<T> setConverter(Class<? extends RlFieldConverter<T>>converter) {
      this.converter = converter;
      return this;      
    }
    
    public Builder<T> setAnalyzer(Class<? extends RlAnalyzer>analyzer) {
      this.analyzer = analyzer;
      return this;
    }
            
    public Builder<T> setName(String name) {
      this.name = name;
      return this;
    }   
    
    public Builder<T> setPk(boolean value) {
      this.pk = value;
      return this;
    }
    
    public Builder<T> setTokenized(boolean value) {
      this.tokenized = value;
      return this;
    }
    
    public Builder<T>setStore(boolean value) {
      this.store = value;
      return this;
    }
        
    public RlField<T> build() {
      
      if (pk) {
        store = true;
        tokenized = false;
        analyzer = null;
      }

      // フィールド値コンバータを取得する
      RlFieldConverter<T> fieldConverter = null;
      if (converter != null) {
        try {
          fieldConverter = converter.newInstance();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }

        // コンバータのタイプと設定されているタイプが一致すること
        // ただし、このフィールドタイプがプリミティブの場合には参照型を考慮する。
        Class<?> converterType = fieldConverter.getType();
        Class<?> refType = null;
        if (type.isPrimitive()) {
          refType = Misc.getReferenceClass(type);
        }
        if (!converterType.equals(type) && !converterType.equals(refType)) {
          StringBuilder s = new StringBuilder();
          s.append("フィールドタイプがコンバータタイプと一致しません:" + type + "," + fieldConverter.getType() + "\n");
          if (javaField != null) {
            s.append(javaField.getDeclaringClass() + "#" + javaField.getName());
          }
          throw new RlException(s.toString());
        }
      }

      // 文字列以外の場合はコンバータが必須。
      if (type != String.class && fieldConverter == null) {
        StringBuilder s = new StringBuilder();
        s.append("フィールドがString以外の場合にはコンバータが必要：" + name + "\n");
        if (javaField != null) {
          s.append(javaField.getDeclaringClass() + "#" + javaField.getName());
        }
        throw new RlException(s.toString());       
      }

      RlField<T> f = new RlField<T>();
      f.javaField = javaField;
      f.type = type;
      f.name = name;
      f.pk = pk;
      f.store = store;
      f.tokenized = tokenized;
      f.fieldConverter = fieldConverter;
      f.analyzerClass = analyzer;
      return f;
    }
  }
}