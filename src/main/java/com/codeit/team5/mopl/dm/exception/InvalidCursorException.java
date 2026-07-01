package com.codeit.team5.mopl.dm.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidCursorException extends DmException {

    public InvalidCursorException(String cursor, String idAfter) {
        super(HttpStatus.BAD_REQUEST, "커서 형식이 올바르지 않습니다.",
                Map.of("cursor", cursor, "idAfter", idAfter));
    }
}
