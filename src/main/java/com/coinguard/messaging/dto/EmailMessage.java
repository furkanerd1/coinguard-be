package com.coinguard.messaging.dto;

import java.io.Serializable;

public record EmailMessage(
        String to,
        String subject,
        String body,
        String templateName
) implements Serializable {
}

