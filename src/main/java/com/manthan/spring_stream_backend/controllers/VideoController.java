package com.manthan.spring_stream_backend.controllers;

import com.manthan.spring_stream_backend.entities.Video;
import com.manthan.spring_stream_backend.payload.CustomMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.manthan.spring_stream_backend.services.VideoService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description) {

        try {
            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);

            Video savedVideo = videoService.save(video, file);

            return ResponseEntity.ok(savedVideo);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(CustomMessage.builder()
                            .message(e.getMessage())
                            .success(false)
                            .build());

        } catch (Exception e) {
            log.error("Video upload failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage.builder()
                            .message("Video upload failed: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Video>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<?> streamVideo(
            @PathVariable String videoId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        try {
            return videoService.stream(videoId, rangeHeader);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error streaming video: " + e.getMessage());
        }
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<Video> getVideo(@PathVariable String videoId) {
        return ResponseEntity.ok(videoService.get(videoId));
    }
}
