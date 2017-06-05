/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Class corresponds to JSON response of get movies rest api.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoviesResponse implements Serializable {
    @JsonProperty("_embedded")
    private MovieList movieList;
    private PageInfo page;

    /**
     * Get movie list.
     *
     * @return movie list
     */
    @JsonProperty("_embedded")
    public MovieList getMovieList() {
        return movieList;
    }

    /**
     * Get page information.
     *
     * @return page information
     */
    public PageInfo getPage() {
        return this.page;
    }
}
