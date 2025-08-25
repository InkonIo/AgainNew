// src/main/java/com/chatalyst/backend/controller/ProductController.java
package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.CreateProductRequest;
import com.chatalyst.backend.dto.MessageResponse;
import com.chatalyst.backend.dto.ProductResponse;
import com.chatalyst.backend.dto.UpdateProductRequest;
import com.chatalyst.backend.dto.ExcelImportResponse;
import com.chatalyst.backend.security.services.ProductService;
import com.chatalyst.backend.security.services.UserPrincipal;
import com.chatalyst.backend.security.services.PsObjectStorageService; // Изменено: используем PsObjectStorageService
import com.chatalyst.backend.security.services.ExcelProductImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "API для управления товарами")
public class ProductController {

    private final ProductService productService;
    private final PsObjectStorageService psObjectStorageService; // Изменено: используем PsObjectStorageService
    private final ExcelProductImportService excelProductImportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Создать новый товар с изображением", 
               description = "Создает новый товар, привязанный к боту, с возможностью загрузки изображения.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар успешно создан",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> createProduct(
            @RequestParam("name") String name,
            @RequestParam("price") String price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("catalog") String catalog,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "inStock", defaultValue = "true") boolean inStock,
            @RequestParam("botId") Long botId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // Создаем объект запроса
            CreateProductRequest request = new CreateProductRequest();
            request.setName(name);
            request.setPrice(new java.math.BigDecimal(price));
            request.setDescription(description);
            request.setCatalog(catalog);
            request.setSubcategory(subcategory);
            request.setInStock(inStock);
            request.setBotId(botId);

            // Если предоставлено изображение, загружаем его
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = psObjectStorageService.uploadImage(imageFile, name); // Изменено
                request.setImageUrl(imageUrl);
                log.info("Изображение загружено для товара \'{}\' : {}", name, imageUrl);
            }

            ProductResponse product = productService.createProduct(request, userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
            
        } catch (NumberFormatException e) {
            log.error("Неверный формат цены для пользователя {}: {}", userPrincipal.getEmail(), price);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Неверный формат цены"));
        } catch (RuntimeException e) {
            log.error("Ошибка при создании товара для пользователя {}: {}", userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Обновить товар с изображением", 
               description = "Обновляет существующий товар по его ID с возможностью обновления изображения.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар успешно обновлен",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "catalog", required = false) String catalog,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "inStock", required = false) Boolean inStock,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteCurrentImage", defaultValue = "false") boolean deleteCurrentImage,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // Получаем текущий товар для проверки прав и получения старого URL изображения
            ProductResponse currentProduct = productService.getProductById(id);
            String oldImageUrl = currentProduct.getImageUrl();

            // Создаем объект запроса на обновление
            UpdateProductRequest request = new UpdateProductRequest();
            if (name != null) request.setName(name);
            if (price != null) request.setPrice(new java.math.BigDecimal(price));
            if (description != null) request.setDescription(description);
            if (catalog != null) request.setCatalog(catalog);
            if (subcategory != null) request.setSubcategory(subcategory);
            if (inStock != null) request.setInStock(inStock);

            // Обработка изображения
            if (deleteCurrentImage && oldImageUrl != null && !oldImageUrl.isEmpty()) {
                // Удаляем старое изображение
                psObjectStorageService.deleteImage(oldImageUrl); // Изменено
                request.setImageUrl(null);
                log.info("Старое изображение удалено для товара с ID {}: {}", id, oldImageUrl);
            } else if (imageFile != null && !imageFile.isEmpty()) {
                // Загружаем новое изображение
                String productName = name != null ? name : currentProduct.getName();
                String newImageUrl = psObjectStorageService.uploadImage(imageFile, productName); // Изменено
                request.setImageUrl(newImageUrl);
                
                // Удаляем старое изображение, если оно было
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    psObjectStorageService.deleteImage(oldImageUrl); // Изменено
                    log.info("Старое изображение заменено для товара с ID {}: {} -> {}", 
                            id, oldImageUrl, newImageUrl);
                }
            }

            ProductResponse updatedProduct = productService.updateProduct(id, request, userPrincipal.getId());
            return ResponseEntity.ok(updatedProduct);
            
        } catch (NumberFormatException e) {
            log.error("Неверный формат цены для пользователя {}: {}", userPrincipal.getEmail(), price);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Неверный формат цены"));
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении товара с ID {} для пользователя {}: {}", 
                     id, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    // Оригинальные методы без изменений для обратной совместимости
    @PostMapping("/json")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Создать новый товар (JSON)", description = "Создает новый товар, привязанный к боту (только JSON).")
    public ResponseEntity<?> createProductJson(@Valid @RequestBody CreateProductRequest request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            ProductResponse product = productService.createProduct(request, userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (RuntimeException e) {
            log.error("Ошибка при создании товара для пользователя {}: {}", userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/json")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Обновить товар (JSON)", description = "Обновляет существующий товар по его ID (только JSON).")
    public ResponseEntity<?> updateProductJson(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            ProductResponse updatedProduct = productService.updateProduct(id, request, userPrincipal.getId());
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении товара с ID {} для пользователя {}: {}", id, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Удалить товар", description = "Удаляет товар по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар успешно удален"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Получаем товар для удаления его изображения
            ProductResponse product = productService.getProductById(id);
            String imageUrl = product.getImageUrl();
            
            // Удаляем товар
            productService.deleteProduct(id, userPrincipal.getId());
            
            // Удаляем изображение из PS.kz Object Storage, если оно есть
            if (imageUrl != null && !imageUrl.isEmpty()) {
                psObjectStorageService.deleteImage(imageUrl); // Изменено
                log.info("Изображение удалено вместе с товаром ID {}: {}", id, imageUrl);
            }
            
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении товара с ID {} для пользователя {}: {}", id, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    // ========================================================================
    // НОВЫЕ МЕТОДЫ ДЛЯ УДАЛЕНИЯ КАТАЛОГОВ И ПОДКАТЕГОРИЙ
    // ========================================================================

    @DeleteMapping("/bot/{botId}/catalogs/{catalog}")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Удалить каталог и все его товары", 
               description = "Удаляет все товары для указанного бота и каталога. Возвращает количество удаленных товаров.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно удалены",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Бот или каталог не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteCatalogByBot(
            @PathVariable Long botId,
            @PathVariable String catalog,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            long deletedCount = productService.deleteProductsByBotAndCatalog(botId, catalog, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse("Удалено товаров: " + deletedCount + " в каталоге: " + catalog));
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении каталога '{}' для бота ID {} пользователем {}: {}", 
                    catalog, botId, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @DeleteMapping("/bot/{botId}/catalogs/{catalog}/subcategories/{subcategory}")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Удалить подкатегорию и все ее товары", 
               description = "Удаляет все товары для указанного бота, каталога и подкатегории. Возвращает количество удаленных товаров.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно удалены",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Бот, каталог или подкатегория не найдены",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteSubcategoryByBotAndCatalog(
            @PathVariable Long botId,
            @PathVariable String catalog,
            @PathVariable String subcategory,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            long deletedCount = productService.deleteProductsByBotAndSubcategory(botId, catalog, subcategory, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse("Удалено товаров: " + deletedCount + " в подкатегории: " + subcategory));
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении подкатегории '{}' в каталоге '{}' для бота ID {} пользователем {}: {}", 
                    subcategory, catalog, botId, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Получить товар по ID", description = "Возвращает информацию о товаре по его ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар успешно получен",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
            @ApiResponse(responseCode = "404", description = "Товар не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении товара с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @GetMapping("/bot/{botId}")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Получить товары бота", description = "Возвращает все товары, привязанные к указанному боту.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно получены",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getProductsByBotId(@PathVariable Long botId,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            List<ProductResponse> products = productService.getProductsByBotId(botId, userPrincipal.getId());
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении товаров для бота с ID {} для пользователя {}: {}", botId, userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    // Новый эндпоинт для получения уникальных каталогов
    @GetMapping("/bot/{botId}/catalogs")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Получить уникальные каталоги бота", description = "Возвращает список уникальных названий каталогов для указанного бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Каталоги успешно получены",
                    content = @Content(schema = @Schema(implementation = String.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    public ResponseEntity<List<String>> getUniqueCatalogsByBot(@PathVariable Long botId) {
        List<String> catalogs = productService.getUniqueCatalogsByBot(botId);
        return ResponseEntity.ok(catalogs);
    }

    // Новый эндпоинт для получения уникальных подкаталогов по каталогу
    @GetMapping("/bot/{botId}/catalogs/{catalog}/subcategories")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Получить уникальные подкаталоги по каталогу", description = "Возвращает список уникальных названий подкаталогов для указанного бота и каталога.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подкаталоги успешно получены",
                    content = @Content(schema = @Schema(implementation = String.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    public ResponseEntity<List<String>> getUniqueSubcategoriesByBotAndCatalog(
            @PathVariable Long botId,
            @PathVariable String catalog) {
        List<String> subcategories = productService.getUniqueSubcategoriesByBotAndCatalog(botId, catalog);
        return ResponseEntity.ok(subcategories);
    }

    // Новый эндпоинт для получения товаров по подкаталогу (в наличии)
    @GetMapping("/bot/{botId}/catalogs/{catalog}/subcategories/{subcategory}/in-stock")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Получить товары по подкаталогу (в наличии)", description = "Возвращает список товаров из указанного подкаталога, которые есть в наличии.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно получены",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
    })
    public ResponseEntity<List<ProductResponse>> getProductsByBotAndSubcategoryInStock(
            @PathVariable Long botId,
            @PathVariable String catalog,
            @PathVariable String subcategory) {
        List<ProductResponse> products = productService.getProductsByBotAndSubcategoryInStock(botId, catalog, subcategory);
        return ResponseEntity.ok(products);
    }

    // Эндпоинт для загрузки изображения отдельно
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Загрузить изображение отдельно", 
               description = "Загружает изображение в хранилище и возвращает его URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Изображение успешно загружено",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка загрузки изображения",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productName") String productName) {
        try {
            String imageUrl = psObjectStorageService.uploadImage(file, productName); // Изменено
            return ResponseEntity.ok(new MessageResponse("Изображение успешно загружено: " + imageUrl));
        } catch (Exception e) {
            log.error("Ошибка загрузки изображения: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Ошибка загрузки изображения: " + e.getMessage()));
        }
    }

    // Эндпоинт для удаления изображения отдельно
    @DeleteMapping(value = "/images/delete")
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Удалить изображение отдельно", 
               description = "Удаляет изображение из хранилища по его URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Изображение успешно удалено",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка удаления изображения",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteImage(@RequestParam("imageUrl") String imageUrl) {
        try {
            boolean deleted = psObjectStorageService.deleteImage(imageUrl); // Изменено
            if (deleted) {
                return ResponseEntity.ok(new MessageResponse("Изображение успешно удалено."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Не удалось удалить изображение."));
            }
        } catch (Exception e) {
            log.error("Ошибка удаления изображения: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Ошибка удаления изображения: " + e.getMessage()));
        }
    }

    // Новый эндпоинт для импорта товаров из Excel файла
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole(\'USER\') or hasRole(\'ADMIN\')")
    @Operation(summary = "Импорт товаров из Excel файла", 
               description = "Загружает Excel файл с товарами и автоматически создает товары с помощью OpenAI для обработки данных.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товары успешно импортированы",
                    content = @Content(schema = @Schema(implementation = ExcelImportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка при импорте товаров",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Бот не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> importProductsFromExcel(
            @RequestParam("file") MultipartFile excelFile,
            @RequestParam("botId") Long botId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // Проверяем, что файл является Excel файлом
            String filename = excelFile.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Файл должен быть в формате Excel (.xlsx или .xls)"));
            }

            // Проверяем размер файла (максимум 10MB)
            if (excelFile.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new MessageResponse("Размер файла не должен превышать 10MB"));
            }

            log.info("Начинается импорт товаров из Excel файла '{}' для бота ID {} пользователем {}", 
                    filename, botId, userPrincipal.getEmail());

            // Импортируем товары
            List<ProductResponse> createdProducts = excelProductImportService.importProductsFromExcel(
                    excelFile, botId, userPrincipal.getId());

            // Формируем ответ
            ExcelImportResponse response = new ExcelImportResponse();
            response.setTotalProcessed(createdProducts.size());
            response.setSuccessfullyCreated(createdProducts.size());
            response.setFailed(0);
            response.setCreatedProducts(createdProducts);
            response.setMessage("Импорт завершен успешно. Создано товаров: " + createdProducts.size());

            log.info("Импорт товаров завершен успешно. Создано товаров: {} для бота ID {}", 
                    createdProducts.size(), botId);

            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Ошибка при импорте товаров для пользователя {}: {}", userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Ошибка при импорте товаров: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при импорте товаров для пользователя {}: {}", 
                     userPrincipal.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Внутренняя ошибка сервера при импорте товаров"));
        }
    }
}
