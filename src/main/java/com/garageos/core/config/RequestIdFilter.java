package com.garageos.core.config;

import com.garageos.core.util.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String requestId = UUID.randomUUID().toString();

            RequestContext.setRequestId(requestId);

            response.setHeader("X-Request-Id", requestId);

            filterChain.doFilter(request, response);

        } finally {

            RequestContext.clear();

        }
    }
}