package com.schwab.assignment.url;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UrlServiceTest {
  @Mock ShortUrlRepository repository;
  @InjectMocks UrlService service;

  @Test void persistsNewShortUrlForValidHttpsDestination() {
    when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ShortUrl created=service.shorten("https://example.com/a");
    assertEquals("https://example.com/a",created.getDestination()); assertEquals(8,created.getCode().length());
    verify(repository).save(any(ShortUrl.class));
  }
  @Test void rejectsNonHttpDestinationWithoutPersisting() {
    assertThrows(IllegalArgumentException.class, () -> service.shorten("file:///etc/passwd"));
    verifyNoInteractions(repository);
  }
  @Test void resolveRecordsRedirect() {
    ShortUrl shortUrl=new ShortUrl("abcd1234","https://example.com"); when(repository.findByCode("abcd1234")).thenReturn(Optional.of(shortUrl));
    assertSame(shortUrl,service.resolve("abcd1234")); assertEquals(1,shortUrl.getRedirects());
  }
  @Test void unknownCodeHasClearException() {
    when(repository.findByCode("missing")).thenReturn(Optional.empty());
    assertThrows(UrlService.UrlNotFoundException.class, () -> service.analytics("missing"));
  }
}
