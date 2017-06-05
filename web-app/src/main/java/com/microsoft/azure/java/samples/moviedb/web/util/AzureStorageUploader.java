/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.java.samples.moviedb.web.util;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

/**
 * Helper class that provides function to upload image to Azure storage.
 */
@Component
public class AzureStorageUploader {
    private static final Logger logger = LoggerFactory.getLogger(AzureStorageUploader.class);

    private final String storageConnectionString;
    private final String originalImageContainer;
    private final String thumbnailImageContainer;

    private String azureStorageBaseUri;

    /**
     * Constructor that accepts settings from property file.
     *
     * @param storageConnectionString Azure storage connection string
     * @param originalImageContainer  storage container name for original images
     * @param thumbnailImageContainer storage container name for thumbnail images
     */
    public AzureStorageUploader(@Value("${moviedb.webapp.storageconnectionstring}") String storageConnectionString,
                                @Value("${moviedb.webapp.originalImageContainer}") String originalImageContainer,
                                @Value("${moviedb.webapp.thumbnailImageContainer}") String thumbnailImageContainer) {
        logger.debug(storageConnectionString);
        logger.debug(originalImageContainer);
        logger.debug(thumbnailImageContainer);

        this.storageConnectionString = storageConnectionString;
        this.originalImageContainer = (originalImageContainer == null || originalImageContainer.isEmpty())
                ? "images-original" : originalImageContainer;
        this.thumbnailImageContainer = (thumbnailImageContainer == null || thumbnailImageContainer.isEmpty())
                ? "images-thumbnail" : thumbnailImageContainer;
    }

    /**
     * Get container name of original images.
     *
     * @return container name
     */
    public String getOriginalImageContainer() {
        return originalImageContainer;
    }

    /**
     * Get container name of thumbnail images.
     *
     * @return container name
     */
    public String getThumbnailImageContainer() {
        return thumbnailImageContainer;
    }

    /**
     * Get the base URI of Azure storage.
     *
     * @return base URI string
     */
    public String getAzureStorageBaseUri() {
        if (azureStorageBaseUri == null) {
            CloudStorageAccount storageAccount;
            try {
                storageAccount = CloudStorageAccount.parse(this.storageConnectionString);
                azureStorageBaseUri = "https://" + storageAccount.createCloudBlobClient().getEndpoint().getHost();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                logger.error("InvalidKeyException: " + e.getMessage());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                logger.error("URISyntaxException: " + e.getMessage());
            }
        }

        return azureStorageBaseUri;
    }

    /**
     * Upload image file to Azure storage with specified name.
     *
     * @param file     image file object
     * @param fileName specified file name
     * @return relative path of the created image blob
     */
    public String uploadToAzureStorage(MultipartFile file, String fileName) {
        String uri = null;

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(this.storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            setupContainer(blobClient, this.thumbnailImageContainer);
            CloudBlobContainer originalImageContainer = setupContainer(blobClient, this.originalImageContainer);

            if (originalImageContainer != null) {
                CloudBlockBlob blob = originalImageContainer.getBlockBlobReference(fileName);
                blob.upload(file.getInputStream(), file.getSize());

                uri = blob.getUri().getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error uploading image: " + e.getMessage());
        }

        return uri;
    }

    private CloudBlobContainer setupContainer(CloudBlobClient blobClient, String containerName) {
        try {
            CloudBlobContainer container = blobClient.getContainerReference(containerName);
            if (!container.exists()) {
                container.createIfNotExists();
                BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
                containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
                container.uploadPermissions(containerPermissions);
            }

            return container;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error setting up container: " + e.getMessage());
            return null;
        }
    }
}
