package com.nexuscrm.auth;

import java.io.Serializable;

public record NexusPrincipal(long userId, long organizationId, long authVersion)
    implements Serializable {}
