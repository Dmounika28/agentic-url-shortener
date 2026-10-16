    package com.assessment.urlshortener.url;

    import jakarta.servlet.http.HttpServletResponse;
    import jakarta.validation.Valid;

    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.io.IOException;
    import java.util.Map;

    @RestController
    @RequestMapping("/api/urls")
    public class ShortUrlController {

        private final ShortUrlService service;

        public ShortUrlController(ShortUrlService service) {
            this.service = service;
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> create(
                @Valid @RequestBody CreateShortUrlRequest request) {

            ShortUrl url = service.create(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "code", url.getShortCode(),
                            "shortUrl", "http://localhost:8080/r/"
                                    + url.getShortCode(),
                            "originalUrl", url.getOriginalUrl()
                    )
            );
        }

        @GetMapping("/{code}")
        public Map<String, Object> get(@PathVariable String code) {
            ShortUrl url = service.getByCode(code);

            return Map.of(
                    "code", url.getShortCode(),
                    "originalUrl", url.getOriginalUrl(),
                    "clickCount", url.getClickCount()
            );
        }
    }