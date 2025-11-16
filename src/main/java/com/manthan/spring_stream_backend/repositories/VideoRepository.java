package com.manthan.spring_stream_backend.repositories;

import com.manthan.spring_stream_backend.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    Optional<Video> findByTitle(String title);
}
