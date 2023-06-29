package com.microservice.user.repository;

import com.microservice.user.model.QUser;
import com.microservice.user.model.User;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.SingleValueBinding;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<User, String>, QuerydslPredicateExecutor<User>, QuerydslBinderCustomizer<QUser>, CrudRepository<User, String> {
    User findByEmail(String email);

    Page<User> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate);

    @Override
    default void customize(QuerydslBindings bindings, QUser root) {
        // Make case-insensitive 'like' filter for all string properties
        bindings.bind(String.class)
                .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
    }
}
