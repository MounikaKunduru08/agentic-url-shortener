package com.schwab.assignment.shortener;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.URI;
import java.util.UUID;

@Service
public class UrlShortenerService {

    private final ShortUrlRepository repository;

    public UrlShortenerService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShortUrl shorten(String destination) {

        URI uri = URI.create(destination);

        if (uri.getScheme() == null
            || !(uri.getScheme().equals("http")
                 || uri.getScheme().equals("https"))
            || uri.getHost() == null) {

            throw new IllegalArgumentException(
                    "destination must be an absolute http(s) URL"
            );
        }

        if (isUnsafeHost(uri.getHost())) {
            throw new IllegalArgumentException(
                    "destination must not target localhost or a private network"
            );
        }

        String code = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        return repository.save(
                new ShortUrl(code, destination)
        );
    }

    @Transactional
    public ShortUrl resolve(String code) {

        ShortUrl url = repository.findByCode(code)
                .orElseThrow(
                        () -> new UrlNotFoundException(code)
                );

        url.recordRedirect();

        return url;
    }

    public ShortUrl analytics(String code) {

        return repository.findByCode(code)
                .orElseThrow(
                        () -> new UrlNotFoundException(code)
                );
    }

    private boolean isUnsafeHost(String host) {

        String normalized = host.toLowerCase();

        if (normalized.equals("localhost")
            || normalized.endsWith(".localhost")) {

            return true;
        }

        try {

            InetAddress address =
                    InetAddress.getByName(host);

            return address.isAnyLocalAddress()
                   || address.isLoopbackAddress()
                   || address.isLinkLocalAddress()
                   || address.isSiteLocalAddress();

        } catch (Exception ignored) {

            return false;
        }
    }

    public static class UrlNotFoundException
            extends RuntimeException {

        public UrlNotFoundException(String code) {
            super(
                    "short URL not found: " + code
            );
        }
    }
}