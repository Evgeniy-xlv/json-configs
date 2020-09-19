package ru.xlv.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.Primitives;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public interface IConfigGson {

    Gson GSON = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass().getAnnotation(Configurable.class) == null && f.getAnnotation(Configurable.class) == null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    Logger LOGGER = Logger.getLogger(IConfigGson.class.getSimpleName());

    String CONFIG_VERSION_TAG = "#<config_version>%s<config_version>";
    String COMMENT_PREFIX = "<--//";

    default void load() {
        try {
            loadExceptionally();
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    default void loadExceptionally() throws NoSuchFieldException, IllegalAccessException, IOException {
        if(getConfigFile().exists()) {
            String parsedConfigVersion = null;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(getConfigFile()), StandardCharsets.UTF_8));
            StringBuilder source = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null) {
                int commentIndex = line.indexOf(COMMENT_PREFIX);
                if(commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                String prefix = "#<config_version>";
                if(line.startsWith(prefix)) {
                    parsedConfigVersion = line.substring(prefix.length(), line.length() - prefix.length() + 1);
                    continue;
                }
                source.append(line).append("\n");
            }
            bufferedReader.close();
            IConfigGson configGson;
            try {
                configGson = GSON.fromJson(source.toString(), this.getClass());
            } catch (Exception t) {
                throw new IOException("An error has occurred during loading of config: " + getConfigFile().getName(), t);
            }

            String actualConfigVersion = null;
            Version annotation = getClass().getAnnotation(Version.class);
            if (annotation != null) {
                actualConfigVersion = annotation.value();
            }

            List<Class<?>> list = ConfigUtils.getClassesHierarchy(this.getClass());
            if(configGson != null) {
                for (Class<?> aClass : list) {
                    for (Field field : aClass.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && (aClass.getAnnotation(Configurable.class) != null || field.getAnnotation(Configurable.class) != null)) {
                            Field field1 = aClass.getDeclaredField(field.getName());
                            field.setAccessible(true);
                            field1.setAccessible(true);
                            field.set(this, field1.get(configGson));
                        }
                    }
                }
            }
            if((parsedConfigVersion == null && actualConfigVersion == null) || !parsedConfigVersion.equals(actualConfigVersion)) {
                LOGGER.info("Config " + getConfigFile().getAbsolutePath() + " is outdated. It'll be refreshed.");
                save();
            }
        } else {
            LOGGER.info("Creating config " + getConfigFile().getPath());
            save();
            load();
        }
    }

    default void save() {
        if(getConfigFile().exists()) {
            if (!getConfigFile().delete()) {
                throw new RuntimeException(getConfigFile().getAbsolutePath());
            }
        }
        if(getConfigFile().getParentFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            getConfigFile().getParentFile().mkdirs();
        }
        String json = GSON.toJson(this);
        Set<Class<?>> list = new HashSet<>();
        list.add(getClass());
        int preSize = 0;
        while(preSize != list.size()) {
            preSize = list.size();
            List<Class<?>> temp = new ArrayList<>();
            for(Class<?> aClass : list) {
                for (Field declaredField : aClass.getDeclaredFields()) {
                    if (declaredField.getType().isAssignableFrom(List.class) || declaredField.getType().isAssignableFrom(Map.class)) {
                        if(declaredField.getGenericType() instanceof ParameterizedType) {
                            Type[] actualTypeArguments = ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments();
                            for (Type type : actualTypeArguments) {
                                if(type instanceof Class) {
                                    temp.add((Class<?>) type);
                                }
                            }
                        }
                    } else {
                        temp.add(declaredField.getType());
                        temp.addAll(ConfigUtils.getClassesHierarchy(declaredField.getType()));
                    }
                }
            }
            list.addAll(temp);
            list.removeIf(aClass -> aClass == File.class || aClass == Class.class || aClass == Object.class || aClass == String.class || Primitives.isPrimitive(aClass) || Map.class.isAssignableFrom(aClass) || List.class.isAssignableFrom(aClass));
        }
        Map<String, String> fieldCommentMap = new HashMap<>();
        for (Class<?> aClass : list) {
            for (Field field : aClass.getDeclaredFields()) {
                String comment = null;
                Configurable annotation = field.getAnnotation(Configurable.class);
                if (annotation == null || annotation.comment().equals("")) {
                    Comment configComment = field.getAnnotation(Comment.class);
                    if(configComment != null) {
                        if (configComment.value().length > 1 /*&& field.getType().isAssignableFrom(List.class) || field.getType().isAssignableFrom(Map.class)*/) {
                            comment = "  " + COMMENT_PREFIX + String.join("\n" + COMMENT_PREFIX, configComment.value());
                        } else {
                            comment = " " + COMMENT_PREFIX + configComment.value()[0];
                        }
                    }
                } else if(!annotation.comment().equals("")) {
                    comment = " " + COMMENT_PREFIX + annotation.comment();
                }
                if(comment != null) {
                    fieldCommentMap.put(field.getName(), comment);
                }
            }
        }
        String[] split = json.split("\n");
        for (int i = 0; i < split.length; i++) {
            for (String s : fieldCommentMap.keySet()) {
                if(split[i].contains("\"" + s + "\":")) {
                    String s1 = fieldCommentMap.get(s);
                    if(s1.contains("\n")) {
                        split[i] = s1 + "\n" + split[i];
                    } else {
                        split[i] += s1;
                    }
                    break;
                }
            }
            split[i] += "\n";
        }
        try {
            FileWriter fileWriter = new FileWriter(getConfigFile());
            String configVersion = "1.0.0";
            Version annotation = getClass().getAnnotation(Version.class);
            if (annotation != null) {
                configVersion = annotation.value();
            }
            fileWriter.write( String.format(CONFIG_VERSION_TAG, configVersion) + "\n");
            for (String s : split) {
                fileWriter.write(s);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    File getConfigFile();
}
