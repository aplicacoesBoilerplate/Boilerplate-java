package com.java.boilerplate.service.context;

import com.java.boilerplate.enums.MatchPolicy;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.AppContext;
import com.java.boilerplate.repository.IAppContextRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppContextService {
    public static final String HEADER_NAME = "X-App-Context";
    public static final String DEFAULT_CONTEXT_KEY = "tz";

    private final IAppContextRepository repository;
    private final CurrentAppContext currentAppContext;

    public AppContextService(IAppContextRepository repository, CurrentAppContext currentAppContext) {
        this.repository = repository;
        this.currentAppContext = currentAppContext;
    }

    @Transactional(readOnly = true)
    public AppContext resolve(String contextKey) {
        String safeContextKey = contextKey == null || contextKey.isBlank()
                ? DEFAULT_CONTEXT_KEY
                : contextKey.trim().toLowerCase();

        return repository.findById(safeContextKey)
                .orElseThrow(() -> new ExceptionsSystem(
                        "Contexto da aplicação inválido: " + safeContextKey,
                        HttpStatus.BAD_REQUEST
                ));
    }

    public AppContext getCurrent() {
        AppContext context = currentAppContext.get();
        if (context == null) {
            throw new ExceptionsSystem(
                    "Contexto da aplicação não foi resolvido para a requisição atual",
                    HttpStatus.BAD_REQUEST
            );
        }
        return context;
    }

    public String getCurrentKey() {
        return this.getCurrent().getContextKey();
    }

    public boolean currentRequiresGender() {
        return this.getCurrent().getMatchPolicy() == MatchPolicy.OPPOSITE_GENDER;
    }
}
