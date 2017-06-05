/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Definition for page info.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageInfo implements Serializable {
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    private Integer number;

    /**
     * Get page size.
     *
     * @return page size.
     */
    public Integer getSize() {
        return this.size;
    }

    /**
     * Get the number of all elements.
     *
     * @return number of all elements
     */
    public Integer getTotalElements() {
        return this.totalElements;
    }

    /**
     * Get total number of pages.
     *
     * @return number of pages
     */
    public Integer getTotalPages() {
        return this.totalPages;
    }

    /**
     * Get page number.
     *
     * @return page number
     */
    public Integer getNumber() {
        return this.number;
    }
}
