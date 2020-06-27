package org.jesse.study.jinmvc.servlet;

import org.jesse.study.jinmvc.annotate.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    // bean容器
    private final Map<String, Object> beanContainer = new HashMap<>();
    // 存储到指定目录下扫描到的类
    private final List<String> classList = new ArrayList<>();
    // 存储url和Java方法的映射
    private final Map<String, Method> urlAndMethodMap = new HashMap<>();
    // 存储url和bean的映射
    private final Map<String, Object> urlAndBeanMap = new HashMap<>();

    @Override
    public void init(ServletConfig var1) throws ServletException {
        // 1、扫描指定路径，将以class结尾的类存储起来
        scanPackage("org.jesse.study.jinmvc");
        // 2、将以@Controller、@Service的类装配到容器中去
        instanceBean();
        // 3、将@Autowired存入bean中
        handleAutowired();
        // 4、Url与Method绑定
        handleUrlMapping();
        System.out.println(beanContainer);
    }

    private void scanPackage(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        String fileName = url.getFile();
        File file = new File(fileName);
        File[] files = file.listFiles();
        for (File f : files) {
            String fName = packageName + "." + f.getName();
            if (f.isDirectory()) {
                scanPackage(fName);
            } else {
                // 把class文件存储起来
                if (fName.contains(".class")) {
                    classList.add(fName);
                }
            }
        }
    }

    private void instanceBean() {
        for (String className : classList) {
            String cn = className.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(cn);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object iocValue = clazz.newInstance();
                    String iocKey = iocValue.getClass().getName();
                    beanContainer.put(iocKey, iocValue);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Service serviceAnnotation = clazz.getAnnotation(Service.class);
                    String iocKey = serviceAnnotation.value();
                    Object iocValue = clazz.newInstance();
                    beanContainer.put(iocKey, iocValue);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAutowired() {
        Collection<Object> instances = beanContainer.values();
        for (Object obj : instances) {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowiredAnnotation = field.getAnnotation(Autowired.class);
                    String beanName = autowiredAnnotation.value();
                    Object fieldValue = beanContainer.get(beanName);
                    field.setAccessible(true);
                    try {
                        field.set(obj, fieldValue);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void handleUrlMapping() {
        Collection<Object> instances = beanContainer.values();
        for (Object obj : instances) {
            Class<?> clazz = obj.getClass();
            if (clazz.isAnnotationPresent(Controller.class)) {
                String urlPathFather = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping requestMappingAnnotateion = clazz.getAnnotation(RequestMapping.class);
                    urlPathFather = requestMappingAnnotateion.value();
                }

                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping requestMappingAnnotateion = method.getAnnotation(RequestMapping.class);
                        String urlPath = urlPathFather + requestMappingAnnotateion.value();
                        // 将url与method绑定
                        urlAndMethodMap.put(urlPath, method);
                        // 将url与instance绑定
                        urlAndBeanMap.put(urlPath, obj);
                    }
                }
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String context = req.getContextPath();
        String uri = req.getRequestURI();
        String url = uri.replace(context, "");

        Method method = urlAndMethodMap.get(url);
        Object bean = urlAndBeanMap.get(url);
        Object result = null;
        Object[] args = buildMethodArgs(req, resp, method);
        try {
            result = method.invoke(bean, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        resp.setContentType("text/html");
        PrintWriter printWriter = resp.getWriter();
        printWriter.println(result);
    }

    private Object[] buildMethodArgs(HttpServletRequest req, HttpServletResponse resp, Method method) {
        Class<?>[] paramClazzs = method.getParameterTypes();
        Object[] args = new Object[paramClazzs.length];

        int i = 0;
        int annoIndex = 0;
        for (Class<?> clazz : paramClazzs) {
            if (ServletRequest.class.isAssignableFrom(clazz)) {
                args[i++] = req;
            } else if (ServletResponse.class.isAssignableFrom(clazz)) {
                args[i++] = resp;
            } else {
                Annotation[] annotations = method.getParameterAnnotations()[annoIndex++];   // 每一个参数都可能会有多个注解，所以这里是二维数组
                for (Annotation annotation:annotations){
                    if (RequestParam.class.isAssignableFrom(annotation.getClass())) {
                        RequestParam requestParam = (RequestParam) annotation;
                        args[i++] = req.getParameter(requestParam.value());
                    }
                }
            }
        }
        return args;
    }

}
