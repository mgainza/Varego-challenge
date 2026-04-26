package com.challenge.transfers.model.api;

public enum DocumentType {
  DNI("01"),
  LE("02"),
  LC("03"),
  CUIT("11"),
  CI("101"),
  PAS("125");

  private final String code;

  DocumentType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
