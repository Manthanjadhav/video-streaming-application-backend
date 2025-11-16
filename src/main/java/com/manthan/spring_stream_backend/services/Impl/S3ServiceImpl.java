package com.manthan.spring_stream_backend.services.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.manthan.spring_stream_backend.services.S3Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String fileName = Instant.now().toEpochMilli() + "_" + file.getOriginalFilename();

        try (S3Client s3Client = getS3Client()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, fileName);

            log.info("✅ File '{}' uploaded successfully to bucket '{}'", fileName, bucketName);
            return fileUrl;

        } catch (IOException e) {
            log.error("❌ Failed to read file input stream: {}", e.getMessage());
            throw new RuntimeException("Failed to read file input stream", e);

        } catch (S3Exception e) {
            log.error("❌ Failed to upload file to S3: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        log.info("🗑️ Deleting file '{}' from S3", fileName);

        try (S3Client s3Client = getS3Client()) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("✅ File deleted successfully from S3: {}", fileUrl);

        } catch (S3Exception e) {
            log.error("❌ Failed to delete file: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public long getFileSize(String key) {
        S3Client s3 = getS3Client();
        HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        return head.contentLength();
    }

    public byte[] getPartialFile(String key, long start, long end) {
        S3Client s3 = getS3Client();

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .range("bytes=" + start + "-" + end)
                .build();

        return s3.getObjectAsBytes(req).asByteArray();
    }
}
