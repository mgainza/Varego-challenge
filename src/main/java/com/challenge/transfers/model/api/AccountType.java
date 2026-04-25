package com.challenge.transfers.model.api;

public enum AccountType {
  OWN_CHECKING(1),
  NON_OWN_CHECKING(2),
  OWN_OTHER(3),
  NON_OWN_OTHER(4);

  private final int code;

  AccountType(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
