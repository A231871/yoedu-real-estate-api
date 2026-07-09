package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.location.ProvinceResponse;
import com.yoedu.yoedurealestateapi.dto.location.WardResponse;
import java.util.List;

public interface LocationService {
    List<ProvinceResponse> getProvinces();

    List<WardResponse> getExistingWardsByProvince(String provinceCode);
}
