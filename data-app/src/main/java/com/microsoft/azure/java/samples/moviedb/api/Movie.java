/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Movie entity corresponds to `movies` table.
 */
@Entity
@Table(name = "movies")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Movie {
    @Id
    private Long id;
    private String name;
    private String description;
    private Double rating;
    private String imageUri;

    /**
     * Get movie id.
     *
     * @return movie id
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Set movie id.
     *
     * @param id movie id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get movie name.
     *
     * @return movie name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set movie name.
     *
     * @param name movie name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get movie description.
     *
     * @return movie description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set movie description.
     *
     * @param description movie description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get movie rating.
     *
     * @return movie rating
     */
    public Double getRating() {
        return this.rating;
    }

    /**
     * Set movie rating.
     *
     * @param rating movie rating
     */
    public void setRating(Double rating) {
        this.rating = rating;
    }

    /**
     * Get image uri.
     *
     * @return image uri
     */
    public String getImageUri() {
        return this.imageUri;
    }

    /**
     * Set image uri.
     *
     * @param imageUri image uri
     */
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
