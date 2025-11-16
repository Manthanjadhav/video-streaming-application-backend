package com.manthan.spring_stream_backend.services.Impl;
import com.manthan.spring_stream_backend.entities.Video;
import com.manthan.spring_stream_backend.repositories.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.manthan.spring_stream_backend.services.S3Service;
import com.manthan.spring_stream_backend.services.VideoService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final S3Service s3Service;

    @Override
    public Video save(Video video, MultipartFile file) {
        log.info("Starting video save process for title: {}", video.getTitle());

        // Validation
        if (file == null || file.isEmpty()) {
            log.warn("File is missing or empty for video '{}'", video.getTitle());
            throw new IllegalArgumentException("Video file must not be empty.");
        }

        if (video.getTitle() == null || video.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Video title cannot be empty.");
        }

        try {
            // Upload file to S3
            String fileUrl = s3Service.uploadFile(file);

            // Set properties
            video.setContentType(file.getContentType());
            video.setFilePath(fileUrl);

            // Save to database
            Video saved = videoRepository.save(video);

            log.info("Video '{}' saved successfully with ID: {}", saved.getTitle(), saved.getVideoId());
            return saved;

        } catch (Exception e) {
            log.error("Error while saving video '{}': {}", video.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to save video: " + e.getMessage(), e);
        }
    }

    @Override
    public Video get(String videoId) {
        try {
            return videoRepository.findById(UUID.fromString(videoId))
                    .orElseThrow(() -> new RuntimeException("Video not found with ID: " + videoId));
        } catch (IllegalArgumentException e) {
            log.error("Invalid video ID: {}", videoId);
            throw new RuntimeException("Invalid video ID format.", e);
        }
    }

    @Override
    public Video getByTitle(String title) {
        return videoRepository.findByTitle(title)
                .orElseThrow(() -> new RuntimeException("Video not found with title: " + title));
    }


    @Override
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    @Override
    public ResponseEntity<byte[]> stream(String videoId, String rangeHeader) {

        Video video = get(videoId);
        String fileUrl = video.getFilePath();
        String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        try {
            long fileSize = s3Service.getFileSize(key);

            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);

                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            if (end > fileSize - 1) {
                end = fileSize - 1;
            }

            byte[] data = s3Service.getPartialFile(key, start, end);
            long contentLength = end - start + 1;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", video.getContentType());
            headers.add("Accept-Ranges", "bytes");
            headers.add("Content-Length", String.valueOf(contentLength));
            headers.add("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);

            return new ResponseEntity<>(data, headers,
                    rangeHeader == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT);

        } catch (Exception e) {
            throw new RuntimeException("Failed to stream video: " + e.getMessage(), e);
        }
    }

}
