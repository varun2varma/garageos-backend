package com.garageos.modules.garagemembership.mapper;

import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GarageMembershipMapper {

    @Mapping(target = "garageId", source = "garage.id")
    @Mapping(target = "garageCode", source = "garage.garageCode")
    @Mapping(target = "garageName", source = "garage.garageName")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "mobile", source = "user.mobile")
    @Mapping(target = "email", source = "user.email")
    GarageMembershipResponse toResponse(GarageMembership membership);

}