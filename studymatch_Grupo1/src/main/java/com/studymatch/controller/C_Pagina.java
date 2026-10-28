package com.studymatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class C_Pagina {

    @GetMapping({ "/", "/index.html" })
    public String index() {
        return "index";
    }
}
