package com.qubar.server.interceptor;

import com.qubar.server.pojo.User;
import com.qubar.server.service.UserService;
import com.qubar.server.utils.NoAuthorization;
import com.qubar.server.utils.UserThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 判断请求的方法是否包含了 NoAuthorization注解，如果包含了，就不需要做token验证处理
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            NoAuthorization annotation = handlerMethod.getMethod().getAnnotation(NoAuthorization.class);
            if (null != annotation) {
                return true;
            }
        }

        String token = request.getHeader("Authorization");
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            response.setStatus(401); //无权限
            return false;
        }

        // 储存到当前的线程中
        UserThreadLocal.set(user);

        return true;
    }
}
