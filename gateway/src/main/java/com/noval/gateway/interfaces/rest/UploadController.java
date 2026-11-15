package com.noval.gateway.interfaces.rest;

import com.noval.gateway.infrastructure.storage.QiniuStorageService;
import com.noval.gateway.interfaces.rest.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Image upload endpoint - uploads images to Qiniu OSS and returns public URLs.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Image Upload", description = "图片上传 - 上传到七牛云 OSS 获取公网 URL")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final QiniuStorageService qiniuStorage;

    public UploadController(QiniuStorageService qiniuStorage) {
        this.qiniuStorage = qiniuStorage;
    }

    @Operation(summary = "上传图片文件", description = "上传一张或多张图片到七牛云，返回公网可访问 URL")
    @PostMapping(value = "/images/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadImages(
            @Parameter(description = "图片文件（支持多张）")
            @RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("No files provided", "bad_request", null));
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                log.info("Uploading file: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
                byte[] bytes = file.getBytes();
                String url = qiniuStorage.uploadBytes(bytes, file.getContentType(), file.getOriginalFilename());
                urls.add(url);
                log.info("Uploaded: {}", url);
            } catch (Exception e) {
                log.error("Upload failed for {}", file.getOriginalFilename(), e);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Upload failed: " + e.getMessage(), "upload_error", null));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("urls", urls);
        result.put("count", urls.size());
        return ResponseEntity.ok(result);
    }
}
