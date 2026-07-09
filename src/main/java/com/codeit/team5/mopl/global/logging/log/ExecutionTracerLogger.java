package com.codeit.team5.mopl.global.logging.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class ExecutionTracerLogger {

    @Around("(@annotation(ExecutionTracer) || @within(ExecutionTracer))")
    public Object logAround(ProceedingJoinPoint joinPoint, ExecutionTracer tracer)
            throws Throwable {
        String fullClassName = joinPoint.getSignature().getDeclaringTypeName();
        String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        String methodName = joinPoint.getSignature().getName();
        String where = String.join(".", className, methodName);
        boolean verbose = tracer.verbose();
        Object[] args = joinPoint.getArgs();
        String argsStr = verbose ? String.join(", ", formatArgs(args)) : "(omitted)";
        log.debug("[start] {} | args: {}", where, argsStr);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result;
        try {
            result = joinPoint.proceed();
        } finally {
            stopWatch.stop();
        }

        log.debug("[end] {} | time: {}ms | result: {}", where, stopWatch.getTotalTimeMillis(),
                result);
        return result;
    }

    private String[] formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }

        String[] formatted = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            formatted[i] = (arg == null) ? "null" : arg.toString();
        }
        return formatted;
    }
}
