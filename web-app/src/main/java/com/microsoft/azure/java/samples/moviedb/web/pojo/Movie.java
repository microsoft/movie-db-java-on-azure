/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * Movie class that contains all movie properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Movie implements Serializable {
    private Long id;
    private String name;
    private String description;
    private Double rating;
    private String imageUri;
    private String imageFullPathUri;
    private String thumbnailFullPathUri;

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
        return name;
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
        return description;
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
        return rating;
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

    /**
     * Get full path image uri.
     *
     * @return full path image uri.
     */
    public String getImageFullPathUri() {
        return this.imageFullPathUri;
    }

    /**
     * Set full path image uri.
     *
     * @param imageFullPathUri full path image uri
     */
    public void setImageFullPathUri(String imageFullPathUri) {
        this.imageFullPathUri = imageFullPathUri;
    }

    /**
     * Get full path thumbnail uri.
     *
     * @return full path thumbnail uri
     */
    public String getThumbnailFullPathUri() {
        return this.thumbnailFullPathUri;
    }

    /**
     * Set full path thumbnail uri.
     *
     * @param thumbnailFullPathUri full path thumbnail uri
     */
    public void setThumbnailFullPathUri(String thumbnailFullPathUri) {
        this.thumbnailFullPathUri = thumbnailFullPathUri;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Movie: {");
        builder.append("id: ").append(id != null ? id : "").append(",");
        builder.append("name: ").append(name != null ? name : "").append(",");
        builder.append("description: ").append(description != null ? description : "").append(",");
        builder.append("rating: ").append(rating != null ? rating : "").append(",");
        builder.append("imageFullPathUri: ").append(imageFullPathUri != null ? imageFullPathUri : "").append(",");
        builder.append("thumbnailFullPathUri: ").append(thumbnailFullPathUri != null ? thumbnailFullPathUri : "");
        builder.append("}");
        return builder.toString();
    }
}