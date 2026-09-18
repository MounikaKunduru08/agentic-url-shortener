package com.example.shortener;

import java.time.LocalDateTime;

/**
 * One row of the short_urls table. A Java record - it's just data,
 * immutable, with no behavior beyond what's built into records
 * automatically (equals/hashCode/toString/accessors).
 */
public record ShortUrl(String shortCode, String longUrl, int clickCount, LocalDateTime createdAt) {
}