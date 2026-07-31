package com.yoedu.yoedurealestateapi.service;

import org.apache.commons.lang3.tuple.Pair;
import com.yoedu.yoedurealestateapi.dto.auth.*;

public interface AuthService {

    void register(RegisterRequest request);

    Pair<AuthResponse, String> login(LoginRequest request);

    Pair<AuthResponse, String> verifyRegistration(String token);

    Pair<AuthResponse, String> refresh(RefreshRequest request);
}
