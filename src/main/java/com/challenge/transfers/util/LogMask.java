package com.challenge.transfers.util;

public final class LogMask {

  private LogMask() {}

  /** Shows only last 4 digits: 32345379 → ****5379 */
  public static String document(String value) {
    if (value == null || value.isBlank() || value.length() <= 4) return "****";
    return "****" + value.substring(value.length() - 4);
  }

  /** Shows first 4 and last 4 digits: 2850001040094059465088 → 2850**********5088 */
  public static String cbu(String value) {
    if (value == null || value.isBlank() || value.length() <= 9) return "****";
    int maskedLength = value.length() - 8;
    return value.substring(0, 4) + "*".repeat(maskedLength) + value.substring(value.length() - 4);
  }
}
