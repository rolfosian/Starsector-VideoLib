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
import com.fs.starfarer.campaign.CampaignPlanet;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.graphics.util.GLListManager;
import com.fs.graphics.util.GLListManager.GLListToken;

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

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static final Class<?> fieldClass;
    private static final Class<?> constructorClass;

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    
    private static final MethodHandle setConstructorAccessibleHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;
    private static final MethodHandle constructorNewInstanceHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));

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

    public static List<Object> getAllVariables(Object instanceToGetFrom) {
        List<Object> lst = new ArrayList<>();
        Class<?> currentClass = instanceToGetFrom.getClass();
        while (currentClass != null) {
            for (Object field : currentClass.getDeclaredFields()) {
                lst.add(getPrivateVariable(field, instanceToGetFrom));
            }
            currentClass = currentClass.getSuperclass();
        }
        return lst;
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

    public static Object planetListToken1Field;
    public static Object planetListToken2Field;
    public static Object planetListToken3Field;
    public static Object planetListToken4Field;
    public static Object planetListToken5Field;

    public static Object campaignPlanetGraphicsField;
    public static Object texClassCtor;
    public static Object texObjectIdField;

    public static class PlanetTexType {
        public static Object PLANET;
        public static Object CLOUD;
        public static Object SHIELD;
        public static Object SHIELD2;
        public static Object ATMOSPHERE;
        public static Object GLOW;

        public static void init() {}
    }

    static {
        try {
            for (Object field : CampaignPlanet.class.getDeclaredFields()) {
                if (getFieldName(field).equals("graphics")) {
                    campaignPlanetGraphicsField = field;
                    setFieldAccessibleHandle.invoke(field, true);
                    break;
                }
            }

            for (Object field : Planet.class.getDeclaredFields()) {
                switch (getFieldName(field)) {
                    case "planetTex":
                        texClassCtor = getFieldType(field).getDeclaredConstructors()[0];
                        PlanetTexType.PLANET = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "cloudTex":
                        PlanetTexType.CLOUD = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "shieldTex":
                        PlanetTexType.SHIELD = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;

                    case "shieldTex2":
                        PlanetTexType.SHIELD2 = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "atmosphereTex":
                        PlanetTexType.ATMOSPHERE = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    case "glowTex":
                        PlanetTexType.GLOW = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;

                    case "listToken1":
                        planetListToken1Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                        
                    case "listToken2":
                        planetListToken2Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                        
                    case "listToken3":
                        planetListToken3Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                        
                    case "listToken4":
                        planetListToken4Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                        
                    case "listToken5":
                        planetListToken5Field = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    default:
                        continue;
                }
            }

            Object textObj = instantiateTexObj(69, 420);
            for (Object field : textObj.getClass().getDeclaredFields()) {
                if (getFieldTypeHandle.invoke(field).equals(int.class)) {
                    setFieldAccessibleHandle.invoke(field, true);

                    if (((int)getFieldHandle.invoke(field, textObj)) == 420) {
                        texObjectIdField = field;
                        break;
                    } 
                }
            }

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

    public static Object getPlanetTex(Planet planet, Object texField) {
        try {
            return getFieldHandle.invoke(texField, planet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // public static void setPlanetTex(Planet planet, Object texObj) {
    //     try {
    //         setFieldHandle.invoke(planetTexField, planet, texObj);
    //     } catch (Throwable e) {
    //         throw new RuntimeException(e);
    //     }
    // }

    public static void setTexObjId(Object texObj, int id) {
        try {
            setFieldHandle.invoke(texObjectIdField, texObj, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void invalidateTokens(Planet planet) {
        try {
            GLListManager.invalidateList((GLListToken)getFieldHandle.invoke(planetListToken1Field, planet));
            GLListManager.invalidateList((GLListToken)getFieldHandle.invoke(planetListToken2Field, planet));
            GLListManager.invalidateList((GLListToken)getFieldHandle.invoke(planetListToken3Field, planet));
            GLListManager.invalidateList((GLListToken)getFieldHandle.invoke(planetListToken4Field, planet));
            GLListManager.invalidateList((GLListToken)getFieldHandle.invoke(planetListToken5Field, planet));
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
