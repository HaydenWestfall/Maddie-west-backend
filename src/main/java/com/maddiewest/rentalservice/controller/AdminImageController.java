package com.maddiewest.rentalservice.controller;

import com.maddiewest.rentalservice.dto.response.ApiResponse;
import com.maddiewest.rentalservice.service.R2StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
@Tag(name = "Admin images", description = "Upload rental item images to Cloudflare R2")
public class AdminImageController {

    private final R2StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an image", description = "Stores an image in R2 and returns its public URL.")
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        String url = storageService.upload(file);
        return ApiResponse.ok(Map.of("url", url));
    }

    @DeleteMapping
    @Operation(summary = "Delete an image", description = "Removes an image from R2 given its public URL.")
    public ApiResponse<Void> delete(@RequestParam("url") String url) {
        storageService.deleteByUrl(url);
        return ApiResponse.ok(null);
    }
}
