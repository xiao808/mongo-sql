package com.github.xiao808.mongo.sql;

import java.util.Objects;

/**
 * @author zengxiao
 * @description An InheritableThreadLocal-based mongo context holder
 * @date 2023/3/22 16:52
 * @since 1.0
 **/
public class InheritableThreadLocalMongoContextHolder {

    /**
     * all mongo context InheritableThreadLocal
     */
    private static final ThreadLocal<MongoContext> MONGO_CONTEXT_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * get current thread mongo context.
     *
     * @return mongo context
     */
    public static MongoContext getContext() {
        MongoContext context = MONGO_CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = createEmptyContext();
            MONGO_CONTEXT_THREAD_LOCAL.set(context);
        }
        return context;
    }

    /**
     * set mongo context to current thread.
     *
     * @param context mongo context
     */
    public static void setContext(MongoContext context) {
        Objects.requireNonNull(context, "Only non-null MongoContext instances are permitted");
        MONGO_CONTEXT_THREAD_LOCAL.set(context);
    }

    /**
     * create empty mongo context.
     *
     * @return mongo context
     */
    public static MongoContext createEmptyContext() {
        return MongoContext.builder().build();
    }

    /**
     * Explicitly clears the context value from the current thread.
     */
    public static void clearContext() {
        MONGO_CONTEXT_THREAD_LOCAL.remove();
    }
}
