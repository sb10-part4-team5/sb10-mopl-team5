package com.codeit.team5.mopl.user.mapper;

import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.user.dto.response.UserSummary;
import com.codeit.team5.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface UserSummaryMapper {

    @Mapping(target = "profileImageUrl", source = "profileImage.url")
    UserSummary toDto(User user);
}
