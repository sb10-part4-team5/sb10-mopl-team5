package com.codeit.team5.mopl.playlist.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidPlaylistNameException extends BusinessException {

    public InvalidPlaylistNameException() {

        super(HttpStatus.BAD_REQUEST, "플레이리스트의 이름이 유효하지 않습니다");
    }
}
