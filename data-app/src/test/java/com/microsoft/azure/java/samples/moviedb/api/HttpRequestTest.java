/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@TestPropertySource(locations = "classpath:application.test.properties")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpRequestTest {

    private static final String FIRST_MOVIE_PATH = "/api/v1/movies/1";
    @LocalServerPort
    private int port;
    @Autowired
    private RestTemplateBuilder builder;
    private RestTemplate restTemplate;

    @Before
    public void setup() throws Exception {
        restTemplate = builder.rootUri("http://localhost:" + port).build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void pingApiV1Uri() throws Exception {
        String result = this.restTemplate.getForObject("/api/v1",
                String.class);
        assertTrue(result.contains("movies"));
    }

    @Test
    public void getMovie() throws Exception {
        Movie firstMovie = this.restTemplate.getForObject(FIRST_MOVIE_PATH, Movie.class);
        assertThat(firstMovie.getId(), is(1L));
        assertThat(firstMovie.getName(), is("Inception (2010)"));
        assertThat(firstMovie.getDescription(), is("This is the description."));
        assertThat(firstMovie.getRating(), is(9.7));
        assertNull(firstMovie.getImageUri());
    }

    @Test
    public void patchMovieRating() throws Exception {
        Movie movie = new Movie();
        movie.setRating(0.0);
        this.restTemplate.patchForObject(FIRST_MOVIE_PATH, new HttpEntity<>(movie), Void.class);

        Movie patchedMovie = this.restTemplate.getForObject(FIRST_MOVIE_PATH, Movie.class);
        assertThat(patchedMovie.getRating(), is(0.0));

        movie.setRating(9.7);
        this.restTemplate.patchForObject(FIRST_MOVIE_PATH, new HttpEntity<>(movie), Void.class);

        Movie restoredMovie = this.restTemplate.getForObject(FIRST_MOVIE_PATH, Movie.class);
        assertThat(restoredMovie.getRating(), is(9.7));
    }
}