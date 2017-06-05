/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

/**
 * Class that contains a list of movies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieList implements Serializable {
    private List<Movie> movies;

    /**
     * Get movie list.
     *
     * @return movie list
     */
    public List<Movie> getMovies() {
        return movies;
    }
}
