/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point of spring boot web application.
 */
@SpringBootApplication
@EnableCaching
public class WebApplication {

    /**
     * main entry point.
     *
     * @param args the parameters
     */
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}