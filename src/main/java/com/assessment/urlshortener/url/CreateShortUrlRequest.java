package com.assessment.urlshortener.url;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateShortUrlRequest(

        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL is too long")
        String originalUrl,

        @Future(message = "Expiry must be in the future")
        LocalDateTime expiresAt

)
{
}