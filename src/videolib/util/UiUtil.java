package videolib.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import org.objectweb.asm.*;

import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignState;

public class UiUtil implements Opcodes {
    public static interface UtilInterface {
        public List<UIComponentAPI> getChildrenNonCopy(Object uiPanel);
        public List<UIComponentAPI> getChildrenCopy(Object uiPanel);
        public UIPanelAPI getParent(Object uiComponent);
        public UIPanelAPI findTopAncestor(Object uiComponent);
    }

    public static final UtilInterface utils;

    static {
        Class<?> uiPanelClass = null;
        for (Object field : CampaignState.class.getDeclaredFields()) {
            if (TexReflection.getFieldName(field).equals("screenPanel")) {
                uiPanelClass = TexReflection.getFieldType(field);
                break;
            }
        }

        String uiPanelInternalName = Type.getInternalName(uiPanelClass);
        Class<?> uiComponentClass = uiPanelClass.getSuperclass();
        String uiComponentInternalName = Type.getInternalName(uiComponentClass);

        String uiPanelAPIDesc = Type.getDescriptor(UIPanelAPI.class);
        String uiPanelClassDesc = Type.getDescriptor(uiPanelClass);

        String superName = Type.getInternalName(Object.class);
        String interfaceName = Type.getInternalName(UtilInterface.class);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // public class UtilInterface extends Object implements this crap
        cw.visit(
            V17,
            ACC_PUBLIC,
            "videolib/util/UtilInterface",
            null,
            superName,
            new String[] {interfaceName}
        );

        MethodVisitor ctor = cw.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // public List<UIComponentAPI> getChildrenNonCopy(Object uiPanel) {
        //     return ((uiPanelClass)uiPanel).getChildrenNonCopy();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getChildrenNonCopy",
                "(Ljava/lang/Object;)Ljava/util/List;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiPanelInternalName,
                "getChildrenNonCopy",
                "()Ljava/util/List;",
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public List<UIComponentAPI> getChildrenCopy(Object uiPanel) {
        //     return ((uiPanelClass)uiPanel).getChildrenCopy();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getChildrenCopy",
                "(Ljava/lang/Object;)Ljava/util/List;",
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiPanelInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiPanelInternalName,
                "getChildrenCopy",
                "()Ljava/util/List;",
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI getParent(Object uiComponent) {
        //     return ((uiComponentClass)uiComponent).getParent();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "getParent",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiComponentInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiComponentInternalName,
                "getParent",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // public UIPanelAPI findTopAncestor(Object uiComponent) {
        //     return ((uiComponentClass)uiComponent).findTopAncestor();
        // }
        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "findTopAncestor",
                "(Ljava/lang/Object;)" + uiPanelAPIDesc,
                null,
                null
            );
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, uiComponentInternalName);

            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                uiComponentInternalName,
                "findTopAncestor",
                "()" + uiPanelClassDesc,
                false
            );

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        try {
            utils = (UtilInterface) MethodHandles.lookup().findConstructor(new ClassLoader(UiUtil.class.getClassLoader()) {
                public Class<?> define(byte[] classBytes, String name) {
                    return defineClass(name, classBytes, 0, classBytes.length);
                }
            }.define(cw.toByteArray(), "videolib.util.UtilInterface"), MethodType.methodType(void.class)).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
