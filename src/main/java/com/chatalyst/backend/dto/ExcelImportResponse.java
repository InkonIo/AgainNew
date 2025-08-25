// src/main/java/com/chatalyst/backend/dto/ExcelImportResponse.java
package com.chatalyst.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResponse {
    private int totalProcessed;
    private int successfullyCreated;
    private int failed;
    private List<ProductResponse> createdProducts;
    private List<String> errors;
    private String message;
}

