package com.chatalyst.backend.Support.Repository;

import com.chatalyst.backend.Support.Entity.MessagePriority;
import com.chatalyst.backend.Support.Entity.MessageStatus;
import com.chatalyst.backend.Support.Entity.SupportMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SupportMessageRepositoryCustomImpl implements SupportMessageRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<SupportMessage> findWithAdvancedFilters(
            MessageStatus status,
            MessagePriority priority,
            Long adminId,
            String search,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String sortBy,
            String sortDirection) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SupportMessage> cq = cb.createQuery(SupportMessage.class);
        Root<SupportMessage> root = cq.from(SupportMessage.class);

        List<Predicate> predicates = new ArrayList<>();
        // === ВАЖНО: Фильтруем только неархивные сообщения для админов ===
        predicates.add(cb.equal(root.get("archived"), false));

        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            predicates.add(cb.equal(root.get("priority"), priority));
        }
        if (adminId != null) {
            predicates.add(cb.equal(root.get("admin").get("id"), adminId));
        }
        if (StringUtils.hasText(search)) {
            String likePattern = "%" + search.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("subject")), likePattern),
                    cb.like(cb.lower(root.get("message")), likePattern),
                    cb.like(cb.lower(root.join("user").get("email")), likePattern)
            ));
        }
        if (dateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        // Сортировка
        if (StringUtils.hasText(sortBy)) {
            Path<?> sortPath;
            if ("user.email".equalsIgnoreCase(sortBy)) {
                sortPath = root.join("user").get("email");
            } else {
                sortPath = root.get(sortBy);
            }
            if ("desc".equalsIgnoreCase(sortDirection)) {
                cq.orderBy(cb.desc(sortPath));
            } else {
                cq.orderBy(cb.asc(sortPath));
            }
        } else {
            cq.orderBy(cb.desc(root.get("createdAt"))); // default
        }

        TypedQuery<SupportMessage> query = em.createQuery(cq);
        return query.getResultList();
    }
}

