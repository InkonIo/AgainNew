package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.Repository.DeliveryDetailsRepository;
import com.chatalyst.backend.dto.DeliveryDetailsRequest;
import com.chatalyst.backend.dto.DeliveryDetailsResponse;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.DeliveryDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryDetailsService {

    private final DeliveryDetailsRepository deliveryDetailsRepository;
    private final BotRepository botRepository;

    /**
     * Создает или обновляет детали доставки для бота.
     * @param request DTO с данными.
     * @param userId ID пользователя, владельца бота.
     * @return Обновленный или созданный DeliveryDetailsResponse.
     */
    @Transactional
    public DeliveryDetailsResponse createOrUpdateDeliveryDetails(DeliveryDetailsRequest request, Long userId) {
        Bot bot = botRepository.findById(request.getBotId())
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + request.getBotId()));

        // Проверка прав
        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для изменения настроек этого бота.");
        }

        DeliveryDetails details = deliveryDetailsRepository.findByBot(bot)
                .orElseGet(DeliveryDetails::new);
        
        details.setBot(bot);
        details.setPickupAddress(request.getPickupAddress());
        details.setContactPhone(request.getContactPhone());
        details.setWhatsappLink(request.getWhatsappLink());
        details.setOtherSocialMediaLink(request.getOtherSocialMediaLink());
        details.setAdditionalInfo(request.getAdditionalInfo());

        DeliveryDetails savedDetails = deliveryDetailsRepository.save(details);
        return convertToResponse(savedDetails);
    }

    /**
     * Получает детали доставки для бота.
     * @param botId ID бота.
     * @return DeliveryDetailsResponse.
     */
    public DeliveryDetailsResponse getDeliveryDetailsByBotId(Long botId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        DeliveryDetails details = deliveryDetailsRepository.findByBot(bot)
                .orElse(null);
        
        return details != null ? convertToResponse(details) : new DeliveryDetailsResponse();
    }

    private DeliveryDetailsResponse convertToResponse(DeliveryDetails details) {
        DeliveryDetailsResponse response = new DeliveryDetailsResponse();
        response.setId(details.getId());
        response.setBotId(details.getBot().getId());
        response.setPickupAddress(details.getPickupAddress());
        response.setContactPhone(details.getContactPhone());
        response.setWhatsappLink(details.getWhatsappLink());
        response.setOtherSocialMediaLink(details.getOtherSocialMediaLink());
        response.setAdditionalInfo(details.getAdditionalInfo());
        return response;
    }
}
