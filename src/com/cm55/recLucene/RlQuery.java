package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * クエリを構築するオブジェクト
 * <p>
 * LuceneのQueryとはまったく別の方法でクエリを構築し、{@link #getLuceneQuery(RlTable)}でLucene検索用のクエリを取得する
 * 
 * </p>
 * @author ysugimura
 */
public abstract class RlQuery {

  /**
   * LuceneのSearcherに与えるQueryを取得する。
   * @param fieldmap 対象とすうテーブル
   * @return Lucene用のクエリオブジェクト
   */
  public abstract <T> Query getLuceneQuery(RlTable<T> fieldmap);
  
  /**
   * フィールド名を指定するクエリ
   */
  public static abstract class AbstractTerm extends RlQuery {

    /** 対象とするフィールド名 */
    protected String fieldName;

    /** 対象とするフィールド名を与える */
    protected AbstractTerm(String fieldName) {
      if (fieldName == null) {
        throw new NullPointerException();
      }
      this.fieldName = fieldName;
    }

    /** 同一性チェック。フィールド名の照合のみ */
    @Override
    public boolean equals(Object o) {
      if (!getClass().isInstance(o)) return false;
      AbstractTerm ft = (AbstractTerm)o;
      return this.fieldName.equals(ft.fieldName);
    }
  }
  
  /**
   * フィールドに対する値が単一のクエリ
   */
  public static abstract class SingleValue extends AbstractTerm {

    /** 単一値 */
    protected Object value;

    /** フィールド名と文字列を与えて初期化する */
    protected SingleValue(String fieldName, Object value) {
      super(fieldName);
      if (value == null) throw new NullPointerException();      
      this.value = value;
    }

    /**
     * 単一の値の同一性のチェック
     */
    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) return false;
      return this.value.equals(((SingleValue)o).value);
    }

    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return fieldName + "=" + value;
    }
  }

  /**
   * 抽象レンジクラス。最小値、最大値を指定する
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
        throw new NullPointerException();
      }
      this.min = min;
      this.max = max;
      this.incMin = incMin;
      this.incMax = incMax;
    }

    /**
     * オブジェクト同一性のチェック
     */
    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) return false;
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
    public <T>Query getLuceneQuery(RlTable<T> fieldmap) {    
      @SuppressWarnings("unchecked")
      RlField<Object> field = (RlField<Object>)fieldmap.getFieldByName(fieldName);
      if (field == null) throw new NullPointerException();
      checkValidity(field);
      return new TermQuery(new Term(fieldName, field.toString(value)));
    }

    private void checkValidity(RlField<?> field) {
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
    public <T>Query getLuceneQuery(RlTable<T> fieldmap) {
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
    public <T> Query getLuceneQuery(RlTable<T> fieldmap) {              
      RlField<?> field = fieldmap.getFieldByName(fieldName);
      if (field == null) throw new RlException("field not found:" + fieldName);
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      for (String s: field.getAnalyzer().expandString(new StringReader("" + value))) {
        builder.add(
          new TermQuery(new Term(field.getName(), s)), BooleanClause.Occur.MUST);
      }
      return builder.build();
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
    public <T>Query getLuceneQuery(RlTable<T> fieldmap) {   
      @SuppressWarnings("unchecked")
      RlField<Object> field = (RlField<Object>)fieldmap.getFieldByName(fieldName);
      if (field == null) throw new RlException("field not found:" + fieldName);
      checkValidity(field);
      Query query = TermRangeQuery.newStringRange(fieldName, 
          field.toString(min), field.toString(max), incMin, incMax);
      return query;
    }
    
    private void checkValidity(RlField<?> field) {
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
  public abstract static class Compound<T extends Compound<T>> extends RlQuery {
    
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
    public <S> Query getLuceneQuery(RlTable<S> fieldmap) {
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      for (RlQuery query : queryList) {
        builder.add(query.getLuceneQuery(fieldmap), getOccur());
      }
      return builder.build();
    }

    protected abstract BooleanClause.Occur getOccur();
    
    @Override
    public boolean equals(Object o) {
      if (!getClass().isInstance(o)) return false;
      Compound<?> that = (Compound<?>)o;
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
    public <T>Query getLuceneQuery(RlTable<T> fieldmap) {
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      builder.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
      for (RlQuery query : queryList) {
        builder.add(query.getLuceneQuery(fieldmap), BooleanClause.Occur.MUST_NOT);
      }
      return builder.build();
    }
    
    /** 文字列化。デバッグ用 */
    @Override
    public String toString() {
      return "not:" + super.toString();
    }
  }
}
