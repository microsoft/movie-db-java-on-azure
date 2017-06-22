/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web;

import com.microsoft.azure.java.samples.moviedb.web.pojo.Movie;
import com.microsoft.azure.java.samples.moviedb.web.pojo.MoviesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

/**
 * Wrapper for sending rest api request to data app with redis cache support.
 */
@Repository
public class MovieRepository {
    private static final String PATH_MOVIE_SEARCH_BY_ID = "/movies/";
    private static final String PATH_MOVIE_SEARCH_BY_PAGE = "/movies?page=";
    private static final Logger logger = LoggerFactory.getLogger(MovieRepository.class);
    private final RestTemplate restTemplate;

    /**
     * Construct rest template with data app uri.
     *
     * @param builder    rest template builder
     * @param dataAppUri data app uri from application.properties
     */
    public MovieRepository(RestTemplateBuilder builder, @Value("${moviedb.webapp.dataAppUri}") String dataAppUri) {
        logger.debug("data app:" + dataAppUri);

        String trimmedURL = dataAppUri.trim().toLowerCase();
        String dataAppApiUrl;
        if (trimmedURL.startsWith("http://") || trimmedURL.startsWith("https://")) {
            dataAppApiUrl = trimmedURL + "/api/v1";
        } else {
            dataAppApiUrl = "http://" + trimmedURL + "/api/v1";
        }

        logger.debug("data app api root url: " + dataAppApiUrl);
        restTemplate = builder.rootUri(dataAppApiUrl).build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    /**
     * Get movie list by page number.
     *
     * @param page page number
     * @return response object that contains movie list and page info
     */
    public MoviesResponse getMovies(String page) {
        String requestPath = PATH_MOVIE_SEARCH_BY_PAGE + page;
        logger.debug(requestPath);
        try {
            return this.restTemplate.getForObject(requestPath, MoviesResponse.class);
        } catch (Exception e) {
            logger.error("Error requesting movies: ", e);
        }
        return null;
    }

    /**
     * Get movie by movie id.
     *
     * @param id movie id
     * @return movie object
     */
    @Cacheable(cacheNames = "movie", key = "#id")
    public Movie getMovie(String id) {
        String requestPath = PATH_MOVIE_SEARCH_BY_ID + id;
        logger.debug(requestPath);
        try {
            return this.restTemplate.getForObject(requestPath, Movie.class);
        } catch (Exception e) {
            logger.error("Error requesting movie: ", e);
        }
        return null;
    }

    /**
     * Patch movie by movie id.
     *
     * @param id    movie id
     * @param movie movie object
     */
    @CacheEvict(cacheNames = "movie", key = "#id")
    public void patchMovie(String id, Movie movie) {
        try {
            this.restTemplate.patchForObject("/movies/" + id, new HttpEntity<>(movie), Void.class);
        } catch (Exception e) {
            logger.error("Error patching movie: ", e);
        }
    }
}
