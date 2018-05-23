package com.cm55.recLucene;

import java.lang.annotation.*;

/**
 * データベースに格納するテーブルを表現するクラスのフィールドの属性を定義する。
 * luceneデータベース上に格納するデータは、POJO(Plain Old Java Object)とする。つまり、
 * <pre>
 * public class Sample {
 *   public long id;
 *   public String name;
 *   public String addr;
 * }
 * </pre>
 * <p>
 * のようなものである。しかし、このluceneデータベースへの格納のされ方を制御する必要がある。
 * </p>
 * <ul>
 * <li>どのキーをプライマリキーとするのか、あるいはプライマリキーが必要無いのか。
 * <li>luceneデータベース上でも元のデータを保持するのか。
 * ※通常は、元データからインデックスデータを作成して、それを格納するが、元データ自体は格納されない。
 * <li>インデックスデータ作成の際にトークン化されるのか、つまり、元データをそのままインデックスとして使うのか、
 * あるいは文字列を適当に分割してその複数の文字列をインデックスとして使うのか。
 * <li>トークン化するとすれば、それを担うクラスは何か。
 * <li>文字列以外のデータ（int型など）を格納する際の相互変換オブジェクトの指定。
 * </ul>
 * 
 * @author ysugimura
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RlFieldAttr {
  
  /** 
   * プライマリキーを示す 。
   * {@link RlTable}の元となるクラスには、０か１個のプライマリキーフィールドが存在する。
   * プライマリキーの無い場合には、全く同じレコードが複数格納可能になることに注意。
   * @return true:このフィールドはプライマリキー、false:このフィールドはプライマリキーではない。
   */
  public boolean pk() default false;
  
  /**
   * 元データをLuceneデータベース上にストアするかを決定する。
   * <p>
   * 通常は元データをLucene側に保持しておく必要は無いので、デフォルトはfalse。
   * ただし、プライマリキーフィールドは自動的にstore=trueとなり、変更することはできない。
   * </p>
   * <p>
   * 元データを保持した場合、検索結果として得られるオブジェクトの該当フィールドに元のデータが格納される。
   * </p>
   * @return　true:元データを保持する。false:元データを保持しない。
   */
  public boolean store() default false;
  
  /**
   * トークン化される。
   * <p>
   * デフォルトはtrue。この場合はアナライザによって適切にトークン分割された後にLuceneのインデックスに
   * 格納される。falseの場合には、与えられたフィールド値自体（String以外の場合はString変換後）
   * がそのままインデックス値とｓｈちえ用いられる。
   * </p>
   * <p>
   * ただし、pkの場合は強制的にfalseとなり、変更することはできない。
   * </p>
   * <p>
   * 当然のことながら、tokenized=falseの場合にはanalyzerの指定は無効となる。
   * トークン化されないのでanalyzerを使用する必要が無い。
   * </p>
   */
  public boolean tokenized() default true;
  
  /**
   * フィールドコンバータ。
   * <p>
   * String以外の型のフィールドを使用する際に、String型との変換を行うコンバータ。
   * Luceneのフィールドには基本的に文字列しか格納することはできない。
   * これでは不便なので、フィールドとしてString以外を用いる場合には、その型とStringとの相互変換
   * を行うコンバータを提供する必要がある。
   * </p>
   * @return
   */
  public Class<? extends RlFieldConverter<?>>converter() default RlFieldConverter.None.class;
  
  /**
   * アナライザの指定
   * <p>
   * デフォルトのRlAnalyzer.Default.classの場合、{@link Defaults}にて指定されたアナライザが使用される。
   * それ以外のアナライザを使用したいときは、そのアナライザクラスを指定する。
   * ただし、tokenized=falseの場合はここに何を指定しても無視される。
   * </p>
   * @return
   */
  public Class<? extends RlAnalyzer>analyzer() default RlAnalyzer.Default.class;

  /**
   * デフォルトオブジェクトクラス
   * <p>
   * クラスフィールドのアノテーションを取得するのではなく、明示的にオブジェクトを作成する場合に使用する。
   * </p>
   * @author ysugimura
   */
  public static class Default implements RlFieldAttr {
    
    private boolean pk = false;
    private boolean store = false;
    private boolean tokenized = true;
    private Class<? extends RlFieldConverter<?>>converter
         = RlFieldConverter.None.class;
    private Class<? extends RlAnalyzer>analyzer = RlAnalyzer.Default.class;
    
    public Class<? extends Annotation> annotationType() {
      return RlFieldAttr.class;
    }

    public Default setPk(boolean pk) {
      this.pk = pk;
      return this;
    }
    public Default setStore(boolean store) {
      this.store = store;
      return this;
    }
    public Default setTokenized(boolean tokenized) {
      this.tokenized = tokenized;
      return this;
    }
    public Default setConverter(Class<? extends RlFieldConverter<?>> converter) {
      this.converter = converter;
      return this;
    }
    public Default setAnalyzer(Class<? extends RlAnalyzer> analyzer) {
      this.analyzer = analyzer;
      return this;
    }    
    
    public boolean pk() {  return pk; }
    public boolean store() { return store; }
    public boolean tokenized() { return tokenized; }
    
    public Class<? extends RlFieldConverter<?>> converter() {
      return converter;
    }
    
    public Class<? extends RlAnalyzer> analyzer() {
      return analyzer;
    }    
  }
}
