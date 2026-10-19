package com.nexuscrm.auth;

import com.nexuscrm.shared.ApiException;
import java.nio.charset.StandardCharsets;

public final class Passwords {
  private Passwords() {}

  public static void validate(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.badRequest(
          "A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8.");
  }
}
