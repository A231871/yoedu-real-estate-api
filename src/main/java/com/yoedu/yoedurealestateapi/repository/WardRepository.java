package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Ward;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByProvinceCode(String provinceCode);
}
