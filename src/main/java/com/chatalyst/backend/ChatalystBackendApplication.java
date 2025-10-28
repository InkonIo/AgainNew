package com.chatalyst.backend;

import io.github.cdimascio.dotenv.Dotenv; // <-- НОВЫЙ ИМПОРТ
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatalystBackendApplication {

	public static void main(String[] args) {
        // --- НОВЫЙ БЛОК ДЛЯ ЧТЕНИЯ .env ---
        try {
            // Загружаем файл .env из корневой директории проекта
            Dotenv dotenv = Dotenv.load();
            
            // Переносим все переменные из .env в системные свойства (System.setProperty)
            // Spring Boot умеет читать из системных свойств, что и позволит разрешить плейсхолдеры.
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            // Добавил простую обработку, если файл .env не найден, чтобы приложение не падало
            // (например, при развертывании в облаке, где переменные заданы напрямую)
            System.err.println("WARNING: .env file not loaded. Assuming environment variables are set externally.");
        }
        // ------------------------------------
        
		SpringApplication.run(ChatalystBackendApplication.class, args);
	}

}
