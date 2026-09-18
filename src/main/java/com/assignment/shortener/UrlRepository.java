package com.example.shortener;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Talks to the database directly with JdbcTemplate - no ORM, no magic.
 * Every method here is a plain SQL statement with '?' placeholders,
 * which Spring fills in safely (this is what makes it immune to SQL
 * injection - user input is always a bound parameter, never pasted
 * into the query string).
 */
@Repository
public class UrlRepository {

    private final JdbcTemplate jdbcTemplate;

    public UrlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ShortUrl shortUrl) {
        jdbcTemplate.update(
                "INSERT INTO short_urls (short_code, long_url, click_count, created_at) VALUES (?, ?, ?, ?)",
                shortUrl.shortCode(), shortUrl.longUrl(), shortUrl.clickCount(),
                Timestamp.valueOf(shortUrl.createdAt()));
    }

    public Optional<ShortUrl> findByShortCode(String shortCode) {
        List<ShortUrl> results = jdbcTemplate.query(
                "SELECT short_code, long_url, click_count, created_at FROM short_urls WHERE short_code = ?",
                (rs, rowNum) -> new ShortUrl(
                        rs.getString("short_code"),
                        rs.getString("long_url"),
                        rs.getInt("click_count"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                shortCode);
        return results.stream().findFirst();
    }

    public boolean existsByShortCode(String shortCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM short_urls WHERE short_code = ?", Integer.class, shortCode);
        return count != null && count > 0;
    }

    public void incrementClickCount(String shortCode) {
        jdbcTemplate.update("UPDATE short_urls SET click_count = click_count + 1 WHERE short_code = ?", shortCode);
    }
}