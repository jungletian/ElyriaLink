package com.elyria.elyrialink;

/**
 * @author jungletian
 */

public class IllegalElyriaLinkException extends Exception {
  public IllegalElyriaLinkException(String message) {
    super(message);
  }

  public IllegalElyriaLinkException(String detailMessage, Throwable cause) {
    super(detailMessage, cause);
  }
}
