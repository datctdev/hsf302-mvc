package com.hsf.e_comerce.review.repository;

import com.hsf.e_comerce.review.entity.Review;
import com.hsf.e_comerce.review.valueobject.ReviewStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReviewSpecification {

    public static Specification<Review> filterForAdmin(
            String keyword,
            Integer rating,
            ReviewStatus status,
            UUID shopId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate hasComment = criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), likePattern);
                Predicate hasProductName = criteriaBuilder.like(criteriaBuilder.lower(root.get("product").get("name")), likePattern);
                Predicate hasUserEmail = criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("email")), likePattern);

                predicates.add(criteriaBuilder.or(hasComment, hasProductName, hasUserEmail));
            }

            if (rating != null) {
                predicates.add(criteriaBuilder.equal(root.get("rating"), rating));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (shopId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("product").get("shop").get("id"),
                        shopId
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}