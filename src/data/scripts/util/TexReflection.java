package data.scripts.util;

import data.scripts.projector.PlanetProjector.PlanetTexType;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.campaign.CampaignPlanet;
import com.fs.starfarer.campaign.RingBand;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.starfarer.loading.specs.PlanetSpec;
import com.fs.graphics.Sprite;
import com.fs.graphics.TextureLoader;
import com.fs.graphics.util.GLListManager;
import com.fs.graphics.util.GLListManager.GLListToken;

import org.apache.log4j.Logger;
import org.lazywizard.console.Console;
import org.lwjgl.opengl.GL11;

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
    private static final Class<?> methodClass;

    private static final MethodHandle getFieldTypeHandle;
    private static final MethodHandle getFieldModifiersHandle;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;

    public static final MethodHandle getMethodNameHandle;
    public static final MethodHandle invokeMethodHandle;
    public static final MethodHandle getModifiersHandle;
    public static final MethodHandle getParameterTypesHandle;
    public static final MethodHandle getReturnTypeHandle;
    
    // private static final MethodHandle constructorNewInstanceHandle;
    private static final MethodHandle getConstructorParameterTypesHandle;

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class.class.getClassLoader());
            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());

            setFieldHandle = lookup.findVirtual(fieldClass, "set", MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            getFieldTypeHandle = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            getFieldModifiersHandle = lookup.findVirtual(fieldClass, "getModifiers", MethodType.methodType(int.class));

            getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            getModifiersHandle = lookup.findVirtual(methodClass, "getModifiers", MethodType.methodType(int.class));
            getParameterTypesHandle = lookup.findVirtual(methodClass, "getParameterTypes", MethodType.methodType(Class[].class));
            getReturnTypeHandle = lookup.findVirtual(methodClass, "getReturnType", MethodType.methodType(Class.class));
            invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));

            // constructorNewInstanceHandle = lookup.findVirtual(constructorClass, "newInstance", MethodType.methodType(Object.class, Object[].class));
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

    public static int getFieldModifiers(Object field) throws Throwable {
        return (int)getFieldModifiersHandle.invoke(field);
    }

    public static boolean isStatic(int modifiers) {
        return (modifiers & 8) != 0;
    }

    public static boolean isPublic(int modifiers) {
        return (modifiers & 1) != 0;
    }

    public static String getTexBindMethodName() {
        if (texObjectBindMethodName != null) return texObjectBindMethodName;
        Object texWrapper = instantiateTexObj(GL11.GL_TEXTURE_2D, 42069);

        try {

            for (Object method : texWrapper.getClass().getDeclaredMethods()) {
                Class<?> returnType = (Class<?>) getReturnTypeHandle.invoke(method);
                Class<?>[] paramTypes = (Class<?>[]) getParameterTypesHandle.invoke(method);

                if ((returnType.equals(void.class)) && paramTypes.length == 0 && isPublic((int)getModifiersHandle.invoke(method))) {
                    invokeMethodHandle.invoke(method, texWrapper);

                    if (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == 42069) {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                        return (String) getMethodNameHandle.invoke(method);
                    }
                }
            }
            return null;

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static VarHandle spriteTextureVarHandle;
    public static VarHandle spriteTextureIdVarHandle;

    public static VarHandle ringBandTextureVarHandle;

    public static VarHandle planetListToken1VarHandle;
    public static VarHandle planetListToken2VarHandle;
    public static VarHandle planetListToken3VarHandle;
    public static VarHandle planetListToken4VarHandle;
    public static VarHandle planetListToken5VarHandle;

    public static VarHandle campaignPlanetGraphicsVarHandle;
    public static VarHandle campaignPlanetSpecVarHandle;

    public static final Class<?> texClass;
    public static final String texObjectBindMethodName;
    public static MethodHandle texClassCtorHandle;
    public static VarHandle texObjectIdVarHandle;
    public static VarHandle texObjectGLBindVarHandle;

    /** This is the repository map for the gl texture id wrapper objects that the PlanetSpec class (and also pretty much everything else it appears) pulls from. Each planet projector will add its own to this temporarily while it is active. */
    public static Map<String, Object> texObjectMap;

    private static final VarHandle[] texClassVarHandles;

    static {
        try {
            Lookup privateLookup = MethodHandles.privateLookupIn(Sprite.class, lookup);
            Class<?> textureClass = null;

            outer:
            for (Object field : Sprite.class.getDeclaredFields()) {
                if (getFieldName(field).equals("texture")) {
                    textureClass = getFieldType(field);

                    for (Object ctor : textureClass.getConstructors()) {
                        Class<?>[] paramTypes = (Class[]) getConstructorParameterTypesHandle.invoke(ctor);
                        if (paramTypes.length == 2) {
                            texClassCtorHandle = lookup.findConstructor(textureClass, MethodType.methodType(void.class, paramTypes[0], paramTypes[1]));
                            break outer;
                        }
                    }
                }
            }
            texClass = textureClass;
            texObjectBindMethodName = getTexBindMethodName();

            spriteTextureVarHandle = privateLookup.findVarHandle(
                Sprite.class,
                "texture",
                textureClass
            );
            spriteTextureIdVarHandle = privateLookup.findVarHandle(
                Sprite.class,
                "textureId",
                String.class
            );

            privateLookup = MethodHandles.privateLookupIn(RingBand.class, lookup);
            ringBandTextureVarHandle = privateLookup.findVarHandle(
                RingBand.class,
                "texture",
                textureClass
            );

            privateLookup = MethodHandles.privateLookupIn(CampaignPlanet.class, lookup);
            campaignPlanetGraphicsVarHandle = privateLookup.findVarHandle(
                CampaignPlanet.class,
                "graphics",
                Planet.class
            );
            campaignPlanetSpecVarHandle = privateLookup.findVarHandle(
                CampaignPlanet.class,
                "spec",
                PlanetSpec.class
            );

            privateLookup = MethodHandles.privateLookupIn(Planet.class, lookup);
            planetListToken1VarHandle = privateLookup.findVarHandle(
                Planet.class,
                "listToken1",
                GLListToken.class
            );
            planetListToken2VarHandle = privateLookup.findVarHandle(
                Planet.class,
                "listToken2",
                GLListToken.class
            );
            planetListToken3VarHandle = privateLookup.findVarHandle(
                Planet.class,
                "listToken3",
                GLListToken.class
            );
            planetListToken4VarHandle = privateLookup.findVarHandle(
                Planet.class,
                "listToken4",
                GLListToken.class
            );
            planetListToken5VarHandle = privateLookup.findVarHandle(
                Planet.class,
                "listToken5",
                GLListToken.class
            );

            PlanetTexType.PLANET = privateLookup.findVarHandle(
                Planet.class,
                "planetTex",
                textureClass
            );
            PlanetTexType.CLOUD = privateLookup.findVarHandle(
                Planet.class,
                "cloudTex",
                textureClass
            );
            PlanetTexType.SHIELD = privateLookup.findVarHandle(
                Planet.class,
                "shieldTex",
                textureClass
            );
            PlanetTexType.SHIELD2 = privateLookup.findVarHandle(
                Planet.class,
                "shieldTex2",
                textureClass
            );
            PlanetTexType.ATMOSPHERE = privateLookup.findVarHandle(
                Planet.class,
                "atmosphereTex",
                textureClass
            );
            PlanetTexType.GLOW = privateLookup.findVarHandle(
                Planet.class,
                "glowTex",
                textureClass
            );

            privateLookup = MethodHandles.privateLookupIn(PlanetSpec.class, lookup);

            PlanetTexType.VARHANDLE_MAP.put(PlanetTexType.PLANET, privateLookup.findVarHandle(
                PlanetSpec.class,
                "texture",
                String.class
            ));
            PlanetTexType.VARHANDLE_MAP.put(PlanetTexType.CLOUD, privateLookup.findVarHandle(
                PlanetSpec.class,
                "cloudTexture",
                String.class
            ));
            PlanetTexType.VARHANDLE_MAP.put(PlanetTexType.SHIELD, privateLookup.findVarHandle(
                PlanetSpec.class,
                "shieldTexture",
                String.class
            ));
            PlanetTexType.VARHANDLE_MAP.put(PlanetTexType.SHIELD2, privateLookup.findVarHandle(
                PlanetSpec.class,
                "shieldTexture2",
                String.class
            ));
            PlanetTexType.VARHANDLE_MAP.put(PlanetTexType.GLOW, privateLookup.findVarHandle(
                PlanetSpec.class,
                "glowTexture",
                String.class
            ));

            privateLookup = MethodHandles.privateLookupIn(textureClass, lookup);
            Object textObj = instantiateTexObj(69, 420);
            List<VarHandle> handles = new ArrayList<>();

            for (Object field : textureClass.getDeclaredFields()) {
                Class<?> type = getFieldType(field);
                String name = getFieldName(field);

                if (type.equals(int.class)) {
                    setFieldAccessibleHandle.invoke(field, true);
                    int value = (int) getFieldHandle.invoke(field, textObj);

                    if (value == 420) {
                        texObjectIdVarHandle = privateLookup.findVarHandle(
                            textureClass,
                            name,
                            int.class
                        );

                    } else if (value == 69) {
                        texObjectGLBindVarHandle = privateLookup.findVarHandle(
                            textureClass,
                            name,
                            int.class
                        );
                    }
                }
                
                if (isStatic(getFieldModifiers(field))) continue;
                handles.add(privateLookup.findVarHandle(textureClass, name, type));
            }
            texClassVarHandles = handles.toArray(new VarHandle[0]);

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
            return (Planet) campaignPlanetGraphicsVarHandle.get(campaignPlanet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPlanetSpecTextureId(VarHandle handle, String id, PlanetSpec spec) {
        try {
            if (handle != null) handle.set(spec, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPlanetSpec(PlanetAPI campaignPlanet, PlanetSpec spec) {
        try {
            campaignPlanetSpecVarHandle.set(campaignPlanet, spec);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getPlanetTex(Planet planet, VarHandle texVarHandle) {
        try {
            return texVarHandle.get(planet);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPlanetTex(Planet planet, VarHandle texVarHandle, Object texObj) {
        try {
            texVarHandle.set(planet, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getTexObjId(Object texObj) {
        try {
            return (int) texObjectIdVarHandle.get(texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setTexObjId(Object texObj, int id) {
        try {
            texObjectIdVarHandle.set(texObj, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void invalidateTokens(Planet planet) {
        try {
            GLListManager.invalidateList((GLListToken)planetListToken1VarHandle.get(planet));
            GLListManager.invalidateList((GLListToken)planetListToken2VarHandle.get(planet));
            GLListManager.invalidateList((GLListToken)planetListToken3VarHandle.get(planet));
            GLListManager.invalidateList((GLListToken)planetListToken4VarHandle.get(planet));
            GLListManager.invalidateList((GLListToken)planetListToken5VarHandle.get(planet));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object instantiateTexObj(int glBindType, int texId) {
        try {
            return texClassCtorHandle.invoke(glBindType, texId);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void logPlanetTexObjBindType(String typeName, Object field, PlanetAPI campaignPlanet) {
        Planet planet = getPlanetFromCampaignPlanet(campaignPlanet);

        try {
            Object texObj = getFieldHandle.invoke(field, planet);
            if (texObj == null) return;

            int bindType = (int) texObjectGLBindVarHandle.get(texObj);
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

    public static void setSpriteTexId(Sprite sprite, String id) {
        try {
            spriteTextureIdVarHandle.set(sprite, id);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSpriteTexId(Sprite sprite) {
        try {
            return (String) spriteTextureIdVarHandle.get(sprite);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setSpriteTexObj(Sprite sprite, Object texObj) {
        try {
            spriteTextureIdVarHandle.set(sprite, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getSpriteTexObj(Sprite sprite) {
        try {
            return spriteTextureIdVarHandle.get(sprite);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setRingBandTexObj(RingBand ringBand, Object texObj) {
        try {
            ringBandTextureVarHandle.set(ringBand, texObj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getRingBandTexObj(RingBand ringBand) {
        try {
            return ringBandTextureVarHandle.get(ringBand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void logTexWrapperIds() {
        print("        LOADED TEXTURE IDs              ");
        print("--------------------------------");
        for (String textureId : texObjectMap.keySet()) {
            print(textureId);
        }
    }

    public static void transplantTexFields(Object original, Object destination) {
        for (VarHandle handle : texClassVarHandles) handle.set(destination, handle.get(original));
    }

    public static void init() {}
}
