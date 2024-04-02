package com.admin4j.framework.security.configuration;

import com.admin4j.framework.security.ISecurityIgnoringUrl;
import com.admin4j.framework.security.authorization.PermissionAuthorizationManager;
import com.admin4j.framework.security.filter.ActuatorFilter;
import com.admin4j.framework.security.ignoringUrl.AnonymousAccessUrl;
import com.admin4j.framework.security.multi.MultiSecurityConfigurerAdapter;
import com.admin4j.framework.security.properties.FormLoginProperties;
import com.admin4j.framework.security.properties.IgnoringUrlProperties;
import com.admin4j.framework.security.properties.JwtProperties;
import com.admin4j.framework.security.properties.MultiAuthenticationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.Map;

/**
 * TODO 需要注入，取消 UserDetailsServiceAutoConfiguration 开启
 * 		value = { PermissionAuthorizationManager.class, AuthenticationProvider.class, UserDetailsService.class,
 * 				AuthenticationManagerResolver.class },
 *
 * @author andanyang
 * @since 2023/3/24 16:34
 */

/**
 * spring security 配置。
 *
 * @author andanyang
 * @EnableGlobalMethodSecurity 有应用出自己开启
 */
// @EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({IgnoringUrlProperties.class, JwtProperties.class, FormLoginProperties.class, MultiAuthenticationProperties.class})
@AutoConfigureBefore(UserDetailsServiceAutoConfiguration.class)
public class SecurityConfiguration {


    @Autowired(required = false)
    List<ISecurityIgnoringUrl> securityIgnoringUrls;
    @Autowired(required = false)
    IgnoringUrlProperties ignoringUrlProperties;
    @Autowired
    FormLoginProperties formLoginProperties;
    @Autowired
    AuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    AccessDeniedHandler accessDeniedHandler;
    @Autowired
    AuthenticationSuccessHandler authenticationSuccessHandler;
    @Autowired
    AuthenticationFailureHandler authenticationFailureHandler;
    @Autowired
    AnonymousAccessUrl anonymousAccessUrl;
    @Autowired
    LogoutSuccessHandler logoutSuccessHandler;
    @Autowired(required = false)
    ActuatorFilter actuatorFilter;
    @Autowired(required = false)
    CorsFilter corsFilter;
    @Autowired(required = false)
    MultiSecurityConfigurerAdapter multiSecurityConfigurerAdapter;
    @Autowired(required = false)
    PermissionAuthorizationManager permissionAuthorizationManager;
    /**
     * 取消ROLE_前缀
     */
    //@Bean
    // public GrantedAuthorityDefaults grantedAuthorityDefaults() {
    //    // Remove the ROLE_ prefix
    //    return new GrantedAuthorityDefaults("");
    //}

    // /**
    //  * 设置中文配置
    //  */
    // @Bean
    // public ReloadableResourceBundleMessageSource messageSource() {
    //     ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    //     messageSource.setBasename("classpath:org/springframework/security/messages_zh_CN");
    //     return messageSource;
    // }

    /**
     * 安全配置
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        // FilterSecurityInterceptor
        httpSecurity
                // 关闭cors
                .cors().disable()
                // CSRF禁用，因为不使用session
                .csrf().disable()
                // 禁用HTTP响应标头
                .headers()
                .cacheControl().disable()
                .frameOptions().disable()
                .and()
                // 基于token，所以不需要session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 认证失败处理类 异常处理
                .exceptionHandling()
                // 未登录或登录过期
                .authenticationEntryPoint(authenticationEntryPoint)
                // 没有权限访问时
                .accessDeniedHandler(accessDeniedHandler)
        ;

        // 添加Logout filter
        httpSecurity.logout().logoutUrl(formLoginProperties.getLogOutProcessingUrl()).permitAll().logoutSuccessHandler(logoutSuccessHandler);


        // 添加CORS filter
        if (corsFilter != null) {
            httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);
        }

        if (actuatorFilter != null) {
            httpSecurity.addFilterAfter(actuatorFilter, LogoutFilter.class);
        }

        // 多渠道登录
        if (multiSecurityConfigurerAdapter != null) {
            httpSecurity.apply(multiSecurityConfigurerAdapter);
        } else if (formLoginProperties.isEnable()) {
            // 开启form表单认证
            httpSecurity.formLogin()
                    .loginProcessingUrl(formLoginProperties.getLoginProcessingUrl())
                    .passwordParameter(formLoginProperties.getPasswordParameter())
                    .usernameParameter(formLoginProperties.getUsernameParameter())
                    .failureHandler(authenticationFailureHandler)
                    .successHandler(authenticationSuccessHandler)
                    .permitAll();
        }

        // 授权请求配置 authorizeHttpRequests(6.0 新版) authorizeRequests（旧版） 区别
        // httpSecurity.authorizeRequests().anyRequest().authenticated();
        httpSecurity.authorizeHttpRequests(register -> {

            // 忽略URl配置
            ignoringRequestMatcherRegistry(register);
            if (permissionAuthorizationManager != null) {
                // 自定义授权
                register.anyRequest().access(permissionAuthorizationManager);
            } else {
                // 除上面外的所有请求全部需要鉴权认证;其他路径必须验证
                register.anyRequest().authenticated();
            }

        });

        return httpSecurity.build();
    }


    /**
     * 忽略URl配置
     */
    private void ignoringRequestMatcherRegistry(AbstractRequestMatcherRegistry<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> matcherRegistry) {


        if (securityIgnoringUrls != null && !securityIgnoringUrls.isEmpty()) {
            securityIgnoringUrls.forEach(url -> {

                if (url.ignoringUrls() == null || url.ignoringUrls().length == 0) {
                    return;
                }

                if (url.support() == null) {
                    matcherRegistry.mvcMatchers(url.ignoringUrls()).permitAll();
                } else {
                    matcherRegistry.antMatchers(url.support(), url.ignoringUrls()).permitAll();
                }
            });
        }

        if (ignoringUrlProperties != null) {

            if (ignoringUrlProperties.getUris() != null && ignoringUrlProperties.getUris().length > 0) {
                matcherRegistry.antMatchers(ignoringUrlProperties.getUris()).permitAll();
            }
            if (ignoringUrlProperties.getGet() != null && ignoringUrlProperties.getGet().length > 0) {
                matcherRegistry.antMatchers(HttpMethod.GET, ignoringUrlProperties.getGet()).permitAll();
            }

            if (ignoringUrlProperties.getPost() != null && ignoringUrlProperties.getPost().length > 0) {
                matcherRegistry.antMatchers(HttpMethod.POST, ignoringUrlProperties.getPost()).permitAll();
            }
            if (ignoringUrlProperties.getPut() != null && ignoringUrlProperties.getPut().length > 0) {
                matcherRegistry.antMatchers(HttpMethod.PUT, ignoringUrlProperties.getPut()).permitAll();
            }
            if (ignoringUrlProperties.getPatch() != null && ignoringUrlProperties.getPatch().length > 0) {
                matcherRegistry.antMatchers(HttpMethod.PATCH, ignoringUrlProperties.getPatch()).permitAll();
            }
            if (ignoringUrlProperties.getDelete() != null && ignoringUrlProperties.getDelete().length > 0) {
                matcherRegistry.antMatchers(HttpMethod.DELETE, ignoringUrlProperties.getDelete()).permitAll();
            }
        }

        // AnonymousAccess 注解
        if (anonymousAccessUrl != null) {

            Map<HttpMethod, String[]> anonymousUrl = anonymousAccessUrl.getAnonymousUrl();
            anonymousUrl.keySet().forEach(i -> {

                matcherRegistry.antMatchers(i, anonymousUrl.get(i)).permitAll();
            });
        }
    }
}
