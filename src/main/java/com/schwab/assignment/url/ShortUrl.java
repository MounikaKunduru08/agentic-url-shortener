package com.schwab.assignment.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class ShortUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    private String code;
    @Column(nullable = false, length = 2048)
    private String destination;
    @Column(nullable = false)
    private final Instant createdAt = Instant.now();
    private long redirects;

    protected ShortUrl() {
    }

    public ShortUrl(String code, String destination) {
        this.code = code;
        this.destination = destination;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDestination() {
        return destination;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getRedirects() {
        return redirects;
    }

    public void recordRedirect() {
        redirects++;
    }
}
