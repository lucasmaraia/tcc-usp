package com.emailservice.presentation.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class ViewController {

    @GetMapping(value = "/templates.html", produces = MediaType.TEXT_HTML_VALUE)
    public byte[] templates() throws IOException {
        return loadFile("templates/templates.html");
    }

    @GetMapping(value = "/login.html", produces = MediaType.TEXT_HTML_VALUE)
    public byte[] login() throws IOException {
        return loadFile("templates/login.html");
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/templates.html";
    }

    private byte[] loadFile(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8).getBytes();
    }
}