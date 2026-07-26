package com.maddiewest.events.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Copies the submitting thread's MDC (notably the request correlation id) onto the
 * async executor's thread, so logs from @Async work (e.g. email sending) can still be
 * traced back to the HTTP request that triggered them.
 */
class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
