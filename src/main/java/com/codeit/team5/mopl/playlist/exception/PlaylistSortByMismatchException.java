package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PlaylistSortByMismatchException extends BusinessException {

    public PlaylistSortByMismatchException(String sortByType, String cursor) {
        super(HttpStatus.BAD_REQUEST,
                "정렬 기준과 커서 타입이 일치하지 않습니다. (허용 포맷: updatedAt -> 예: 2026-07-01T10:00:00Z, subscribeCount -> 숫자)",
                Map.of("sortBy", sortByType, "cursor", cursor));
    }
}
