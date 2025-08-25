// src/main/java/com/chatalyst/backend/security/services/ExcelProductImportService.java
package com.chatalyst.backend.security.services;

import com.chatalyst.backend.dto.CreateProductRequest;
import com.chatalyst.backend.dto.ProductResponse;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.util.CustomMultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelProductImportService {

    private final ProductService productService;
    private final BotRepository botRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final PsObjectStorageService psObjectStorageService;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    /**
     * Импортирует товары из Excel файла с использованием OpenAI для маппинга данных.
     * @param excelFile Excel файл с товарами.
     * @param botId ID бота, к которому привязываются товары.
     * @param userId ID пользователя (для проверки прав).
     * @return Список созданных товаров.
     * @throws RuntimeException если произошла ошибка при обработке файла.
     */
    public List<ProductResponse> importProductsFromExcel(MultipartFile excelFile, Long botId, Long userId) {
        try {
            // Проверяем права пользователя на бота
            Bot bot = botRepository.findById(botId)
                    .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));
            
            if (!bot.getOwner().getId().equals(userId)) {
                throw new RuntimeException("У вас нет прав для добавления товаров в этого бота.");
            }

            // Парсим Excel файл
            List<Map<String, Object>> excelData = parseExcelFile(excelFile);
            log.info("Извлечено {} строк из Excel файла", excelData.size());

            // Используем OpenAI для маппинга данных
            List<Map<String, Object>> mappedData = mapDataWithOpenAI(excelData);
            log.info("Данные успешно обработаны через OpenAI");

            // Создаем товары
            List<ProductResponse> createdProducts = new ArrayList<>();
            for (Map<String, Object> productData : mappedData) {
                try {
                    CreateProductRequest request = convertToCreateProductRequest(productData, botId);
                    ProductResponse product = productService.createProduct(request, userId);
                    createdProducts.add(product);
                    log.info("Товар создан: {}", product.getName());
                } catch (Exception e) {
                    log.error("Ошибка при создании товара: {}", e.getMessage());
                    // Продолжаем обработку остальных товаров
                }
            }

            log.info("Импорт завершен. Создано товаров: {}", createdProducts.size());
            return createdProducts;

        } catch (IOException e) {
            log.error("Ошибка при чтении Excel файла: {}", e.getMessage());
            throw new RuntimeException("Ошибка при чтении Excel файла: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при импорте товаров: {}", e.getMessage());
            throw new RuntimeException("Ошибка при импорте товаров: " + e.getMessage());
        }
    }

    /**
     * Парсит Excel файл и извлекает данные в виде списка карт.
     * @param excelFile Excel файл.
     * @return Список карт с данными из Excel.
     * @throws IOException если произошла ошибка при чтении файла.
     */
    private List<Map<String, Object>> parseExcelFile(MultipartFile excelFile) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(excelFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Берем первый лист
            
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new RuntimeException("Excel файл пуст");
            }

            // Получаем заголовки из первой строки
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Не найдена строка с заголовками");
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            // Читаем данные из остальных строк
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String header = headers.get(j);
                    Object value = getCellValue(cell);
                    rowData.put(header, value);
                }
                
                // Добавляем строку только если она не пустая
                if (!rowData.values().stream().allMatch(v -> v == null || v.toString().trim().isEmpty())) {
                    data.add(rowData);
                }
            }
        }

        return data;
    }

    /**
     * Получает значение ячейки как объект.
     * @param cell Ячейка Excel.
     * @return Значение ячейки.
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * Получает значение ячейки как строку.
     * @param cell Ячейка Excel.
     * @return Строковое значение ячейки.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Использует OpenAI для маппинга данных из Excel в нужный формат.
     * @param excelData Данные из Excel.
     * @return Обработанные данные.
     */
    private List<Map<String, Object>> mapDataWithOpenAI(List<Map<String, Object>> excelData) {
        try {
            // Подготавливаем промпт для OpenAI
            String prompt = createMappingPrompt(excelData);
            
            // Отправляем запрос к OpenAI
            String response = callOpenAI(prompt);
            
            // Парсим ответ от OpenAI
            return parseOpenAIResponse(response);
            
        } catch (Exception e) {
            log.error("Ошибка при обработке данных через OpenAI: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обработке данных через OpenAI: " + e.getMessage());
        }
    }

    /**
     * Создает промпт для OpenAI на основе данных из Excel.
     * @param excelData Данные из Excel.
     * @return Промпт для OpenAI.
     */
    private String createMappingPrompt(List<Map<String, Object>> excelData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты помощник для обработки данных о товарах. ");
        prompt.append("Мне нужно преобразовать данные из Excel файла в стандартный формат для товаров.\n\n");
        prompt.append("Требуемый формат товара:\n");
        prompt.append("- name (string): название товара\n");
        prompt.append("- price (number): цена товара\n");
        prompt.append("- description (string): описание товара\n");
        prompt.append("- catalog (string): основная категория товара (например: 'Электроника', 'Одежда', 'Дом и сад')\n");
        prompt.append("- subcategory (string): подкатегория товара (например: 'Ноутбуки', 'Мужская одежда', 'Кухонная техника')\n");
        prompt.append("- imageUrl (string): URL изображения товара (если есть в данных)\n");
        prompt.append("- inStock (boolean): есть ли товар в наличии\n\n");
        
        prompt.append("Данные из Excel файла:\n");
        
        // Добавляем первые несколько строк как пример
        int sampleSize = Math.min(5, excelData.size());
        for (int i = 0; i < sampleSize; i++) {
            prompt.append("Строка ").append(i + 1).append(": ").append(excelData.get(i)).append("\n");
        }
        
        prompt.append("\nПожалуйста, проанализируй данные и верни JSON массив с товарами в требуемом формате. ");
        prompt.append("Если какое-то поле не найдено, используй разумные значения по умолчанию. ");
        prompt.append("Для цены извлеки только числовое значение. ");
        prompt.append("Для inStock используй true, если не указано иначе. ");
        prompt.append("Для catalog и subcategory постарайся определить подходящие категории на основе названия и описания товара. ");
        prompt.append("Для imageUrl используй только если в данных есть прямая ссылка на изображение (URL начинающийся с http/https). ");
        prompt.append("Если imageUrl не найден или это не URL, оставь поле пустым. ");
        prompt.append("Верни только JSON массив без дополнительного текста.");
        
        return prompt.toString();
    }

    /**
     * Отправляет запрос к OpenAI API.
     * @param prompt Промпт для OpenAI.
     * @return Ответ от OpenAI.
     */
    private String callOpenAI(String prompt) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiModel);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.1);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Ошибка при вызове OpenAI API: " + response.getStatusCode());
            }
            
            // Парсим ответ и извлекаем содержимое
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return responseJson.path("choices").get(0).path("message").path("content").asText();
            
        } catch (Exception e) {
            log.error("Ошибка при вызове OpenAI API: {}", e.getMessage());
            throw new RuntimeException("Ошибка при вызове OpenAI API: " + e.getMessage());
        }
    }

    /**
     * Парсит ответ от OpenAI и преобразует его в список карт.
     * @param response Ответ от OpenAI.
     * @return Список обработанных данных.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseOpenAIResponse(String response) {
        try {
            // Очищаем ответ от возможных markdown блоков
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();
            
            // Парсим JSON
            JsonNode jsonNode = objectMapper.readTree(cleanResponse);
            
            if (jsonNode.isArray()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (JsonNode item : jsonNode) {
                    Map<String, Object> map = objectMapper.convertValue(item, Map.class);
                    result.add(map);
                }
                return result;
            } else {
                throw new RuntimeException("Ответ от OpenAI не является JSON массивом");
            }
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге ответа от OpenAI: {}", e.getMessage());
            log.error("Ответ от OpenAI: {}", response);
            throw new RuntimeException("Ошибка при парсинге ответа от OpenAI: " + e.getMessage());
        }
    }

    /**
     * Преобразует обработанные данные в CreateProductRequest.
     * @param productData Данные товара.
     * @param botId ID бота.
     * @return CreateProductRequest.
     */
    private CreateProductRequest convertToCreateProductRequest(Map<String, Object> productData, Long botId) {
        CreateProductRequest request = new CreateProductRequest();
        
        request.setName(getStringValue(productData, "name"));
        request.setPrice(getBigDecimalValue(productData, "price"));
        request.setDescription(getStringValue(productData, "description"));
        request.setCatalog(getStringValue(productData, "catalog"));
        request.setSubcategory(getStringValue(productData, "subcategory"));
        
        // Обрабатываем imageUrl - скачиваем и загружаем в PsObjectStorageService
        String imageUrl = getStringValue(productData, "imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty() && isValidUrl(imageUrl)) {
            try {
                String uploadedImageUrl = downloadAndUploadImage(imageUrl, request.getName());
                request.setImageUrl(uploadedImageUrl);
                log.info("Изображение успешно загружено для товара '{}': {}", request.getName(), uploadedImageUrl);
            } catch (Exception e) {
                log.warn("Не удалось загрузить изображение для товара '{}' с URL '{}': {}", 
                        request.getName(), imageUrl, e.getMessage());
                request.setImageUrl(null);
            }
        } else {
            request.setImageUrl(null);
        }
        
        request.setInStock(getBooleanValue(productData, "inStock", true));
        request.setBotId(botId);
        
        return request;
    }

    /**
     * Скачивает изображение по URL и загружает его в PsObjectStorageService.
     * @param imageUrl URL изображения для скачивания.
     * @param productName Название товара (для генерации имени файла).
     * @return URL загруженного изображения в хранилище.
     * @throws Exception если произошла ошибка при скачивании или загрузке.
     */
    private String downloadAndUploadImage(String imageUrl, String productName) throws Exception {
        try {
            log.info("Скачивание изображения с URL: {}", imageUrl);
            
            // Скачиваем изображение
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                
                // Определяем тип контента на основе URL или используем по умолчанию
                String contentType = determineContentType(imageUrl);
                String filename = generateFilename(imageUrl, productName);
                
                // Создаем CustomMultipartFile для передачи в PsObjectStorageService
                MultipartFile multipartFile = new CustomMultipartFile(
                    imageBytes,
                    "image",
                    filename,
                    contentType
                );
                
                // Загружаем в хранилище
                return psObjectStorageService.uploadImage(multipartFile, productName);
            }
            
        } catch (Exception e) {
            log.error("Ошибка при скачивании/загрузке изображения с URL '{}': {}", imageUrl, e.getMessage());
            throw new Exception("Не удалось обработать изображение: " + e.getMessage());
        }
    }

    /**
     * Определяет тип контента на основе URL изображения.
     * @param imageUrl URL изображения.
     * @return MIME тип контента.
     */
    private String determineContentType(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.contains(".png")) {
            return "image/png";
        } else if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.contains(".gif")) {
            return "image/gif";
        } else if (lowerUrl.contains(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg"; // По умолчанию
        }
    }

    /**
     * Генерирует имя файла на основе URL и названия товара.
     * @param imageUrl URL изображения.
     * @param productName Название товара.
     * @return Имя файла.
     */
    private String generateFilename(String imageUrl, String productName) {
        String extension = ".jpg"; // По умолчанию
        
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.contains(".png")) {
            extension = ".png";
        } else if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
            extension = ".jpg";
        } else if (lowerUrl.contains(".gif")) {
            extension = ".gif";
        } else if (lowerUrl.contains(".webp")) {
            extension = ".webp";
        }
        
        String cleanProductName = productName.replaceAll("[^a-zA-Z0-9а-яА-Я\\s]", "")
                                            .replaceAll("\\s+", "_")
                                            .toLowerCase();
        
        return cleanProductName + "_image" + extension;
    }

    /**
     * Проверяет, является ли строка валидным URL.
     * @param url Строка для проверки.
     * @return true, если строка является валидным URL.
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String trimmedUrl = url.trim().toLowerCase();
        return trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://");
    }

    /**
     * Извлекает строковое значение из карты данных.
     * @param data Карта данных.
     * @param key Ключ.
     * @return Строковое значение.
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : "";
    }

    /**
     * Извлекает BigDecimal значение из карты данных.
     * @param data Карта данных.
     * @param key Ключ.
     * @return BigDecimal значение.
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return BigDecimal.ZERO;
        
        try {
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                String stringValue = value.toString().trim();
                // Удаляем все нечисловые символы кроме точки и запятой
                stringValue = stringValue.replaceAll("[^0-9.,]", "");
                // Заменяем запятую на точку
                stringValue = stringValue.replace(",", ".");
                return new BigDecimal(stringValue);
            }
        } catch (Exception e) {
            log.warn("Не удалось преобразовать значение '{}' в BigDecimal, используется 0", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Извлекает boolean значение из карты данных.
     * @param data Карта данных.
     * @param key Ключ.
     * @param defaultValue Значение по умолчанию.
     * @return Boolean значение.
     */
    private boolean getBooleanValue(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            String stringValue = value.toString().toLowerCase().trim();
            return "true".equals(stringValue) || "да".equals(stringValue) || "1".equals(stringValue);
        }
    }
}

