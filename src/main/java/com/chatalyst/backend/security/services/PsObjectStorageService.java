package com.chatalyst.backend.security.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class PsObjectStorageService {

    @Value("${ps.bucket-name}")
    private String bucketName;

    @Value("${ps.endpoint-url}")
    private String endpointUrl;

    private final S3Client s3Client;

    // Внедряем только S3Client через конструктор (без S3Presigner)
    public PsObjectStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
        log.info("PsObjectStorageService инициализирован с S3Client.");
    }

    /**
     * Загружает изображение в PS.kz Object Storage.
     * @param file Файл изображения для загрузки.
     * @param productName Название товара (используется для генерации имени файла).
     * @return URL загруженного изображения.
     * @throws RuntimeException если загрузка не удалась.
     */
    public String uploadImage(MultipartFile file, String productName) {
        if (file.isEmpty()) {
            throw new RuntimeException("Файл изображения пуст");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Файл должен быть изображением");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            
            String key = generateFileName(productName, fileExtension);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            log.info("Изображение успешно загружено в S3: {}", key);
            
            // Генерируем публичный URL для доступа к объекту
            return String.format("%s/%s/%s", endpointUrl, bucketName, key);

        } catch (S3Exception e) {
            log.error("Ошибка S3 при загрузке изображения: {}", e.getMessage());
            throw new RuntimeException("Ошибка S3 при загрузке изображения: " + e.getMessage());
        } catch (IOException e) {
            log.error("Ошибка при чтении файла изображения: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обработке файла изображения: " + e.getMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при загрузке изображения: {}", e.getMessage());
            throw new RuntimeException("Неожиданная ошибка при загрузке изображения: " + e.getMessage());
        }
    }

    /**
     * Удаляет изображение из PS.kz Object Storage.
     * @param imageUrl URL изображения для удаления.
     * @return true если удаление прошло успешно, false в противном случае.
     */
    public boolean deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("Попытка удалить изображение с пустым URL");
            return false;
        }

        try {
            // Извлекаем ключ объекта из URL
            String key = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Изображение успешно удалено из S3: {}", key);
            return true;

        } catch (S3Exception e) {
            log.error("Ошибка S3 при удалении изображения: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при удалении изображения: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Генерирует уникальное имя файла на основе названия товара.
     * @param productName Название товара.
     * @param fileExtension Расширение файла.
     * @return Уникальное имя файла.
     */
    private String generateFileName(String productName, String fileExtension) {
        String cleanProductName = productName.replaceAll("[^a-zA-Z0-9а-яА-Я\\s]", "")
                                            .replaceAll("\\s+", "_")
                                            .toLowerCase();
        
        if (cleanProductName.length() > 30) {
            cleanProductName = cleanProductName.substring(0, 30);
        }
        
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("product_%s_%s%s", cleanProductName, uniqueId, fileExtension);
    }
}

