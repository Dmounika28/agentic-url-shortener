package com.assessment.urlshortener.url;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;

@Service
public class ShortUrlService {

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortUrlRepository repository;

    public ShortUrlService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShortUrl create(CreateShortUrlRequest request) {
        validateUrl(request.originalUrl());

        String code;

        do {
            code = generateCode();
        } while (repository.existsByShortCode(code));

        ShortUrl shortUrl = new ShortUrl(
                request.originalUrl(),
                code,
                request.expiresAt()
        );

        return repository.save(shortUrl);
    }

    @Transactional
    public ShortUrl getAndTrackClick(String code) {
        ShortUrl shortUrl = repository.findByShortCode(code)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found"));

        if (shortUrl.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired");
        }

        shortUrl.incrementClickCount();

        return shortUrl;
    }

    public ShortUrl getByCode(String code) {
        return repository.findByShortCode(code)
                .orElseThrow(() ->
                        new UrlNotFoundException("Short URL not found"));
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);

            if (!uri.isAbsolute()
                    || uri.getHost() == null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException(
                        "Only valid HTTP and HTTPS URLs are allowed");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            code.append(CHARACTERS.charAt(
                    RANDOM.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }
}