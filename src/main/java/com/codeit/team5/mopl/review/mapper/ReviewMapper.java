package com.codeit.team5.mopl.review.mapper;

import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = {UserMapper.class}
)
public interface ReviewMapper {

    @Mapping(target = "author", source = "author")
    ReviewResponse toDto(Review review);
}
