package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.location.ProvinceResponse;
import com.yoedu.yoedurealestateapi.dto.location.WardResponse;
import com.yoedu.yoedurealestateapi.service.LocationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceResponse>> getProvinces() {
        return ApiResponse.success(locationService.getProvinces());
    }

    @GetMapping("/wards/{provinceCode}")
    public ApiResponse<List<WardResponse>> getWards(
        @PathVariable String provinceCode
    ) {
        return ApiResponse.success(
            locationService.getExistingWardsByProvince(provinceCode)
        );
    }
}
