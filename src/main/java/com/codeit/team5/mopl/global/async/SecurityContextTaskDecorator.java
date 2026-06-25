package com.codeit.team5.mopl.global.async;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

public class SecurityContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return new DelegatingSecurityContextRunnable(runnable);
    }
}
