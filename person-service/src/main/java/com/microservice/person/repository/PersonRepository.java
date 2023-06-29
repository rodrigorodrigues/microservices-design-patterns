package com.microservice.person.repository;

import com.microservice.person.model.Person;
import com.microservice.person.model.QPerson;
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
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, String>, QuerydslPredicateExecutor<Person>, QuerydslBinderCustomizer<QPerson>, CrudRepository<Person, String> {
    Page<Person> findAllByFullNameIgnoreCaseStartingWith(String name, Pageable pageable, Predicate predicate);

    Page<Person> findByChildrenExists(boolean exists, Pageable pageable, Predicate predicate);

    Page<Person> findAllByCreatedByUser(String createdByUser, Pageable pageable, Predicate predicate);

    @Override
    default void customize(QuerydslBindings bindings, QPerson root) {
        // Make case-insensitive 'like' filter for all string properties
/*
        bindings.bind(root.fullName, root.createdByUser, root.id, root.lastModifiedByUser, root.address.address, root.address.city)
            .all((path, values) -> {
                BooleanBuilder predicate = new BooleanBuilder();
                values.forEach(value -> predicate.or(path.containsIgnoreCase(value)));
                return Optional.of(predicate);
            });
*/
        bindings.bind(String.class)
            .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
/*
        bindings.bind(Instant.class)
            .first((SingleValueBinding<DateTimePath<Instant>, Instant>) DateTimeExpression::eq);
*/
    }
}
