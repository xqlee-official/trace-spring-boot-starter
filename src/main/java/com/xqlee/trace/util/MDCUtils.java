/*
 * Copyright 2017-2024 xqlee.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xqlee.trace.util;

import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

public class MDCUtils {

    /**
     * traceId 的key
     * 在日志文件中配置
     */
    public static final String TRACE_ID_KEY = "traceId";

    //子线程中新生成的 traceId 前缀
    private static final String NEW_TRACE_ID_KEY_PREFIX = "new_";

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 生成或获取 traceId
     * @param request request
     * @return 返回
     */
    public static String getOrGenerateTraceId(HttpServletRequest request) {

        return Optional.ofNullable(request.getHeader(TRACE_ID_KEY))
                .orElseGet(MDCUtils::generateTraceId);
    }


    public static String getOrGenerateTraceId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return getOrGenerateTraceId(request);
    }


    /**
     * 包装 Callable
     * 把主线程的 现场变量 复制到 异步线程里
     * @param callable 需要返回执行结果的任务
     * @param context 提交任务者的本地变量
     * @return 返回后
     */
    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            setMDCContextMap(context);
            try {
                return callable.call();
            } finally {//清除子线程的，避免内存溢出，就和ThreadLocal.remove()一个原因
                MDC.clear();
            }
        };
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return () -> {
            setMDCContextMap(context);
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 设置上下文MDC
     * @param context 主线程context
     */
    private static void setMDCContextMap(final Map<String, String> context) {
        if (CollectionUtils.isEmpty(context)) {
            MDC.clear();
            //如果提交任务的线程已经结束或其他情况，导致已经没有MDC变量了 则新生成要给并标识
            setTraceId(NEW_TRACE_ID_KEY_PREFIX + generateTraceId());
        } else {
            //将主线程的context设置到当前线程
            MDC.setContextMap(context);
        }
    }

    /**
     * 生成 traceId
     * @return traceId
     */
    private static String generateTraceId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "");
    }
}
