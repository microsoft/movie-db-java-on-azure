/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of spring boot application.
 */
@SpringBootApplication
public class Application {
    /**
     * Main entry point.
     *
     * @param args the parameters
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
