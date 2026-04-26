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

  public static AccountType fromCode(String code) {
    if (code == null || code.isBlank()) return null;
    int numeric = Integer.parseInt(code);
    for (AccountType type : values()) {
      if (type.code == numeric) return type;
    }
    throw new IllegalArgumentException("Unknown account type code: " + code);
  }
}
