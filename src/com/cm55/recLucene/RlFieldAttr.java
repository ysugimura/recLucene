package com.cm55.recLucene;

import java.lang.annotation.*;

/**

 * @author ysugimura
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RlFieldAttr {
  
  /** 
   * プライマリキーを示す 。
   * {@link RlClassTable}の元となるクラスには、０か１個のプライマリキーフィールドが存在する。
   * プライマリキーの無い場合には、全く同じレコードが複数格納可能になることに注意。
   * プライマリキーは、必ずstore=trueになり、tokenized=falseになる。
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
   * tokenized=trueの場合に指定する必要がある。
   * デフォルトのRlAnalyzer.Default.classの場合、{@link RlDefaults}にて指定されたアナライザが使用される。
   * それ以外のアナライザを使用したいときは、そのアナライザクラスを指定する。
   * ただし、tokenized=falseの場合はここに何を指定しても無視される。
   * </p>
   * @return
   */
  public Class<? extends RlAnalyzer>analyzer() default RlAnalyzer.Default.class;

}
