package br.com.temaqui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class AcessoDemonstracao implements WebMvcConfigurer {
    private final byte[] chave;

    public AcessoDemonstracao(@Value("${temaqui.demo-key}") String chave) {
        if (chave.isBlank()) {
            throw new IllegalArgumentException("Defina DEMO_KEY.");
        }
        this.chave = chave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("GET".equals(request.getMethod()) && "/api/ofertas".equals(request.getServletPath())) {
                    return true;
                }
                String informada = request.getHeader("X-Demo-Key");
                if (informada != null && MessageDigest.isEqual(chave, informada.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
                throw new RegraException(HttpStatus.UNAUTHORIZED, "Informe uma chave de demonstracao valida.");
            }
        }).addPathPatterns("/api/**");
    }
}
