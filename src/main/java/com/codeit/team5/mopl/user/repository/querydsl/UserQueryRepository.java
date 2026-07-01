package com.codeit.team5.mopl.user.repository.querydsl;

import com.codeit.team5.mopl.user.dto.request.UserCursorRequest;
import com.codeit.team5.mopl.user.entity.User;
import java.util.List;

public interface UserQueryRepository {

    List<User> findUsers(UserCursorRequest request, int fetchLimit);

    long countUsers(UserCursorRequest request);

}
