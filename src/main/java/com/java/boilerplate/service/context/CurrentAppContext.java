package com.java.boilerplate.service.context;

import com.java.boilerplate.model.AppContext;
import org.springframework.stereotype.Component;

@Component
public class CurrentAppContext {
    private static final ThreadLocal<AppContext> CURRENT = new ThreadLocal<>();

    public void set(AppContext context) {
        CURRENT.set(context);
    }

    public AppContext get() {
        return CURRENT.get();
    }

    public String getKey() {
        AppContext context = this.get();
        return context == null ? null : context.getContextKey();
    }

    public void clear() {
        CURRENT.remove();
    }
}
