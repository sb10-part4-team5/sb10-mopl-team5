package com.codeit.team5.mopl.review.mapper;

import com.codeit.team5.mopl.review.dto.response.ReviewResponse;
import com.codeit.team5.mopl.review.entity.Review;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ReviewMapper {

    @Mapping(target = "author", source = "author")
    ReviewResponse toDto(Review review);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "profileImageUrl", source = "profileImage.url")
    UserSummaryResponse toAuthor(User user);
}
