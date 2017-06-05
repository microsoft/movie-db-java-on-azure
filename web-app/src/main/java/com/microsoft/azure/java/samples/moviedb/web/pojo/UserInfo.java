/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.web.pojo;


public class UserInfo {
    private String displayName;
    private Boolean isAllowedToUpdateMovieDB;

    public UserInfo(String displayName, Boolean isAllowedToUpdateMovieDB) {
        this.displayName = displayName;
        this.isAllowedToUpdateMovieDB = isAllowedToUpdateMovieDB;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Boolean getIsAllowedToUpdateMovieDB() {
        return this.isAllowedToUpdateMovieDB;
    }
}
