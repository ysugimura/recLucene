package com.cm55.recLucene;

/**
 * {@link RlFieldAttr#converter()}に指定されるクラス
 * <p>
 * 
 * </p>
 * @author ysugimura
 *
 * @param <T>
 */
public interface RlFieldConverter<T> {

  /** コンバート対象の型 */
  public Class<T>getType();
  
  /** 文字列へコンバート */
  public String toString(T value);

  /** 文字列からコンバート */
  public T fromString(String string);

  public static abstract class Abstract<T> implements RlFieldConverter<T> {
    private Class<T>type;
    
    protected Abstract(Class<T> type) {
      this.type = type;
    }
    
    public Class<T>getType() {
      return type;
    }
  }
  
  /**
   * 指定無し用マーカークラス。
   * <p>
   * {@link RlFieldAttr#converter()}のデフォルト値として指定される「コンバータ無し」を意味するクラス。
   * </p>
   * @author ysugimura
   */
  public static class None extends RlFieldConverter.Abstract<Object> {

    public None() {
      super(Object.class);
    }
    
    @Override
    public String toString(Object value) {
      throw new RlException("not supported");
    }

    @Override
    public Object fromString(String string) {
      throw new RlException("not supported");
    }
    
  }
  
  
  /**
   * ブール値コンバータ
   * @author ysugimura
   */
  public static class BooleanConv extends RlFieldConverter.Abstract<Boolean> {

    public BooleanConv() {
      super(Boolean.class);
    }
    
    @Override
    public String toString(Boolean value) {
      return value? "1":"0";
    }

    @Override
    public Boolean fromString(String string) {
      return Integer.parseInt(string) > 0;      
    }
    
  }
  
  /**
   * long値コンバータ
   */
  public static class LongConv extends RlFieldConverter.Abstract<Long> {

    public LongConv() {
      super(Long.class);
    }
    
    @Override
    public String toString(Long value) {
      return value + "";
    }

    @Override
    public Long fromString(String string) {
      return Long.parseLong(string);
    }
    
  }
  
  public static class IntConv extends RlFieldConverter.Abstract<Integer> {
   
    public IntConv() {
      super(Integer.class);
    }
    
    @Override
    public String toString(Integer value) {
      return value + "";
    }
    
    @Override
    public Integer fromString(String string) {
      return Integer.parseInt(string);
    }
    
  }
}
