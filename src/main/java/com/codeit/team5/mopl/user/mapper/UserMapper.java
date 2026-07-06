package com.codeit.team5.mopl.user.mapper;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.constant.UserSortBy;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Sort.Direction;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "profileImageUrl", source = "user.profileImage.url")
    UserResponse toDto(User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "profileImageUrl", source = "profileImage.url")
    UserSummaryResponse toSummaryResponse(User user);

    default CursorResponse<UserResponse> toCursor(
            List<User> page, boolean hasNext, long totalCount, UserSortBy sortBy, Direction sortDirection
    ) {
        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            User last = page.get(page.size() - 1);
            nextCursor = switch (sortBy) {
                case EMAIL -> last.getEmail();
                case NAME -> last.getName();
                case CREATED_AT -> last.getCreatedAt().toString();
                case ROLE -> last.getRole().name();
                case LOCKED -> String.valueOf(last.isLocked());
            };

            nextIdAfter = last.getId().toString();
        }

        List<UserResponse> data = page.stream().map(this::toDto).toList();
        String direction = sortDirection == Direction.ASC ? "ASCENDING" : "DESCENDING";

        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy.getValue(), direction);
    }
}
