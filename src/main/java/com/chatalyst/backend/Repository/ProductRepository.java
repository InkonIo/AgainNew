package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Product;
import com.chatalyst.backend.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Найти все товары, привязанные к определенному боту
    List<Product> findByBot(Bot bot);

    // Найти товар по имени и боту
    Optional<Product> findByNameAndBot(String name, Bot bot);

    // Добавленный метод для поиска товаров по боту и подкатегории
    List<Product> findByBotAndSubcategory(Bot bot, String subcategory);

    // Найти товары по боту, каталогу, подкатегории и наличию на складе
    List<Product> findByBotAndCatalogAndSubcategoryAndInStock(Bot bot, String catalog, String subcategory, boolean inStock);

    // Найти товар по имени, боту и наличию на складе
    Optional<Product> findByNameAndBotAndInStock(String name, Bot bot, boolean inStock);

    List<Product> findByBotAndCatalog(Bot bot, String catalog);

    List<Product> findByBotAndCatalogAndSubcategory(Bot bot, String catalog, String subcategory);
    
    // Получить список уникальных каталогов, привязанных к боту.
    // Для получения уникальных значений используется @Query с оператором DISTINCT.
    @Query("SELECT DISTINCT p.catalog FROM Product p WHERE p.bot.id = :botId")
    List<String> findUniqueCatalogsByBotId(@Param("botId") Long botId);

    // Получить список уникальных подкатегорий для конкретного бота и каталога.
    // Используется @Query для уникальных значений.
    @Query("SELECT DISTINCT p.subcategory FROM Product p WHERE p.bot.id = :botId AND p.catalog = :catalog")
    List<String> findUniqueSubcategoriesByBotIdAndCatalog(@Param("botId") Long botId, @Param("catalog") String catalog);
}
