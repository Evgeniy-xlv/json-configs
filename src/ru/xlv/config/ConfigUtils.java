package ru.xlv.config;

import java.util.ArrayList;
import java.util.List;

class ConfigUtils {

    static List<Class<?>> getClassesHierarchy(Class<?> clazz) {
        List<Class<?>> list = new ArrayList<>();
        Class<?> c = clazz;
        while(c != null) {
            list.add(c);
            c = c.getSuperclass();
        }
        return list;
    }
}
