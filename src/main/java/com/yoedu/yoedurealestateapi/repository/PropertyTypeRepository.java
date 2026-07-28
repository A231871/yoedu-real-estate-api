package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyTypeRepository extends JpaRepository<PropertyType, Integer> {
    boolean existsBySlugAndIdNot(String slug, Integer id);
}
