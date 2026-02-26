package com.coinguard.transaction.specification;

import com.coinguard.transaction.dto.request.TransactionFilterRequest;
import com.coinguard.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> filterTransactions(Long userId, TransactionFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User must be either sender or receiver
            Predicate senderPredicate = criteriaBuilder.equal(root.get("fromWallet").get("user").get("id"), userId);
            Predicate receiverPredicate = criteriaBuilder.equal(root.get("toWallet").get("user").get("id"), userId);
            predicates.add(criteriaBuilder.or(senderPredicate, receiverPredicate));

            // Filter by type
            if (filter.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), filter.type()));
            }

            // Filter by status
            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }

            // Filter by category
            if (filter.category() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), filter.category()));
            }

            // Filter by currency
            if (filter.currency() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), filter.currency()));
            }

            // Filter by amount range
            if (filter.minAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
            }

            // Filter by date range
            if (filter.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.endDate()));
            }

            // Search in description, reference, sender/receiver names
            if (filter.searchQuery() != null && !filter.searchQuery().isBlank()) {
                String searchPattern = "%" + filter.searchQuery().toLowerCase() + "%";

                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern
                );
                Predicate referencePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("referenceNo")), searchPattern
                );
                Predicate senderNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("fromWallet").get("user").get("firstName")), searchPattern
                );
                Predicate receiverNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("toWallet").get("user").get("firstName")), searchPattern
                );

                predicates.add(criteriaBuilder.or(
                    descriptionPredicate,
                    referencePredicate,
                    senderNamePredicate,
                    receiverNamePredicate
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
