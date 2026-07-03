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

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "contentId", source = "review.contentId")
    @Mapping(target = "text", source = "review.text")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "author", source = "user")
    ReviewResponse toDto(Review review, User user);


    @Mapping(target = "userId", source = "id")
    @Mapping(target = "profileImageUrl", source = "profileImage.url")
    UserSummaryResponse toAuthor(User user);
}
