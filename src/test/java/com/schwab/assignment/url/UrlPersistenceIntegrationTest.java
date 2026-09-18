package com.schwab.assignment.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class UrlPersistenceIntegrationTest {
    @Autowired
    UrlService service;
    @Autowired
    ShortUrlRepository repository;

    @Test
    void createdUrlAndRedirectCountArePersisted() {
        ShortUrl created = service.shorten("https://example.com/persisted");
        service.resolve(created.getCode());
        ShortUrl reloaded = repository.findByCode(created.getCode()).orElseThrow();
        assertEquals("https://example.com/persisted", reloaded.getDestination());
        assertEquals(1, reloaded.getRedirects());
    }
}
