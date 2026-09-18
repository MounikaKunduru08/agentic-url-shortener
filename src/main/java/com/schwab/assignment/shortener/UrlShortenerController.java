package com.schwab.assignment.shortener;

import com.schwab.assignment.shortener.UrlShortenerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public  UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    /*
     * Request DTO used when creating a short URL.
     *
     * @NotBlank ensures destination cannot be
     * null, empty, or whitespace.
     */
    public record ShortenRequest(
            @NotBlank String destination
    ) {
    }

    /*
     * Response returned after creating a short URL.
     */
    public record ShortenResponse(
            String code,
            String shortUrl,
            String destination
    ) {
    }

    /*
     * Response returned by the analytics endpoint.
     */
    public record AnalyticsResponse(
            String code,
            String destination,
            long redirects
    ) {
    }

    /**
     * Creates a shortened URL.
     *
     * POST /api/urls
     *
     * Example request:
     *
     * {
     *   "destination": "https://example.com/products"
     * }
     */
    @PostMapping("/api/urls")
    public ResponseEntity<ShortenResponse> shorten(
            @Valid @RequestBody ShortenRequest request) {

        ShortUrl url =
                service.shorten(
                        request.destination()
                );

        URI location =
                URI.create(
                        "/" + url.getCode()
                );

        return ResponseEntity
                .created(location)
                .body(toResponse(url));
    }

    /**
     * Redirects a short code to its original URL.
     *
     * GET /{code}
     *
     * Example:
     *
     * GET /abc12345
     *
     * HTTP 302
     * Location: https://example.com/products
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code) {

        ShortUrl url =
                service.resolve(code);

        URI destination =
                URI.create(
                        url.getDestination()
                );

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(destination)
                .build();
    }

    /**
     * Returns analytics for a short URL.
     *
     * GET /api/urls/{code}/analytics
     */
    @GetMapping("/api/urls/{code}/analytics")
    public AnalyticsResponse analytics(
            @PathVariable String code) {

        ShortUrl url =
                service.analytics(code);

        return new AnalyticsResponse(
                url.getCode(),
                url.getDestination(),
                url.getRedirects()
        );
    }

    /**
     * Converts the database entity into the
     * REST API response representation.
     */
    private ShortenResponse toResponse(
            ShortUrl url) {

        return new ShortenResponse(
                url.getCode(),
                "/" + url.getCode(),
                url.getDestination()
        );
    }
}