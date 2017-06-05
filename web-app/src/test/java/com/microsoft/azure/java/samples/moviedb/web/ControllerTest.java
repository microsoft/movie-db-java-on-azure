/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(locations = "classpath:application.test.properties")
@ContextConfiguration(locations = {"classpath:/applicationContext-test.xml"})
public class ControllerTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    private MovieController movieController;
    private MockMvc mockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(movieController).build();
    }

    @Test
    public void testGetMovie() throws Exception {
        this.mockMvc.perform(get("/movies/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("movie"));
    }

    @Test
    public void testGetMovies() throws Exception {
        this.mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("movies"));
    }

}