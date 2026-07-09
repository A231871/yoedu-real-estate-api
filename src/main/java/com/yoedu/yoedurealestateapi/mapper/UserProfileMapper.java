package com.yoedu.yoedurealestateapi.mapper;

import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.dto.user.UpdateProfileRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

  UserProfileResponse toResponse(User user);

  void updateProfileFromRequest(UpdateProfileRequest request, @MappingTarget User user);
}
