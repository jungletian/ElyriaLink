package com.elyria.elyrialink;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

@Target({ ElementType.FIELD }) @Retention(RetentionPolicy.SOURCE) public @interface Json {
}
