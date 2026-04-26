package com.challenge.transfers.exception;

public class SoapTimeoutException extends RuntimeException {

  public SoapTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
