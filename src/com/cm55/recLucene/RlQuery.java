package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * クエリを構築するインターフェース
 * <p>
 * LuceneのQueryとはまったく別の方法でクエリを構築し、最後にgetQuery() でLuceneのQueryを取り出す。
 * </p>
 * 
 * @author ysugimura
 */
public abstract class RlQuery {

  /**
   * LuceneのSearcherに与えるQueryを取得する。
   * @param table TODO
   * @return Lucene用のクエリオブジェクト
   */
  public abstract Query getLuceneQuery(RlTable table);
  
  @Override
  public boolean equals(Object o) {
    return checkEquals((RlQuery)o);
  }
  
  /**
   * オブジェクトクラスの同一性のチェック
   * @param that
   * @return
   */
  protected boolean checkEquals(RlQuery that) {
    if (!this.getClass().equals(that.getClass())) return false;

    return true;
  }
  
  /**
   * フィールド名を指定するクエリ
   * @author ysugimura
   */
  public static abstract class AbstractTerm extends RlQuery {

    /** フィールド名 */
    protected String fieldName;

    /** フィールド名を与えて初期化する */
    protected AbstractTerm(String fieldName) {
      if (fieldName == null) {
        throw new NullPointerException("field name or value is null");
      }
      this.fieldName = fieldName;
    }

    /** 上位のチェックに加え、フィールド名の同一性をチェックする */
    @Override
    protected boolean checkEquals(RlQuery that) {
      if (!super.checkEquals(that)) return false;
      AbstractTerm ft = (AbstractTerm)that;
      if (!this.fieldName.equals(ft.fieldName)) {
        return false;
      }
      return true;
    }
  }
  
  /**
   * フィールドに対する値が単一のクエリ
   * @author ysugimura
   */
  public static abstract class SingleValue extends AbstractTerm {

    protected  Object value;

    /** フィールド名と文字列を与えて初期化する */
    protected SingleValue(String fieldName, Object value) {
      super(fieldName);
      if (value == null) {
        throw new NullPointerException("field name or value is null");
      }
      this.fieldName = fieldName;
      this.value = value;
    }

    /**
     * 単一の値の同一性のチェック
     */
    @Override
    protected boolean checkEquals(RlQuery that) {
      if (!super.checkEquals(that)) return false;
      return this.value.equals(((SingleValue)that).value);
    }

    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return fieldName + "=" + value;
    }
  }

  /**
   * 抽象レンジクラス
   * @author ysugimura
   */
  public static abstract class AbstractRange extends AbstractTerm {

    /** 最小値 */
    protected Object min;
    
    /** 最大値 */
    protected Object max;
    
    /** 最小値を含む */
    protected boolean incMin;
    
    /** 最大値を含む */
    protected boolean incMax;
    
    protected AbstractRange(String fieldName, Object min, Object max, boolean incMin,
        boolean incMax) {
      super(fieldName);
      if (min == null || max == null) {
        throw new NullPointerException("field name or min/max value is null");
      }
      this.min = min;
      this.max = max;
      this.incMin = incMin;
      this.incMax = incMax;
    }

    /**
     * 上位の同一性チェックに加え、レンジ値等の同一性チェック
     */
    @Override
    protected boolean checkEquals(RlQuery o) {
      if (!super.checkEquals(o)) return false;
      AbstractRange that = (AbstractRange)o;
      return 
        this.min.equals(that.min) &&
        this.max.equals(that.max) &&
        this.incMin == that.incMin &&
        this.incMax == that.incMax;
    }
  }
  
  /** 
   * 完全一致クエリ
   * <p>
   * 指定された値は解析されず、かつフィールド値も解析されない。
   * つまり、フィールドはtokenized=falseでなければいけない。
   * </p>
   * @author ysugimura
   */
  public static class Match extends SingleValue  {

    public <T>Match(String fieldName, T value) {
      super(fieldName, value);
    }

    /** Lucene用Queryを取得する */
    @Override
    public Query getLuceneQuery(RlTable table) {    
      RlField field = table.getFieldByName(fieldName);
      if (field == null) throw new NullPointerException();
      checkValidity(field);
      return new TermQuery(new Term(fieldName, field.toString(value)));
    }

    private void checkValidity(RlField field) {
      if (field.isTokenized()) {
        throw new RlException("tokenized=trueのフィールドにMatchクエリは使用できません:" + field.getName());
      }
      
      
      // フィールドタイプがint型で、valueタイプがInteger型の場合がありうるので参照型に統一
      Class<?>fieldType = Misc.getReferenceClass(field.getType());
      Class<?>valueType = Misc.getReferenceClass(value.getClass());
      
      
      // このフィールドにvalue値を格納可能か
      if (!fieldType.isAssignableFrom(valueType)) {
        throw new RlException("Matchクエリの指定値が不適当です");
      }      
    }

    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "Match:" + super.toString();
    }
  }

  /** 前方検索クエリの実装。正規化はされるが解析はされない。 */
  public static class Prefix extends SingleValue {

    public Prefix(String fieldName, String value) {
      super(fieldName, value);
    }

    /** Lucene用Queryを取得する */
    @Override
    public Query getLuceneQuery(RlTable table) {
      return new PrefixQuery(new Term(fieldName, "" + value));
    }

    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "Prefix:" + super.toString();
    }
    
  }

  /** 文字列クエリの実装 */
  public static class Word extends SingleValue  {

    /** フィールド名と文字列を与えて初期化する */
    public Word(String fieldName, String value) {
      super(fieldName, value);
    }

    @Override
    public Query getLuceneQuery(RlTable table) {              
      RlField field = table.getFieldByName(fieldName);
      if (field == null) throw new RuntimeException();
      BooleanQuery bQuery = new BooleanQuery();
      for (String s: field.getAnalyzer().expandString(new StringReader("" + value))) {
        bQuery.add(
          new TermQuery(new Term(field.getName(), s)), BooleanClause.Occur.MUST);
      }
      return bQuery;
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "Word:" + super.toString();
    }
  }

  /** 範囲クエリ */
  public static class Range extends AbstractRange {
    
    /** フィールド名と文字列を与えて初期化する */
    public <T>Range(String fieldName, T min, T max, boolean incMin,
        boolean incMax) {
      super(fieldName, min, max, incMin, incMax);
    }
    
    public <T>Range(String fieldName, T min, T max) {
      super(fieldName, min, max, true, true);
    }
    
    @Override
    public Query getLuceneQuery(RlTable table) {   
      RlField field = table.getFieldByName(fieldName);
      if (field == null) throw new RuntimeException();
      checkValidity(field);
      Query query = TermRangeQuery.newStringRange(fieldName, 
          field.toString(min), field.toString(max), incMin, incMax);
      return query;
    }
    
    private void checkValidity(RlField field) {
      if (field.isTokenized()) {
        throw new RlException("tokenized=trueのフィールドにRangeクエリは使用できません:" + field.getName());
      }
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "Range:" + fieldName + "=" + min + "," + max + "," + incMin + "," + incMax;
    }
  }
  
  /** 複合クエリ抽象クラス */
  public abstract static class Compound<T extends Compound<T>>
      extends RlQuery {
    
    java.util.List<RlQuery> queryList = new ArrayList<RlQuery>();

    
    /** クエリを追加する */
    @SuppressWarnings("unchecked")
    public T add(RlQuery... queries) {
      queryList.addAll(Arrays.asList(queries));
      return (T) this;
    }

    /** {@inheritDoc} */
    public int count() {
      return queryList.size();
    }
    
    /** Lucene用のQueryを取得する */
    @Override
    public Query getLuceneQuery(RlTable table) {
      BooleanQuery booleanQuery = new BooleanQuery();
      for (RlQuery query : queryList) {
        booleanQuery.add(query.getLuceneQuery(table), getOccur());
      }
      return booleanQuery;
    }

    protected abstract BooleanClause.Occur getOccur();
    
    @Override
    protected boolean checkEquals(RlQuery o) {
      if (!super.checkEquals(o)) return false;
      Compound that = (Compound)o;
      if (this.queryList.size() != that.queryList.size()) return false;
      for (int i = 0; i < this.queryList.size(); i++) {
        if (!this.queryList.get(i).equals(that.queryList.get(i))) return false;
      }
      return true;
    }
    
    /** 
     * コンパクション
     * <p>
     * 内部リストに0または1しか格納されていない場合には、リストである必要性がないのでコンパクト化する。
     * </p>
     * @return null:何も格納されていないとき、RlQuery:一つのみの場合、
     * このCompound:複数格納されているとき。
     */
    public RlQuery compact() {
      switch (queryList.size()) {
      case 0: return null;
      case 1: return queryList.get(0);
      default: return this;
      }
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      if (queryList.size() == 0) return "";
      StringBuilder s = new StringBuilder();     
      for (RlQuery query: queryList) {
        s.append(",(" + query + ")");
      }      
      return s.toString().substring(1);
    }
  }

  /**
   * 複合ANDクエリ
   */
  public static class And extends Compound<And> {
    
    public And(RlQuery... queries) {
      add(queries);
    }
    
    @Override
    protected BooleanClause.Occur getOccur() {
      return BooleanClause.Occur.MUST;
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "And:" + super.toString();
    }
  }

  /**
   * 複合ORクエリ
   */
  public static class Or extends Compound<Or> {
    
    public Or(RlQuery... queries) {
      add(queries);
    }
    
    @Override
    protected BooleanClause.Occur getOccur() {
      return BooleanClause.Occur.SHOULD;
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "Or:" + super.toString();
    }
  }

  /**
   * 複合NOTクエリ
   */
  public static class Not extends Compound<Not> {

    public Not(RlQuery... queries) {
      add(queries);
    }
    
    @Override
    protected BooleanClause.Occur getOccur() {
      return BooleanClause.Occur.MUST_NOT;
    }

    /** クエリを取得する */
    @Override
    public Query getLuceneQuery(RlTable table) {
      BooleanQuery booleanQuery = new BooleanQuery();
      booleanQuery.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
      for (RlQuery query : queryList) {
        booleanQuery.add(query.getLuceneQuery(table), BooleanClause.Occur.MUST_NOT);
      }
      return booleanQuery;
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "not:" + super.toString();
    }
  }
}
