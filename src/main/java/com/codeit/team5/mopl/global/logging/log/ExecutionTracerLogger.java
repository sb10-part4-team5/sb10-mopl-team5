package com.codeit.team5.mopl.global.logging.log;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import com.codeit.team5.mopl.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class ExecutionTracerLogger {

    @Around("@annotation(ExecutionTracer) || @within(ExecutionTracer)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().toShortString();
        String where = String.join(".", className, methodName);
        Object[] args = joinPoint.getArgs();

        log.info("[start] {} | args: {}", where, Arrays.toString(args));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (BusinessException e) {
            log.error("{} | {}", where, e.toString());
            throw e;
        } catch (Exception e) {
            log.error("[error] {} | type: {} | message: {}", where, e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        } finally {
            stopWatch.stop();
        }
        log.info("[end] {} | time: {}ms | result: {}", where, stopWatch.getTotalTimeMillis(),
                result);
        return result;
    }
}
