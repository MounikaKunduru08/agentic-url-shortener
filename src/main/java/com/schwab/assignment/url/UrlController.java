package com.schwab.assignment.url;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
public class UrlController {
  private final UrlService service;
  UrlController(UrlService service) { this.service = service; }
  record ShortenRequest(@NotBlank String destination) {} record ShortenResponse(String code, String shortUrl, String destination) {}
  record AnalyticsResponse(String code, String destination, long redirects) {}
  @PostMapping("/api/urls") ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
    ShortUrl url = service.shorten(request.destination()); return ResponseEntity.created(URI.create("/" + url.getCode())).body(toResponse(url)); }
  @GetMapping("/{code}") ResponseEntity<Void> redirect(@PathVariable String code) { return ResponseEntity.status(302).location(URI.create(service.resolve(code).getDestination())).build(); }
  @GetMapping("/api/urls/{code}/analytics") AnalyticsResponse analytics(@PathVariable String code) { ShortUrl u = service.analytics(code); return new AnalyticsResponse(u.getCode(), u.getDestination(), u.getRedirects()); }
  private ShortenResponse toResponse(ShortUrl u) { return new ShortenResponse(u.getCode(), "/" + u.getCode(), u.getDestination()); }
}
