package org.apache.lucene.analysis.hunspell;

import java.util.regex.Pattern;

public class HunspellAffix {
  private String append; /* the affix itself, what is appended */
  private char appendFlags[]; /* continuation class flags */
  private String strip;
  
  private String condition;
  
  private char flag;
  private boolean crossProduct;
  
  private Pattern conditionPattern;
  
  /**
   * @return the append
   */
  public String getAppend() {
    return append;
  }

  /**
   * @param append the append to set
   */
  public void setAppend(String append) {
    this.append = append;
  }

  /**
   * @return the appendFlags
   */
  public char[] getAppendFlags() {
    return appendFlags;
  }

  /**
   * @param appendFlags the appendFlags to set
   */
  public void setAppendFlags(char[] appendFlags) {
    this.appendFlags = appendFlags;
  }

  /**
   * @return the strip
   */
  public String getStrip() {
    return strip;
  }

  /**
   * @param strip the strip to set
   */
  public void setStrip(String strip) {
    this.strip = strip;
  }

  /**
   * @return the condition
   */
  public String getCondition() {
    return condition;
  }

  /**
   * @param condition the condition to set
   */
  public void setCondition(String condition) {
    this.condition = condition;
    this.conditionPattern = Pattern.compile(".*" + condition);
  }

  /**
   * @return the flag
   */
  public char getFlag() {
    return flag;
  }

  /**
   * @param flag the flag to set
   */
  public void setFlag(char flag) {
    this.flag = flag;
  }

  /**
   * @return the crossProduct
   */
  public boolean isCrossProduct() {
    return crossProduct;
  }

  /**
   * @param crossProduct the crossProduct to set
   */
  public void setCrossProduct(boolean crossProduct) {
    this.crossProduct = crossProduct;
  }
  
  public boolean checkCondition(char input[], int offset, int length) {
    // XXX
    StringBuilder builder = new StringBuilder();
    builder.append(input, offset, length);
    return conditionPattern.matcher(builder).matches();
  }
}
