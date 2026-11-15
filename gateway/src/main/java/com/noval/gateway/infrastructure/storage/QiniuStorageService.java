package com.noval.gateway.infrastructure.storage;

import com.noval.gateway.infrastructure.config.QiniuConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Qiniu Cloud OSS storage service.
 * Uploads images to Qiniu and returns public URLs.
 */
@Service
public class QiniuStorageService {

    private static final Logger log = LoggerFactory.getLogger(QiniuStorageService.class);

    private final QiniuConfig config;
    private final UploadManager uploadManager;
    private final Auth auth;

    public QiniuStorageService(QiniuConfig config) {
        this.config = config;
        Configuration cfg = new Configuration(Region.autoRegion());
        this.uploadManager = new UploadManager(cfg);
        this.auth = Auth.create(config.getAccessKey(), config.getSecretKey());
    }

    /**
     * Upload a base64 encoded image to Qiniu.
     * @param base64Data base64 data URL (e.g., "data:image/png;base64,...")
     * @return public URL of the uploaded image
     */
    public String uploadBase64(String base64Data) {
        if (!config.isConfigured()) {
            throw new IllegalStateException("Qiniu OSS is not configured. Please set gateway.qiniu.* properties.");
        }

        try {
            // Parse base64 data URL
            String base64Content = base64Data;
            String extension = "png";

            if (base64Data.contains(",")) {
                String[] parts = base64Data.split(",", 2);
                if (parts.length == 2) {
                    base64Content = parts[1];
                    // Extract extension from data URL
                    if (parts[0].contains("image/jpeg")) {
                        extension = "jpg";
                    } else if (parts[0].contains("image/webp")) {
                        extension = "webp";
                    } else if (parts[0].contains("image/gif")) {
                        extension = "gif";
                    }
                }
            }

            // Decode base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Content);

            // Generate unique filename
            String filename = generateFilename(extension);

            // Upload to Qiniu
            String uploadToken = auth.uploadToken(config.getBucket());
            Response response = uploadManager.put(imageBytes, filename, uploadToken);

            if (!response.isOK()) {
                throw new RuntimeException("Failed to upload to Qiniu: " + response.error);
            }

            // Build public URL
            String domain = config.getDomain();
            if (!domain.endsWith("/")) {
                domain += "/";
            }
            String publicUrl = domain + filename;

            log.info("Uploaded image to Qiniu: {}", publicUrl);
            return publicUrl;

        } catch (QiniuException e) {
            log.error("Qiniu upload error", e);
            throw new RuntimeException("Failed to upload to Qiniu: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 data", e);
            throw new RuntimeException("Invalid base64 image data: " + e.getMessage(), e);
        }
    }

    /**
     * Upload raw bytes to Qiniu.
     * @param bytes image bytes
     * @param contentType MIME type (e.g. "image/jpeg")
     * @param originalFilename original filename for extension detection
     * @return public URL of the uploaded image
     */
    public String uploadBytes(byte[] bytes, String contentType, String originalFilename) {
        if (!config.isConfigured()) {
            throw new IllegalStateException("Qiniu OSS is not configured. Please set gateway.qiniu.* properties.");
        }

        try {
            String extension = detectExtension(contentType, originalFilename);
            String filename = generateFilename(extension);

            String uploadToken = auth.uploadToken(config.getBucket());
            Response response = uploadManager.put(bytes, filename, uploadToken);

            if (!response.isOK()) {
                throw new RuntimeException("Failed to upload to Qiniu: " + response.error);
            }

            String domain = config.getDomain();
            if (!domain.endsWith("/")) {
                domain += "/";
            }
            String publicUrl = domain + filename;

            log.info("Uploaded image to Qiniu: {} ({} bytes)", publicUrl, bytes.length);
            return publicUrl;

        } catch (QiniuException e) {
            log.error("Qiniu upload error", e);
            throw new RuntimeException("Failed to upload to Qiniu: " + e.getMessage(), e);
        }
    }

    private String detectExtension(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
            if (contentType.contains("webp")) return "webp";
            if (contentType.contains("gif")) return "gif";
            if (contentType.contains("png")) return "png";
        }
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
                    ext.equals("webp") || ext.equals("gif")) {
                return ext;
            }
        }
        return "png";
    }

    private String generateFilename(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        return "compose/" + timestamp + "_" + uuid + "." + extension;
    }
}
