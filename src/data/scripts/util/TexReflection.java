package data.scripts.util;

import java.lang.invoke.MethodHandles;
import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import java.util.jar.JarEntry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.combat.entities.terrain.Planet;

import java.util.*;

public class TexReflection {
    private static final Logger logger = Logger.getLogger(TexReflection.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static List<Class<?>> getAllObfClasses(String jarName) {
        try {
            JarFile jarFile = new JarFile(jarName);
            Enumeration<JarEntry> entries = jarFile.entries();
            List<Class<?>> obfClasses = new ArrayList<>();
    
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
    
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    String className = name.replace("/", ".").substring(0, name.length() - ".class".length());
                    obfClasses.add(Class.forName(className, false, Global.class.getClassLoader()));
                }
            }
    
            jarFile.close();
            return obfClasses;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final Class<?> fieldClass;
    private static final Class<?> constructorClass;
    private static final Class<?> typeArrayClass;

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    
    private static final MethodHandle setConstructorAccessibleHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;
    private static final MethodHandle constructorNewInstanceHandle;
    private static final MethodHandle getConstructorGenericParameterTypesHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));

            typeArrayClass = Class.forName("[Ljava.lang.reflect.Type;", false, Class.class.getClassLoader());
            getConstructorGenericParameterTypesHandle = lookup.findVirtual(constructorClass, "getGenericParameterTypes", MethodType.methodType(typeArrayClass));

            setConstructorAccessibleHandle = lookup.findVirtual(constructorClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getConstructorParameterTypesHandle = lookup.findVirtual(constructorClass, "getParameterTypes", MethodType.methodType(Class[].class));
            constructorNewInstanceHandle = lookup.findVirtual(constructorClass, "newInstance", MethodType.methodType(Object.class, Object[].class));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object instantiateClass(String canonicalName, Class<?>[] paramTypes, Object... params) {
        try {
            Class<?> clazz = Class.forName(canonicalName, false, Class.class.getClassLoader());
            Object ctor = clazz.getDeclaredConstructor(paramTypes);
            setConstructorAccessibleHandle.invoke(ctor, true);
            return constructorNewInstanceHandle.invoke(ctor, params);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Class<?>[]> getConstructorParamTypes(Class<?> cls) {
        Object[] ctors = cls.getDeclaredConstructors();
        List<Class<?>[]> lst = new ArrayList<>();

        try {
            for (Object ctor : ctors) {
                Class<?>[] ctorParams = (Class<?>[]) getConstructorParameterTypesHandle.invoke(ctor);
                lst.add(ctorParams);
            }
            return lst;

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getFieldType(Object field) {
        try {
            return (Class<?>) getFieldTypeHandle.invoke(field);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static String getFieldName(Object field) {
        try {
            return (String) getFieldNameHandle.invoke(field);
        } catch (Throwable e) {
            print(e);
            return null;
        }
    }

    public static Object texClassCtor;
    public static Object campaignPlanetGraphicsField;
    public static Object planetTexField;
    public static Object planetCloudTexField;
    public static Object planetShieldTexField;
    public static Object planetShieldTex2Field;
    public static Object planetAtmosphereTexField;
    public static Object planetGlowTexField;

    static {
        try {
            List<Class<?>> obfClasses = getAllObfClasses("fs.common_obf.jar");
            for (Class<?> cls : obfClasses) {
                Object[] ctors = cls.getDeclaredConstructors();
                if (ctors.length < 1) continue;

                Object ctor = ctors[0];
                Class<?>[] ctorParams = (Class<?>[]) getConstructorParameterTypesHandle.invoke(ctor);
                if (ctorParams.length == 2 && ctorParams[0] != int.class && ctorParams[1] != int.class) continue;

                Object[] fields = cls.getDeclaredFields();
                int colorCount = 0;
                int intCount = 0;
                int stringCount = 0;

                for (Object field : fields) {
                    Class<?> fieldType = getFieldType(field);

                    if (fieldType.equals(Color.class)) colorCount++;
                    else if (fieldType.equals(int.class)) intCount++;
                    else if (fieldType.equals(String.class)) stringCount++;
                }

                if (colorCount == 3 && intCount == 8 && stringCount == 2)  {
                    texClassCtor = ctor;
                    break;
                }
            }

            obfClasses = getAllObfClasses("starfarer_obf.jar");

            outer:
            for (Class<?> cls : obfClasses) {
                if (cls.getSimpleName().equals("CampaignPlanet")) {
                    for (Object field : cls.getDeclaredFields()) {
                        if (getFieldName(field).equals("graphics")) {
                            campaignPlanetGraphicsField = field;
                            setFieldAccessibleHandle.invoke(field, true);
                            break outer;
                            
                        }
                    }
                }
            }

            for (Object field : Planet.class.getDeclaredFields()) {
                switch (getFieldName(field)) {
                    case "planetTex":
                        planetTexField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "cloudTex":
                        planetCloudTexField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "shieldTex":
                        planetShieldTexField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;

                    case "shieldTex2":
                        planetShieldTex2Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "atmosphereTex":
                        planetAtmosphereTexField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "glowTex":
                        planetGlowTexField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    default:
                        continue;
                }
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateVariable(Object field, Object instanceToModify, Object newValue) {
        try {
            setFieldAccessibleHandle.invoke(field, true);
            setFieldHandle.invoke(field, instanceToModify, newValue);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateVariable(String fieldName, Object instanceToModify, Object newValue) {
        try {
            for (Object obj : instanceToModify.getClass().getDeclaredFields()) {
                setFieldAccessibleHandle.invoke(obj, true);
                String name = (String) getFieldNameHandle.invoke(obj);
                if (name.equals(fieldName)) {
                    setFieldHandle.invoke(obj, instanceToModify, newValue);
                    return;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static Object getPrivateVariable(Object field, Object instanceToGetFrom) {
        try {
            setFieldAccessibleHandle.invoke(field, true);
            return getFieldHandle.invoke(field, instanceToGetFrom);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Planet getPlanetFromCampaignPlanet(PlanetAPI campaignPlanet) {
        try {
            return (Planet) getFieldHandle.invoke(campaignPlanetGraphicsField, campaignPlanet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPlanetTex(Planet planet) {
        try {
            return getFieldHandle.invoke(planetTexField, planet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPlanetTex(Planet planet, Object texObj) {
        try {
            setFieldHandle.invoke(planetTexField, planet, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object instantiateTexObj(int glBindType, int texId) {
        try {
            return constructorNewInstanceHandle.invoke(texClassCtor, glBindType, texId);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {}
}
