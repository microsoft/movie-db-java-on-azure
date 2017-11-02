package com.microsoft.azure;

import com.microsoft.azure.serverless.functions.ExecutionContext;
import com.microsoft.azure.serverless.functions.annotation.BlobOutput;
import com.microsoft.azure.serverless.functions.annotation.BlobTrigger;
import com.microsoft.azure.serverless.functions.annotation.FunctionName;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class Function {
    private static byte[] resizeImage(byte[] imageInByte) throws IOException {
        InputStream in = new ByteArrayInputStream(imageInByte);
        BufferedImage originalImage = ImageIO.read(in);

        final int imageWidth = 60;
        final int imageHeight = imageWidth * originalImage.getHeight() / originalImage.getWidth();

        BufferedImage resizedImage = new BufferedImage(imageWidth, imageHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, imageWidth, imageHeight, null);
        g.dispose();

        byte[] imageInByteOut = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(resizedImage, "png", out);
            out.flush();
            imageInByteOut = out.toByteArray();
        }

        return imageInByteOut;
    }

    @FunctionName("resize")
    @BlobOutput(name = "myOutputBlob", path = "images-thumbnail/{name}",
            connection = "STORAGE_CONNECTION_STRING", dataType = "binary")
    public byte[] functionHandler(@BlobTrigger(name = "myBlob", path = "images-original/{name}",
            connection = "STORAGE_CONNECTION_STRING",
            dataType = "binary") byte[] myBlob,
                                  final ExecutionContext executionContext) throws IOException {
        executionContext.getLogger().log(Level.INFO, "Resizing image...1");
        return resizeImage(myBlob);
    }
}
