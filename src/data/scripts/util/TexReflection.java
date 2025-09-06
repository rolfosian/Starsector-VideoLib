package data.scripts.util;

import data.scripts.projector.PlanetProjector.PlanetTexType;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.campaign.CampaignPlanet;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.starfarer.loading.specs.PlanetSpec;
import com.fs.graphics.Sprite;
import com.fs.graphics.TextureLoader;
import com.fs.graphics.util.GLListManager;
import com.fs.graphics.util.GLListManager.GLListToken;

import org.apache.log4j.Logger;
import org.lazywizard.console.Console;

@SuppressWarnings("unchecked")
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

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;
    
    private static final MethodHandle constructorNewInstanceHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));

            constructorNewInstanceHandle = lookup.findVirtual(constructorClass, "newInstance", MethodType.methodType(Object.class, Object[].class));
            getConstructorParameterTypesHandle = lookup.findVirtual(constructorClass, "getParameterTypes", MethodType.methodType(Class[].class));

        } catch (Exception e) {
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

    public static void setPrivateVariable(Object field, Object instanceToModify, Object newValue) {
        try {
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

    public static Object spriteTextureField;
    public static Object spriteTextureIdField;

    public static Object planetListToken1Field;
    public static Object planetListToken2Field;
    public static Object planetListToken3Field;
    public static Object planetListToken4Field;
    public static Object planetListToken5Field;

    public static Object campaignPlanetGraphicsField;
    public static Object campaignPlanetSpecField;

    public static Object texClassCtor;
    public static Object texObjectIdField;
    public static Object texObjectGLBindField;

    /** This is the repository map for the gl texture id wrapper objects that the PlanetSpec class pulls from. Each planet projector will add its own to this temporarily while it is active. */
    public static Map<String, Object> texObjectMap;
    
    static {
        try {

            for (Object field : Sprite.class.getDeclaredFields()) {
                switch(getFieldName(field)) {
                    case "texture":
                        spriteTextureField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        break;
                    
                    case "textureId":
                        spriteTextureIdField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        break;
                    
                    default:
                        break;
                }
            }

            for (Object field : CampaignPlanet.class.getDeclaredFields()) {
                switch(getFieldName(field)) {
                    case "graphics":
                        campaignPlanetGraphicsField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;

                    case "spec":
                        campaignPlanetSpecField = field;
                        setFieldAccessibleHandle.invoke(field, true);
                        continue;
                    
                    default:
                        continue;
                }
            }

            for (Object field : Planet.class.getDeclaredFields()) {
                switch (getFieldName(field)) {
                    case "planetTex":
                        for (Object ctor : getFieldType(field).getConstructors()) {
                            if (((Class[])getConstructorParameterTypesHandle.invoke(ctor)).length == 2) {
                                texClassCtor = ctor;
                                break;
                            }
                        }

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

            for (Object field : PlanetSpec.class.getDeclaredFields()) {
                switch(getFieldName(field)) {
                    case "texture":
                        PlanetTexType.FIELD_MAP.put(PlanetTexType.PLANET, field);
                        setFieldAccessibleHandle.invoke(field, true);
                        break;

                    case "cloudTexture":
                        PlanetTexType.FIELD_MAP.put(PlanetTexType.CLOUD, field);
                        setFieldAccessibleHandle.invoke(field, true);
                        break;

                    case "glowTexture":
                        PlanetTexType.FIELD_MAP.put(PlanetTexType.GLOW, field);
                        setFieldAccessibleHandle.invoke(field, true);
                        break;

                    case "shieldTexture":
                        PlanetTexType.FIELD_MAP.put(PlanetTexType.SHIELD, field);
                        setFieldAccessibleHandle.invoke(field, true);
                        break;
                        
                    case "shieldTexture2":
                        PlanetTexType.FIELD_MAP.put(PlanetTexType.SHIELD2, field);
                        setFieldAccessibleHandle.invoke(field, true);
                        break;
                }
            }

            Object textObj = instantiateTexObj(69, 420);
            for (Object field : textObj.getClass().getDeclaredFields()) {
                if (getFieldTypeHandle.invoke(field).equals(int.class)) {
                    setFieldAccessibleHandle.invoke(field, true);
                    int value = (int) getFieldHandle.invoke(field, textObj);

                    if (value == 420) {
                        texObjectIdField = field;
                    } else if (value == 69) {
                        texObjectGLBindField = field;
                    }
                }
            }

            for (Class<?> cls : getAllObfClasses("fs.common_obf.jar")) {
                Object[] fields = cls.getDeclaredFields();
                if (!(fields.length == 4)) continue;
    
                boolean booleanMatch = false;
                boolean mapMatch = false;
                boolean loggerMatch = false;
                boolean textureLoaderMatch = false;
    
                Object mapField = null;
                for (Object field : fields) {

                    Class<?> fieldType = getFieldType(field);
                    if (fieldType.equals(boolean.class)) {
                        booleanMatch = true;

                    } else if (fieldType.equals(Map.class)) {
                        mapMatch = true;
                        mapField = field;

                    } else if (fieldType.equals(Logger.class)) {
                        loggerMatch = true;

                    } else if (fieldType.equals(TextureLoader.class)) {
                        textureLoaderMatch = true;
                    } 
                }
    
                if (booleanMatch && mapMatch && loggerMatch && textureLoaderMatch) {
                    setFieldAccessibleHandle.invoke(mapField, true);
                    texObjectMap = (Map<String, Object>) getFieldHandle.invoke(mapField, null);
                    break;
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

    public static void setPlanetSpecTextureId(Object field, String id, PlanetSpec spec) {
        try {
            setFieldHandle.invoke(field, spec, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPlanetSpec(PlanetAPI campaignPlanet, PlanetSpec spec) {
        try {
            setFieldHandle.invoke(campaignPlanetSpecField, campaignPlanet, spec);
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

    public static Object setPlanetTex(Planet planet, Object texField, Object texObj) {
        try {
            return setFieldHandle.invoke(texField, planet, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getTexObjId(Object texObj) {
        try {
            return (int) getFieldHandle.invoke(texObjectIdField, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

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

    public static void logPlanetTexObjBindType(String typeName, Object field, PlanetAPI campaignPlanet) {
        Planet planet = getPlanetFromCampaignPlanet(campaignPlanet);

        try {
            Object texObj = getFieldHandle.invoke(field, planet);
            if (texObj == null) return;

            int bindType = (int) getFieldHandle.invoke(texObjectGLBindField, texObj);
            Console.showMessage("BindType for " + campaignPlanet.getName() + " " + typeName+":" + " " + String.valueOf(bindType));
            print("BindType for", campaignPlanet.getName(), typeName+":", bindType);

        } catch (Throwable e) {
            print(e);
        }
    }
    
    // runcode import data.scripts.util.TexReflection; TexReflection.logTexObjBindTypes(); 
    // All return GL11.GL_TEXTURE_2D afaik
    public static void logPlanetTexObjBindTypes() {
        for (PlanetAPI campaignPlanet : Global.getSector().getPlayerFleet().getContainingLocation().getPlanets()) {
            TexReflection.logPlanetTexObjBindType("planet tex", PlanetTexType.PLANET, campaignPlanet);
            TexReflection.logPlanetTexObjBindType("cloud tex", PlanetTexType.CLOUD, campaignPlanet);
            TexReflection.logPlanetTexObjBindType("atmosphere tex", PlanetTexType.ATMOSPHERE, campaignPlanet);
            TexReflection.logPlanetTexObjBindType("glow tex", PlanetTexType.GLOW, campaignPlanet);
            TexReflection.logPlanetTexObjBindType("shield tex", PlanetTexType.SHIELD, campaignPlanet);
            TexReflection.logPlanetTexObjBindType("shield2 tex", PlanetTexType.SHIELD2, campaignPlanet);
        }
    }

    public static void setSpriteTexId(Sprite sprite, Object id) {
        try {
            setFieldHandle.invoke(spriteTextureIdField, sprite, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSpriteTexId(Sprite sprite) {
        try {
            return (String) getFieldHandle.invoke(spriteTextureIdField, sprite);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setSpriteTexObj(Sprite sprite, Object texObj) {
        try {
            setFieldHandle.invoke(spriteTextureField, sprite, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getSpriteTexObj(Sprite sprite) {
        try {
            return getFieldHandle.invoke(spriteTextureField, sprite);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {}
}
