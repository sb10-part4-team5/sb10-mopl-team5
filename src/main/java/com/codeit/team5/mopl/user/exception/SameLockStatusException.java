package com.codeit.team5.mopl.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class SameLockStatusException extends UserException {

    public SameLockStatusException(boolean isLocked) {
        super(HttpStatus.CONFLICT, "현재 사용자의 잠금 상태와 변경할 잠금 상태가 동일합니다.", Map.of("lockStatus", isLocked));
    }
}
