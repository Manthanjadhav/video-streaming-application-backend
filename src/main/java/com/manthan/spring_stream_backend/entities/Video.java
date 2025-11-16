package com.manthan.spring_stream_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name="videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID videoId;

    private String title;

    private String description;

    private String contentType;

    private String filePath;

//    @ManyToOne
//    private Course course;
}
