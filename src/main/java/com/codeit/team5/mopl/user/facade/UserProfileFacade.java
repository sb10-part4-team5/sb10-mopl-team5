package com.codeit.team5.mopl.user.facade;

import com.codeit.team5.mopl.binarycontent.service.UploadWithRollback;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.exception.UserForbiddenException;
import com.codeit.team5.mopl.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileFacade {

    private final UserService userService;
    private final UploadWithRollback uploadWithRollback;

    public UserResponse updateProfile(
            UUID currentUserId,
            UUID userId,
            UserUpdateRequest request,
            FileRequest image
    ) {
        if (!currentUserId.equals(userId)) {
            throw new UserForbiddenException(currentUserId, userId);
        }

        return uploadWithRollback.execute(StorageDirectory.PROFILE, image,
                uploaded -> userService.update(userId, request, uploaded));
    }
}
