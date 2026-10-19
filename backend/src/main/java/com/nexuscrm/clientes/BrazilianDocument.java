package com.nexuscrm.clientes;

import com.nexuscrm.shared.ApiException;

public final class BrazilianDocument {
  private BrazilianDocument() {}

  public static String normalize(String type, String input) {
    if (input == null || !input.matches("[0-9.\\/\\-\\s]+"))
      throw ApiException.badRequest("CPF/CNPJ deve conter somente dígitos e pontuação válida.");
    String digits = input.replaceAll("\\D", "");
    if (!isValid(type, digits))
      throw ApiException.badRequest("CPF/CNPJ inválido para o tipo de cliente informado.");
    return digits;
  }

  public static boolean isValid(String type, String value) {
    if (value == null || !value.matches("[0-9]+") || value.chars().distinct().count() == 1)
      return false;
    if ("PERSON".equals(type) && value.length() == 11) {
      return digit(value, 9, new int[] {10, 9, 8, 7, 6, 5, 4, 3, 2}) == value.charAt(9) - '0'
          && digit(value, 10, new int[] {11, 10, 9, 8, 7, 6, 5, 4, 3, 2}) == value.charAt(10) - '0';
    }
    if ("COMPANY".equals(type) && value.length() == 14) {
      return digit(value, 12, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2})
              == value.charAt(12) - '0'
          && digit(value, 13, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2})
              == value.charAt(13) - '0';
    }
    return false;
  }

  private static int digit(String value, int length, int[] weights) {
    int sum = 0;
    for (int i = 0; i < length; i++) sum += (value.charAt(i) - '0') * weights[i];
    int remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  }
}
