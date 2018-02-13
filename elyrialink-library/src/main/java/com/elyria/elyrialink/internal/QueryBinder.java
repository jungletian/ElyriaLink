package com.elyria.elyrialink.internal;

import com.elyria.elyrialink.IllegalElyriaLinkException;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public interface QueryBinder<T> {
  void bind(T target) throws IllegalElyriaLinkException;
}
