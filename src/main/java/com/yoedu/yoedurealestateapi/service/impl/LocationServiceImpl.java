package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.Province;
import com.yoedu.yoedurealestateapi.domain.entities.Ward;
import com.yoedu.yoedurealestateapi.dto.location.ProvinceResponse;
import com.yoedu.yoedurealestateapi.dto.location.WardResponse;
import com.yoedu.yoedurealestateapi.repository.ProvinceRepository;
import com.yoedu.yoedurealestateapi.repository.WardRepository;
import com.yoedu.yoedurealestateapi.service.LocationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    // Utils
    private ProvinceResponse toProvinceResponse(Province province) {
        return new ProvinceResponse(province.getCode(), province.getName());
    }

    private WardResponse toWardResponse(Ward ward) {
        return new WardResponse(ward.getCode(), ward.getName());
    }

    // Service methods
    @Override
    public List<ProvinceResponse> getProvinces() {
        return provinceRepository
            .findAll()
            .stream()
            .map(this::toProvinceResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getExistingWardsByProvince(String provinceCode) {
        return wardRepository
            .findByProvinceCode(provinceCode)
            .stream()
            .map(this::toWardResponse)
            .collect(Collectors.toList());
    }
}
