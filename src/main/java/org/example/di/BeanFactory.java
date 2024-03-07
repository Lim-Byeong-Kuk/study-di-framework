package org.example.di;

import org.example.annotation.Inject;
import org.example.controller.UserController;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class BeanFactory {
    private final Set<Class<?>> preInstantiatedClazz;

    // classType 을 key 로 가지고 classType으로 객체로 생성된 instance 를 value 로 가짐
    private Map<Class<?>, Object> beans = new HashMap<>();

    public BeanFactory(Set<Class<?>> preInstantiatedClazz) {
        this.preInstantiatedClazz = preInstantiatedClazz;
        initialize();
    }

    private void initialize() {
        for (Class<?> clazz : preInstantiatedClazz) {
            Object instance = createInstance(clazz);
            beans.put(clazz, instance);
        }
    }

    private Object createInstance(Class<?> clazz) {
        // 생성자
        Constructor<?> constructor = findConstructor(clazz);

        // 파라미터
        List<Object> parameters = new ArrayList<>();
        for (Class<?> typeClass : constructor.getParameterTypes()) {
            /**
             *  clazz 가 UserController 로 들어올 경우
             *  UserController 의 생성자 타입인 UserService 가 typeClass
             *  getParameterByClass(UserService); 하면
             *  createInstance(UserService)로 재귀함수 호출이 된다.
             *  UserService 는 생성자의 파라미터가 없고 밑에 try 문에서 생성된다.
             *  그리고 생성된 인스턴스가 parameters 에 셋팅된다.
             */
            parameters.add(getParameterByClass(typeClass));
        }

        // 인스턴스 생성
        try {
            return constructor.newInstance(parameters.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * clazz 타입의 클래스에서
     * @Inject 어노테이션 붙은 생성자 가져오기
     */
    private Constructor<?> findConstructor(Class<?> clazz) {
        Constructor<?> constructor = BeanFactoryUtils.getInjectedConstructor(clazz);

        if(Objects.nonNull(constructor)) {
            return constructor;
        }
        return clazz.getConstructors()[0];
    }

    /**
     *
     * instanceBean 이 null 이면 재귀함수 호출
     * ---왜?---
     *
     * createInstance(UserController) 의 과정에서
     * getParameterByCLass(typeClass) 의 typeClass로 UserService 가 들어올 경우
     * getBean(UserService) 하면 null 일 수 밖에 없다.
     * 왜냐하면 UserController 는 UserService 를 가지고있고 의존한다.
     * UserService 의 인스턴스가 먼저 만들어져야 한다.
     * 따라서 createInstance(UserService)가 먼저 호출되어야 한다.
     *
     */
    private Object getParameterByClass(Class<?> typeClass) {
        Object instanceBean = getBean(typeClass);

        if(Objects.nonNull(instanceBean)) {
            return instanceBean;
        }

        return createInstance(typeClass);
    }



    public <T> T getBean(Class<T> requiredType) {
        return (T) beans.get(requiredType);

    }
}
