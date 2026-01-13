package com.pgrdaw.tagfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/help")
public class HelpController {

    @GetMapping("/images")
    public String helpImages() {
        return "help/images";
    }

    @GetMapping("/tags")
    public String helpTags() {
        return "help/tags";
    }

    @GetMapping("/filters")
    public String helpFilters() {
        return "help/filters";
    }

    @GetMapping("/reports")
    public String helpReports() {
        return "help/reports";
    }
}
