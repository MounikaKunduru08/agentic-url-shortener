package com.schwab.assignment.shortener;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    private String code;

    @Column(
            nullable = false,
            length = 2048
    )
    private String destination;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private long redirects;

    /**
     * Required by JPA.
     */
    protected ShortUrl() {
    }

    public ShortUrl(
            String code,
            String destination) {

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