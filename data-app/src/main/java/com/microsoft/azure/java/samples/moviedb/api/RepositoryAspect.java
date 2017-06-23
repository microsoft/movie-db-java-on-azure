package com.microsoft.azure.java.samples.moviedb.api;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryAspect {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Pointcut("execution(public * org.springframework.data.repository.Repository+.*(..))")
    public void monitor() {
    }

    @Around("monitor()")
    public Object profile(ProceedingJoinPoint pjp) {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - start;
            logger.info(pjp.getTarget() + "---" + pjp.getSignature() + "---" + elapsedTime + " ms");
        }
        return null;
    }
}
