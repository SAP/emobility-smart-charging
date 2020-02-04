package com.sap.charging.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; 


@Configuration
public class CustomWebMvcConfigurerAdapter implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/playground").setViewName("redirect:/playground/");
        registry.addViewController("/playground/").setViewName("redirect:/playground/index.html");
    }
}