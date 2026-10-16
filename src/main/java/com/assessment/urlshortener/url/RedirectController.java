package com.assessment.urlshortener.url;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class RedirectController {

    private final ShortUrlService service;

    public RedirectController(ShortUrlService service) {
        this.service = service;
    }

    @GetMapping("/r/{code}")
    public void redirect(
            @PathVariable String code,
            HttpServletResponse response) throws IOException {

        ShortUrl url = service.getAndTrackClick(code);
        response.sendRedirect(url.getOriginalUrl());
    }
}