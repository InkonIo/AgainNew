package com.chatalyst.backend.StripePayment.service;

import com.chatalyst.backend.StripePayment.dto.PaymentMethodRequest;
import com.chatalyst.backend.StripePayment.dto.PaymentMethodResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardManagementService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.test-customer-id}")
    private String testCustomerId;

    /**
     * Получает все карты пользователя с подробной информацией
     */
    public List<PaymentMethodResponse> getUserCards(Long userId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            String customerId = testCustomerId; // В тестовом режиме

            PaymentMethodListParams listParams = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
            
            List<PaymentMethod> paymentMethods = PaymentMethod.list(listParams).getData();
            
            // Получаем информацию о клиенте для определения карты по умолчанию
            Customer customer = Customer.retrieve(customerId);
            String defaultPaymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();
            
            return paymentMethods.stream()
                .map(pm -> PaymentMethodResponse.builder()
                    .id(pm.getId())
                    .type(pm.getType())
                    .card(PaymentMethodResponse.CardDetails.builder()
                        .brand(pm.getCard().getBrand())
                        .last4(pm.getCard().getLast4())
                        .expMonth(Math.toIntExact(pm.getCard().getExpMonth()))
                        .expYear(Math.toIntExact(pm.getCard().getExpYear()))
                        .country(pm.getCard().getCountry())
                        .funding(pm.getCard().getFunding())
                        .fingerprint(pm.getCard().getFingerprint())
                        .build())
                    .isDefault(pm.getId().equals(defaultPaymentMethodId))
                    .status("active")
                    .build())
                .collect(Collectors.toList());
                
        } catch (StripeException e) {
            log.error("Ошибка получения карт пользователя: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
 * Создает новую карту для пользователя
 */
public PaymentMethodResponse createCard(Long userId, PaymentMethodRequest request) {
    try {
        Stripe.apiKey = stripeSecretKey;

        String customerId = testCustomerId; // В тестовом режиме

        // Создаем карту через CardDetails
        PaymentMethodCreateParams.CardDetails cardDetails = PaymentMethodCreateParams.CardDetails.builder()
                .setNumber(request.getCardNumber())
                .setExpMonth(Long.parseLong(request.getExpiryMonth()))
                .setExpYear(Long.parseLong(request.getExpiryYear()))
                .setCvc(request.getCvc())
                .build();

        // Создаем PaymentMethod с типом CARD
        PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(cardDetails)
                .build();

        PaymentMethod paymentMethod = PaymentMethod.create(params);

        // Привязываем PaymentMethod к клиенту
        paymentMethod.attach(
                com.stripe.param.PaymentMethodAttachParams.builder()
                        .setCustomer(customerId)
                        .build()
        );

        // Если нужно установить как карту по умолчанию
        if (Boolean.TRUE.equals(request.getSetAsDefault())) {
            setAsDefaultCard(customerId, paymentMethod.getId());
        }

        log.info("Создана новая карта {} для пользователя {}", paymentMethod.getId(), userId);

        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .type(paymentMethod.getType())
                .card(PaymentMethodResponse.CardDetails.builder()
                        .brand(paymentMethod.getCard().getBrand())
                        .last4(paymentMethod.getCard().getLast4())
                        .expMonth(Math.toIntExact(paymentMethod.getCard().getExpMonth()))
                        .expYear(Math.toIntExact(paymentMethod.getCard().getExpYear()))
                        .country(paymentMethod.getCard().getCountry())
                        .funding(paymentMethod.getCard().getFunding())
                        .fingerprint(paymentMethod.getCard().getFingerprint())
                        .build())
                .isDefault(Boolean.TRUE.equals(request.getSetAsDefault()))
                .status("active")
                .message("Карта успешно добавлена")
                .build();

    } catch (StripeException e) {
        log.error("Ошибка создания карты: {}", e.getMessage(), e);
        return PaymentMethodResponse.builder()
                .status("failed")
                .message("Не удалось создать карту: " + e.getMessage())
                .build();
    }
}


    /**
     * Обновляет карту (например, устанавливает как карту по умолчанию)
     */
    public PaymentMethodResponse updateCard(Long userId, String paymentMethodId, PaymentMethodRequest request) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            String customerId = testCustomerId; // В тестовом режиме

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

            // Если нужно установить как карту по умолчанию
            if (Boolean.TRUE.equals(request.getSetAsDefault())) {
                setAsDefaultCard(customerId, paymentMethodId);
            }

            log.info("Обновлена карта {} для пользователя {}", paymentMethodId, userId);

            return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .type(paymentMethod.getType())
                .card(PaymentMethodResponse.CardDetails.builder()
                    .brand(paymentMethod.getCard().getBrand())
                    .last4(paymentMethod.getCard().getLast4())
                    .expMonth(Math.toIntExact(paymentMethod.getCard().getExpMonth()))
                    .expYear(Math.toIntExact(paymentMethod.getCard().getExpYear()))
                    .country(paymentMethod.getCard().getCountry())
                    .funding(paymentMethod.getCard().getFunding())
                    .fingerprint(paymentMethod.getCard().getFingerprint())
                    .build())
                .isDefault(Boolean.TRUE.equals(request.getSetAsDefault()))
                .status("updated")
                .message("Карта успешно обновлена")
                .build();

        } catch (StripeException e) {
            log.error("Ошибка обновления карты: {}", e.getMessage(), e);
            return PaymentMethodResponse.builder()
                .status("failed")
                .message("Не удалось обновить карту: " + e.getMessage())
                .build();
        }
    }

    /**
     * Удаляет карту пользователя
     */
    public boolean deleteCard(Long userId, String paymentMethodId) {
        try {
            Stripe.apiKey = stripeSecretKey;

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.detach();
            
            log.info("Удалена карта {} для пользователя {}", paymentMethodId, userId);
            return true;

        } catch (StripeException e) {
            log.error("Ошибка удаления карты: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Устанавливает карту как карту по умолчанию
     */
    private void setAsDefaultCard(String customerId, String paymentMethodId) throws StripeException {
        CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
            .setInvoiceSettings(
                CustomerUpdateParams.InvoiceSettings.builder()
                    .setDefaultPaymentMethod(paymentMethodId)
                    .build()
            )
            .build();

        Customer customer = Customer.retrieve(customerId);
        customer.update(updateParams);
        
        log.info("Карта {} установлена как основная для клиента {}", paymentMethodId, customerId);
    }

    /**
     * Проверяет, принадлежит ли карта пользователю
     */
    public boolean isCardOwnedByUser(Long userId, String paymentMethodId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            String customerId = testCustomerId; // В тестовом режиме

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            return customerId.equals(paymentMethod.getCustomer());

        } catch (StripeException e) {
            log.error("Ошибка проверки владельца карты: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Получает информацию о конкретной карте
     */
    public PaymentMethodResponse getCard(Long userId, String paymentMethodId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            String customerId = testCustomerId; // В тестовом режиме

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            
            // Проверяем, принадлежит ли карта пользователю
            if (!customerId.equals(paymentMethod.getCustomer())) {
                return PaymentMethodResponse.builder()
                    .status("failed")
                    .message("Карта не принадлежит пользователю")
                    .build();
            }

            // Получаем информацию о клиенте для определения карты по умолчанию
            Customer customer = Customer.retrieve(customerId);
            String defaultPaymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();

            return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .type(paymentMethod.getType())
                .card(PaymentMethodResponse.CardDetails.builder()
                    .brand(paymentMethod.getCard().getBrand())
                    .last4(paymentMethod.getCard().getLast4())
                    .expMonth(Math.toIntExact(paymentMethod.getCard().getExpMonth()))
                    .expYear(Math.toIntExact(paymentMethod.getCard().getExpYear()))
                    .country(paymentMethod.getCard().getCountry())
                    .funding(paymentMethod.getCard().getFunding())
                    .fingerprint(paymentMethod.getCard().getFingerprint())
                    .build())
                .isDefault(paymentMethod.getId().equals(defaultPaymentMethodId))
                .status("active")
                .build();

        } catch (StripeException e) {
            log.error("Ошибка получения информации о карте: {}", e.getMessage(), e);
            return PaymentMethodResponse.builder()
                .status("failed")
                .message("Не удалось получить информацию о карте: " + e.getMessage())
                .build();
        }
    }
}

