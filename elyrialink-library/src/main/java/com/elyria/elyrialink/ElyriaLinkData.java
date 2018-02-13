package com.elyria.elyrialink;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class ElyriaLinkData {
  public final Class<?> clazz;
  public final boolean userRequired;

  public ElyriaLinkData(Class<?> clazz, boolean userRequired) {
    this.clazz = clazz;
    this.userRequired = userRequired;
  }
}
