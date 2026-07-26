package com.maddiewest.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String redirectToSwagger(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || !host.contains(":")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Swagger redirect is only available for explicit host:port requests");
        }
        return "redirect:/swagger-ui.html";
    }
}
