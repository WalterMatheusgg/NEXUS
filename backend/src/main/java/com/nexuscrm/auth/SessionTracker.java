package com.nexuscrm.auth;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionTracker {
  private final Map<Long, Deque<HttpSession>> sessions = new ConcurrentHashMap<>();

  public synchronized void register(long user, HttpSession session) {
    String sessionId = session.getId();
    sessions
        .values()
        .forEach(active -> active.removeIf(s -> !valid(s) || s.getId().equals(sessionId)));
    Deque<HttpSession> active = sessions.computeIfAbsent(user, key -> new ArrayDeque<>());
    active.removeIf(s -> !valid(s) || s.getId().equals(session.getId()));
    while (active.size() >= 5) invalidate(active.removeFirst());
    active.addLast(session);
  }

  public synchronized void invalidateAll(long user) {
    Deque<HttpSession> active = sessions.remove(user);
    if (active != null) active.forEach(this::invalidate);
  }

  private boolean valid(HttpSession session) {
    try {
      session.getCreationTime();
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  private void invalidate(HttpSession session) {
    try {
      session.invalidate();
    } catch (IllegalStateException ignored) {
    }
  }

  @Scheduled(fixedDelay = 600000)
  public synchronized void prune() {
    sessions.values().forEach(active -> active.removeIf(s -> !valid(s)));
    sessions.entrySet().removeIf(e -> e.getValue().isEmpty());
  }
}
