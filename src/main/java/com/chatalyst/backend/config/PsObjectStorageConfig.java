package com.chatalyst.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;

@Configuration
public class PsObjectStorageConfig {

    @Value("${ps.access-key-id}")
    private String accessKeyId;

    @Value("${ps.secret-access-key}")
    private String secretAccessKey;

    @Value("${ps.endpoint-url}")
    private String endpointUrl;

    @Bean
public S3Client s3Client() {
    return S3Client.builder()
            .endpointOverride(URI.create(endpointUrl))  // https://object.pscloud.io
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .region(Region.US_EAST_1) // или нужный регион
            .build();
}
}


