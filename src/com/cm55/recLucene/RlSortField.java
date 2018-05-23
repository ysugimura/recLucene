package com.cm55.recLucene;


/**
 * ソートフィールド指定
 * <p>
 * ここでは、フィールドとその昇順・降順のみを指定する。
 * 複数のRlSortFieldがまとめられてRlSortFieldsになり、それが実際に検索の際
 * のソート指定となる。
 * </p>
 * <p>
 * フィールドを指定してのソートについては、Second Edition P160
 * </p>
 * 
 * @author ysugimura
 *
 */
public class RlSortField {

  /** フィールド*/
  public final RlField field;
  
  /** 降順 */
  public final boolean desc;

  /** 昇順ソートフィールドを指定する */
  public RlSortField(RlField field) {
    this(field, false);
  }
  
  /** 降順もしくは昇順ソートフィールドを指定する */
  public RlSortField(RlField field, boolean desc) {
    this.field = field;
    this.desc = desc;
  }

  /** フィールドを取得する */
  public RlField getField() {
    return field;
  }
  
  /** 降順フラグを取得する */
  public boolean getDesc() {
    return desc;
  }
  
  /** このフィールドのテーブル定義を取得する */
  public RlTable getTableDef() {
    return field.getTable();
  }
}
