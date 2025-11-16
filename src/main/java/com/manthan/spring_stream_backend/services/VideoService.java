package com.manthan.spring_stream_backend.services;

import com.manthan.spring_stream_backend.entities.Video;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {
    Video save(Video video, MultipartFile file);

    Video get(String videoId);

    Video getByTitle(String title);

    List<Video> getAllVideos();

    ResponseEntity<byte[]> stream(String videoId, String rangeHeader);
}
