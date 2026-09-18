package com.example.shortener;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * All the business logic lives here: validating input, generating a
 * unique code, and answering "what does this code redirect to, and
 * please count this as a click". The controller (below) stays thin and
 * just translates HTTP <-> these method calls.
 */
@Service
public class UrlShortenerService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final UrlRepository repository;
    private final SecureRandom random = new SecureRandom();

    public UrlShortenerService(UrlRepository repository) {
        this.repository = repository;
    }

    public ShortUrl createShortUrl(String longUrl) {
        validateLongUrl(longUrl);
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(code, longUrl, 0, LocalDateTime.now());
        repository.save(shortUrl);
        return shortUrl;
    }

    public ShortUrl getByShortCode(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new NoSuchElementException("No URL found for code: " + shortCode));
    }

    /** Looks up the long URL for a code AND records the click - this is
     * what the redirect endpoint calls. */
    public String resolveAndRecordClick(String shortCode) {
        ShortUrl shortUrl = getByShortCode(shortCode);
        repository.incrementClickCount(shortCode);
        return shortUrl.longUrl();
    }

    private void validateLongUrl(String longUrl) {
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("longUrl must not be blank");
        }
        if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
            throw new IllegalArgumentException("longUrl must start with http:// or https://");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!repository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique code after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}