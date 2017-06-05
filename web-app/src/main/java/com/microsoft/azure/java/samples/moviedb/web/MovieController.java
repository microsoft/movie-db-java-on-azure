/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web;

import com.microsoft.azure.java.samples.moviedb.web.pojo.Movie;
import com.microsoft.azure.java.samples.moviedb.web.pojo.MoviesResponse;
import com.microsoft.azure.java.samples.moviedb.web.pojo.PageInfo;
import com.microsoft.azure.java.samples.moviedb.web.util.AzureStorageUploader;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * Controller that handles HTTP requests and returns corresponding views.
 */
@Controller
public class MovieController {
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private AzureStorageUploader azureStorageUploader;

    /**
     * Get movie info by movie id.
     *
     * @param id    movie id
     * @param model spring model
     * @return movie detail page
     */
    @RequestMapping(value = "/movies/{id}", method = RequestMethod.GET)
    public String getMovieById(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.getMovie(Long.toString(id));
        if (movie != null) {
            if (movie.getImageUri() != null) {
                movie.setImageFullPathUri(azureStorageUploader.getAzureStorageBaseUri() + movie.getImageUri());
            }

            model.addAttribute("movie", movie);
            return "moviedetail";
        } else {
            return "moviedetailerror";
        }
    }

    /**
     * Update movie description by movie id.
     *
     * @param id          movie id
     * @param description movie description
     * @return updated movie detail page
     */
    @RequestMapping(value = "/movies/{id}", method = RequestMethod.POST)
    public String updateMovieDescription(@PathVariable Long id, @RequestParam("description") String description) {
        logger.debug("Update movie description");

        Movie movie = new Movie();
        movie.setDescription(description);

        movieRepository.patchMovie(Long.toString(id), movie);

        return "redirect:/movies/" + id;
    }

    /**
     * Get one page of movies.
     *
     * @param page  page number
     * @param model spring model
     * @return movie list page
     */
    @RequestMapping(value = "/movies", method = RequestMethod.GET)
    public String getMovieList(@RequestParam(value = "page", required = false, defaultValue = "0") Long page,
                               Model model) {
        MoviesResponse moviesResponse = movieRepository.getMovies(Long.toString(page));
        if (moviesResponse != null) {
            setupMovieList(moviesResponse, model);
        }

        model.addAttribute("page", page);

        return "moviespage";
    }

    /**
     * Upload image file to Azure blob and save its relative path to database.
     *
     * @param file image file
     * @param id   movie id
     * @return updated movie detail page
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String updateMovieImage(@RequestParam("file") MultipartFile file, @RequestParam("id") Long id) {
        logger.debug(file.getOriginalFilename());

        String newName = id + "." + FilenameUtils.getExtension(file.getOriginalFilename());
        String imageUri = this.azureStorageUploader.uploadToAzureStorage(file, newName.toLowerCase());

        if (imageUri != null) {
            Timestamp timestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
            String timestampQuery = "?timestamp=" + timestamp.toString();

            Movie movie = new Movie();
            movie.setImageUri(imageUri.toLowerCase() + timestampQuery);
            movieRepository.patchMovie(Long.toString(id), movie);
        }

        return "redirect:/movies/" + id;
    }

    private void setupMovieList(MoviesResponse moviesResponse, Model model) {
        setupMovieListPageInfo(moviesResponse, model);
        setupMovieListThumbnail(moviesResponse, model);
    }

    private void setupMovieListPageInfo(MoviesResponse moviesResponse, Model model) {
        final String movielistPath = "/movies?page=";
        PageInfo page = moviesResponse.getPage();
        if (page != null) {
            Integer number = page.getNumber();
            Integer prev = number - 1;
            Integer next = number + 1;
            model.addAttribute("number", next);
            String prevPath, nextPath;
            if (prev >= 0) {
                prevPath = movielistPath + String.valueOf(prev);
                model.addAttribute("prev", prevPath);
                model.addAttribute("hasprev", true);
            }
            if (next < page.getTotalPages()) {
                nextPath = movielistPath + String.valueOf(next);
                model.addAttribute("next", nextPath);
                model.addAttribute("hasnext", true);
            }
        }
    }

    private void setupMovieListThumbnail(MoviesResponse moviesResponse, Model model) {
        List<Movie> movies = moviesResponse.getMovieList().getMovies();
        for (Movie movie : movies) {
            if (movie.getImageUri() != null) {
                String thumbnailUri = movie.getImageUri().replace(
                        azureStorageUploader.getOriginalImageContainer().toLowerCase(),
                        azureStorageUploader.getThumbnailImageContainer().toLowerCase());

                movie.setThumbnailFullPathUri(azureStorageUploader.getAzureStorageBaseUri() + thumbnailUri);
            }
        }

        model.addAttribute("movies", movies);
    }
}
