package com.elyria.elyrialink;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS) public @interface ElyriaLink {
  // 注解里的内容
  String[] value();

  boolean userRequire() default false;
}
