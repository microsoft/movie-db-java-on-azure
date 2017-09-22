/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.java.samples.moviedb.web;

import com.microsoft.azure.java.samples.moviedb.web.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;


@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${facebook.client.client-id}")
    String facebookAppId;

    @ModelAttribute
    public void globalAttributes(Model model) {
        Boolean isAllowedToUpdateMovieDB = true;
        String displayName = "Anonymous";

        if (facebookAppId != null && !facebookAppId.isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof OAuth2Authentication) {
                displayName = (String) ((LinkedHashMap) ((OAuth2Authentication) auth).getUserAuthentication().getDetails()).get("name");
                isAllowedToUpdateMovieDB = auth.isAuthenticated();
            } else {
                isAllowedToUpdateMovieDB = false;
            }
        }

        UserInfo userInfo = new UserInfo(displayName, isAllowedToUpdateMovieDB);
        model.addAttribute("userInfo", userInfo);
    }
}
