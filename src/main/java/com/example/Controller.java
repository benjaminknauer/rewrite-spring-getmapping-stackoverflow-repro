package com.example;

import org.springframework.web.bind.annotation.GetMapping;

class Controller {
    @GetMapping("/hello")
    String hello() {
        return "hello";
    }
}
