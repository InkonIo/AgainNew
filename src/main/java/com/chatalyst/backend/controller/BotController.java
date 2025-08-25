package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.BotStats;
import com.chatalyst.backend.dto.CreateBotRequest;
import com.chatalyst.backend.dto.MessageResponse;
import com.chatalyst.backend.dto.UpdateBotRequest;
import com.chatalyst.backend.dto.UpdateShopNameRequest;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.security.services.BotService;
import com.chatalyst.backend.security.services.UserPrincipal;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bot Management", description = "API для управления AI-ботами")
@CrossOrigin(origins = "*", maxAge = 3600) // Убедитесь, что CORS настроен правильно
public class BotController {

    private final BotService botService;

    /**
     * Создает нового бота для текущего авторизованного пользователя.
     * @param createBotRequest Запрос на создание бота.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с сообщением об успехе или ошибке.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // Только авторизованные пользователи могут создавать ботов
    @Operation(summary = "Создать нового AI-бота", description = "Создает и настраивает нового бота для текущего пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Бот успешно создан",
                    content = @Content(schema = @Schema(implementation = Bot.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные или токен",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> createBot(@Valid @RequestBody CreateBotRequest createBotRequest,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Bot newBot = botService.createBot(createBotRequest, userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newBot);
        } catch (RuntimeException e) {
            log.error("Ошибка при создании бота для пользователя {}: {}", userPrincipal.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Получает список всех ботов, принадлежащих текущему авторизованному пользователю.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity со списком ботов.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить список ботов пользователя", description = "Возвращает все AI-боты, принадлежащие текущему пользователю.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список ботов успешно получен",
                    content = @Content(schema = @Schema(implementation = Bot.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<List<Bot>> getMyBots(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Bot> bots = botService.getBotsByUserId(userPrincipal.getId());
        return ResponseEntity.ok(bots);
    }

    /**
     * Эндпоинт для обновления имени магазина у бота.
     *
     * @param botId Идентификатор бота.
     * @param request Объект с новым именем магазина.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с сообщением об успехе.
     */
    @PutMapping("/shop-name/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Обновить имя магазина бота", description = "Обновляет имя магазина по ID бота. Только владелец может обновить имя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Имя магазина успешно обновлено",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка обновления",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> updateShopName(@PathVariable Long botId,
                                            @Valid @RequestBody UpdateShopNameRequest request,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            botService.updateShopName(botId, userPrincipal.getId(), request.getShopName());
            return ResponseEntity.ok(new MessageResponse("Имя магазина успешно обновлено!"));
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении имени магазина для бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Удаляет бота по его ID.
     * @param botId ID бота для удаления.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с сообщением об успехе.
     */
    @DeleteMapping("/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Удалить AI-бота", description = "Удаляет бота по его ID. Только владелец может удалить бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бот успешно удален",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка удаления",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deleteBot(@PathVariable Long botId,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            botService.deleteBot(botId, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse("Бот успешно удален!"));
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * PUT-метод для обновления существующего бота.
     * @param botId Идентификатор бота, который нужно обновить.
     * @param updateBotRequest Тело запроса с обновленными данными.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с обновленным объектом бота или сообщением об ошибке.
     */
    @PutMapping("/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Обновить существующего AI-бота", description = "Обновляет данные существующего бота. Доступно только владельцу бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Бот успешно обновлен",
                    content = @Content(schema = @Schema(implementation = Bot.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка, бот не найден или нет прав доступа",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> updateBot(@PathVariable Long botId,
                                       @Valid @RequestBody UpdateBotRequest updateBotRequest,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Добавлена проверка на null, чтобы избежать NullPointerException
            if (userPrincipal == null || userPrincipal.getId() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Пользователь не авторизован."));
            }
            Bot updatedBot = botService.updateBot(botId, userPrincipal.getId(), updateBotRequest);
            return ResponseEntity.ok(updatedBot);
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить статистику по боту", description = "Возвращает общее количество сообщений и диалогов для указанного бота. Доступно только владельцу бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика успешно получена",
                    content = @Content(schema = @Schema(implementation = BotStats.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка, бот не найден или нет прав доступа",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getBotStatistics(@PathVariable Long botId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            BotStats stats = botService.getBotStatistics(botId, userPrincipal.getId());
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении статистики для бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Активирует webhook для бота.
     * @param botId ID бота для активации webhook.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с сообщением об успехе или ошибке.
     */
    @PostMapping("/{botId}/webhook/activate")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Активировать webhook бота", description = "Активирует webhook для указанного бота. Доступно только владельцу бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook успешно активирован",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка активации webhook",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> activateWebhook(@PathVariable Long botId,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            botService.activateWebhook(botId, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse("Webhook успешно активирован!"));
        } catch (RuntimeException e) {
            log.error("Ошибка при активации webhook для бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Деактивирует webhook для бота.
     * @param botId ID бота для деактивации webhook.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с сообщением об успехе или ошибке.
     */
    @PostMapping("/{botId}/webhook/deactivate")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Деактивировать webhook бота", description = "Деактивирует webhook для указанного бота. Доступно только владельцу бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook успешно деактивирован",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка деактивации webhook",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> deactivateWebhook(@PathVariable Long botId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            botService.deactivateWebhook(botId, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse("Webhook успешно деактивирован!"));
        } catch (RuntimeException e) {
            log.error("Ошибка при деактивации webhook для бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Получает статус webhook для бота.
     * @param botId ID бота для проверки статуса webhook.
     * @param userPrincipal Информация об авторизованном пользователе.
     * @return ResponseEntity с информацией о статусе webhook.
     */
    @GetMapping("/{botId}/webhook/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Получить статус webhook бота", description = "Возвращает текущий статус webhook для указанного бота. Доступно только владельцу бота.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус webhook успешно получен",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения статуса webhook",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> getWebhookStatus(@PathVariable Long botId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String status = botService.getWebhookStatus(botId, userPrincipal.getId());
            return ResponseEntity.ok(new MessageResponse(status));
        } catch (RuntimeException e) {
            log.error("Ошибка при получении статуса webhook для бота {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Ошибка: " + e.getMessage()));
        }
    }
}

