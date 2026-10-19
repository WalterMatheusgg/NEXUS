package com.nexuscrm.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.nexuscrm.shared.ApiException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

class SessionTrackerTests {
  @Test
  void switchingAccountsDetachesTheSessionFromThePreviousAccount() {
    SessionTracker tracker = new SessionTracker();
    HttpSession session = session("same-browser");
    tracker.register(1L, session);
    tracker.register(2L, session);
    tracker.invalidateAll(1L);
    verify(session, never()).invalidate();
    tracker.invalidateAll(2L);
    verify(session).invalidate();
  }

  @Test
  void signingIntoASixthBrowserRevokesOnlyTheOldestSession() {
    SessionTracker tracker = new SessionTracker();
    HttpSession first = session("first-browser");
    tracker.register(1L, first);
    for (int index = 0; index < 5; index++) {
      tracker.register(1L, session("browser-" + index));
    }
    verify(first).invalidate();
  }

  @Test
  void passwordPolicyRejectsShortPasswordsAndBcryptUtf8Truncation() {
    assertThatThrownBy(() -> Passwords.validate("short")).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> Passwords.validate("á".repeat(40))).isInstanceOf(ApiException.class);
    Passwords.validate("Strong-local-passphrase-2026");
  }

  private HttpSession session(String id) {
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn(id);
    return session;
  }
}
