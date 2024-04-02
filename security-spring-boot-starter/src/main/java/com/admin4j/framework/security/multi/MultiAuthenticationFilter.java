package com.admin4j.framework.security.multi;

import com.admin4j.framework.security.properties.FormLoginProperties;
import com.admin4j.framework.security.properties.MultiAuthenticationProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 多渠道过滤器
 *
 * @author andanyang
 * @since 2023/6/1 15:49
 */

public class MultiAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    static final String DEFAULT_AUTH_TYPE = "";
    private final MultiAuthenticationProperties multiAuthenticationProperties;
    private final FormLoginProperties formLoginProperties;


    public MultiAuthenticationFilter(MultiAuthenticationProperties multiAuthenticationProperties, FormLoginProperties formLoginProperties) {

        super(multiAuthenticationProperties.getLoginProcessingUrlPrefix() + (
                StringUtils.endsWith(multiAuthenticationProperties.getLoginProcessingUrlPrefix(), "/")
                        ? "**" : "/**"
        ));
        this.multiAuthenticationProperties = multiAuthenticationProperties;
        this.formLoginProperties = formLoginProperties;
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {

        if (multiAuthenticationProperties.isPostOnly() && !"POST".equals(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        MultiAuthenticationToken token = obtainToken(request);
        setDetails(request, token);
        // 匹配成功交给 PermissionAuthorizationManager 去认证
        return this.getAuthenticationManager().authenticate(token);
    }

    /**
     * 获取未认证的令牌
     *
     * @param request
     * @return
     */
    protected MultiAuthenticationToken obtainToken(HttpServletRequest request) {

        /**
         * 获取授权方式
         */
        String authType = request.getParameter("authType");

        if (StringUtils.isBlank(authType)) {

            // 尝试去uri路径里面获取 /login/phone
            String requestURI = request.getRequestURI();
            authType = StringUtils.substringAfter(requestURI, multiAuthenticationProperties.getLoginProcessingUrlPrefix());
        }

        String principal;
        if (StringUtils.isBlank(authType)) {
            // 默认开启了formLogin 获取默认的 username字段
            authType = DEFAULT_AUTH_TYPE;
            principal = request.getParameter(formLoginProperties.getUsernameParameter());
        } else {
            String field = multiAuthenticationProperties.getAuthMap() == null ? authType : multiAuthenticationProperties.getAuthMap().getOrDefault(authType, authType);
            principal = request.getParameter(field);
        }

        return MultiAuthenticationToken.unauthenticated(authType, principal, request.getParameterMap());
    }

    protected void setDetails(HttpServletRequest request,
                              MultiAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }
}
