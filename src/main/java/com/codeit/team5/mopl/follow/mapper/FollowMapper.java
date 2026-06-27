package com.codeit.team5.mopl.follow.mapper;

import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface FollowMapper {

    @Mapping(target = "followerId", source = "follower.id")
    @Mapping(target = "followeeId", source = "followee.id")
    FollowResponse toDto(Follow follow);
}
