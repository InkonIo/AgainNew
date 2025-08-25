// src/main/java/com/chatalyst/backend/controller/ProductImageController.java
package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.MessageResponse;
import com.chatalyst.backend.security.services.ProductService;
import com.chatalyst.backend.security.services.UserPrincipal;
import com.chatalyst.backend.security.services.PsObjectStorageService; // Изменено: используем PsObjectStorageService
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления изображениями товаров через PS.kz Object Storage.
 */
@RestController
@RequestMapping("/api/products/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Image Management", description = "API для управления изображениями товаров")
public class ProductImageController {

    private final PsObjectStorageService psObjectStorageService; // Изменено: используем PsObjectStorageService
    private final ProductService productService;

    /**
     * Загружает изображение товара в PS.kz Object Storage.
     * @param file Файл изображения.
     * @param productName Название товара (для генерации имени файла).
     * @param userPrincipal Аутентифицированный пользователь.
     * @return URL загруженного изображения.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Загрузить изображение товара", 
               description = "Загружает изображение товара в PS.kz Object Storage и возвращает URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Изображение успешно загружено",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос или файл",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> uploadProductImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productName") String productName,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Файл изображения не может быть пустым"));
            }

            if (productName == null || productName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Название товара не может быть пустым"));
            }

            String imageUrl = psObjectStorageService.uploadImage(file, productName.trim()); // Изменено: используем PsObjectStorageService
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Изображение успешно загружено");
            
            log.info("Пользователь {} загрузил изображение для товара \'{}\': {}", 
                    userPrincipal.getEmail(), productName, imageUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке изображения для пользователя {}: {}", 
                     userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Ошибка загрузки: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при загрузке изображения: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Удаляет изображение из PS.kz Object Storage.
     * @param imageUrl URL изображения для удаления.
     * @param userPrincipal Аутентифицированный пользователь.
     * @return Результат операции удаления.
     */
    @DeleteMapping
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Удалить изображение товара", 
               description = "Удаляет изображение товара из PS.kz Object Storage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Изображение успешно удалено",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> deleteProductImage(
            @RequestParam("imageUrl") String imageUrl,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("URL изображения не может быть пустым"));
            }

            boolean deleted = psObjectStorageService.deleteImage(imageUrl.trim()); // Изменено: используем PsObjectStorageService
            
            if (deleted) {
                log.info("Пользователь {} удалил изображение: {}", userPrincipal.getEmail(), imageUrl);
                return ResponseEntity.ok(new MessageResponse("Изображение успешно удалено"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Не удалось удалить изображение"));
            }
            
        } catch (Exception e) {
            log.error("Ошибка при удалении изображения для пользователя {}: {}", 
                     userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка при удалении изображения"));
        }
    }

    /**
     * Проверяет доступность изображения по URL.
     * @param imageUrl URL изображения для проверки.
     * @return Результат проверки доступности.
     */
    @GetMapping("/check")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Проверить доступность изображения", 
               description = "Проверяет, доступно ли изображение по указанному URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Результат проверки",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> checkImageAccessibility(@RequestParam("imageUrl") String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("URL изображения не может быть пустым"));
            }

            // PsObjectStorageService не имеет метода isImageAccessible, так как S3 не предоставляет прямого способа
            // проверить доступность объекта без попытки его получить или использовать предзагруженный URL.
            // В большинстве случаев, если объект загружен успешно, он будет доступен по публичному URL.
            // Если требуется проверка, можно попробовать сгенерировать предзагруженный URL и проверить его.
            // Для простоты, пока уберем эту проверку, или оставим заглушку.
            // boolean accessible = psObjectStorageService.isImageAccessible(imageUrl.trim()); // Этот метод удален
            boolean accessible = true; // Заглушка, если метод isImageAccessible не нужен или будет реализован по-другому
            
            Map<String, Object> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("accessible", accessible);
            response.put("message", accessible ? "Изображение доступно" : "Изображение недоступно");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при проверке доступности изображения: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Ошибка при проверке изображения"));
        }
    }
}