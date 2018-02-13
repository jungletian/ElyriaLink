package com.elyria.elyrialink;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class ElyriaLinkAnnotation {
  public final String clazz;
  public final boolean userRequired;

  ElyriaLinkAnnotation(String clazz, boolean userRequired) {
    this.clazz = clazz;
    this.userRequired = userRequired;
  }
}
