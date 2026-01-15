package data.scripts.projector;

import java.util.List;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.*;

import rolflectionlib.inheritor.Inherit;
import rolflectionlib.util.RolfLectionUtil;
import rolflectionlib.util.TexReflection;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
// import com.fs.starfarer.api.input.InputEventAPI;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.VideoPaths;
import data.scripts.buffers.TexBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;
// import data.scripts.util.TexReflection;
// import data.scripts.util.VideoUtils;

/**
 * Direct subclass of obfuscated Texture class implementing EveryFrameScript, EveryFrameCombatPlugin, and Projector interfaces. Automatically stops and starts itself if it is being rendered.
 */
public class TransientTexProjector implements Opcodes {
    public static interface TransientTexInstantiator {
        public Projector instantiate(String videoId, int width, int height);
    }
    public static final TransientTexInstantiator instantiator;

    public static void print(String msg) {
        Global.getLogger(TransientTexProjector.class).info(msg);
    }

    private static final Logger logger = Logger.getLogger(VideoProjector.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    public static final Class<?> transientTexClass;

    static {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        String className = "data/scripts/projector/TransientTex";
        String superName = Type.getInternalName(TexReflection.texClass);
    
        String decoderName = Type.getInternalName(Decoder.class);
        String decoderDesc = Type.getDescriptor(Decoder.class);

        String muteDecoderName = Type.getInternalName(MuteDecoder.class);
        String texReflectionName = Type.getInternalName(data.scripts.util.TexReflection.class);
        String globalName = Type.getInternalName(Global.class);

        String texBufferInternalName = Type.getInternalName(TexBuffer.class);
        String texBufferDesc = Type.getDescriptor(TexBuffer.class);
        String playModeDesc = Type.getDescriptor(PlayMode.class);
        String eofModeDesc = Type.getDescriptor(EOFMode.class);

        String viewPortDesc = Type.getDescriptor(ViewportAPI.class);
        String combatEngineDesc = Type.getDescriptor(CombatEngineAPI.class);
        String listDesc = Type.getDescriptor(List.class);
        
        String texReflectionInternal = Type.getInternalName(data.scripts.util.TexReflection.class);
        String bindMethodName = RolfLectionUtil.getMethodName(TexReflection.getTexBindMethod());
    
        cw.visit(
            V17,
            ACC_PUBLIC,
            className,
            null,
            superName,
            new String[] {
                Type.getInternalName(EveryFrameCombatPlugin.class),
                Type.getInternalName(EveryFrameScript.class),
                Type.getInternalName(Projector.class)
            }
        );

        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "TIMEOUT_FRAMES", "I", null, 5);

        cw.visitField(ACC_PRIVATE, "timeoutFrames", "I", null, 0).visitEnd();
        cw.visitField(ACC_PRIVATE, "isDone", "Z", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "runWhilePaused", "Z", null, null).visitEnd();

        cw.visitField(ACC_PRIVATE, "videoFilePath", "Ljava/lang/String;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "width", "I", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "height", "I", null, null).visitEnd();

        cw.visitField(ACC_PRIVATE, "paused", "Z", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "isRendering", "Z", null, null).visitEnd();

        cw.visitField(ACC_PRIVATE, "MODE", playModeDesc, null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "EOF_MODE", eofModeDesc, null, null).visitEnd();

        cw.visitField(ACC_PRIVATE, "decoder", decoderDesc, null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "textureBuffer", texBufferDesc, null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "currentTextureId", "I", null, 0).visitEnd();
    
        /* constructor */
        MethodVisitor ctor = cw.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "(Ljava/lang/String;II)V",
            null,
            null
        );
        ctor.visitCode();
    
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitLdcInsn(GL11.GL_TEXTURE_2D);
        ctor.visitInsn(ICONST_0);
        ctor.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "(II)V", false);

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitMethodInsn(
            INVOKESTATIC,
            Type.getInternalName(VideoPaths.class),
            "getVideoPath",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
        );
        ctor.visitFieldInsn(PUTFIELD, className, "videoFilePath", "Ljava/lang/String;");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ILOAD, 2);
        ctor.visitFieldInsn(PUTFIELD, className, "width", "I");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ILOAD, 3);
        ctor.visitFieldInsn(PUTFIELD, className, "height", "I");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_1);
        ctor.visitFieldInsn(PUTFIELD, className, "runWhilePaused", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(
            GETSTATIC,
            Type.getInternalName(PlayMode.class),
            "PLAYING",
            playModeDesc
        );
        ctor.visitFieldInsn(
            PUTFIELD,
            className,
            "MODE",
            playModeDesc
        );

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(
            GETSTATIC,
            Type.getInternalName(EOFMode.class),
            "LOOP",
            eofModeDesc
        );
        ctor.visitFieldInsn(
            PUTFIELD,
            className,
            "EOF_MODE",
            eofModeDesc
        );
    
        ctor.visitInsn(RETURN);
        ctor.visitEnd();
        
        {
            /* bind method () */
            MethodVisitor bind = cw.visitMethod(
                ACC_PUBLIC,
                bindMethodName,
                "()V",
                null,
                null
            );
            bind.visitCode();
        
            /* timeoutFrames= 0 */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitInsn(ICONST_0);
            bind.visitFieldInsn(
                PUTFIELD,
                className,
                "timeoutFrames",
                "I"
            );
        
            Label decoderExists = new Label();
        
            /* if (decoder != null) goto decoderExists */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(
                GETFIELD,
                className,
                "decoder",
                decoderDesc
            );
            bind.visitJumpInsn(IFNONNULL, decoderExists);
            

            /* decoder = new MuteDecoder(...) */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitTypeInsn(NEW, muteDecoderName);
            bind.visitInsn(DUP);
        
            bind.visitVarInsn(ALOAD, 0);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "videoFilePath", "Ljava/lang/String;");
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "width", "I");
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "height", "I");
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "MODE", playModeDesc);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "EOF_MODE", eofModeDesc);
        
            bind.visitMethodInsn(
                INVOKESPECIAL,
                muteDecoderName,
                "<init>",
                "(" +
                    "Ldata/scripts/projector/Projector;" +
                    "Ljava/lang/String;" +
                    "II" +
                    playModeDesc +
                    eofModeDesc +
                ")V",
                false
            );
        
            bind.visitFieldInsn(
                PUTFIELD,
                className,
                "decoder",
                decoderDesc
            );
        
            /* decoder.start(0) */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            bind.visitInsn(LCONST_0);
            bind.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "start",
                "(J)V",
                true
            );

            bind.visitVarInsn(ALOAD, 0);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            bind.visitMethodInsn(INVOKEINTERFACE, decoderName, "getTextureBuffer", "()" + texBufferDesc, true);
            bind.visitFieldInsn(PUTFIELD, className, "textureBuffer", texBufferDesc);
        
            /* currentTextureId = decoder.getCurrentVideoTextureId() */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            bind.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "getCurrentVideoTextureId",
                "()I",
                true
            );
            bind.visitFieldInsn(
                PUTFIELD,
                className,
                "currentTextureId",
                "I"
            );

            // isDone = false;
            bind.visitVarInsn(ALOAD, 0);
            bind.visitInsn(ICONST_0);
            bind.visitFieldInsn(PUTFIELD, className, "isDone", "Z");

            // paused = false;
            bind.visitVarInsn(ALOAD, 0);
            bind.visitInsn(ICONST_0);
            bind.visitFieldInsn(PUTFIELD, className, "paused", "Z");
        
            /* TexReflection.setTexObjId(this, currentTextureId) */
            bind.visitVarInsn(ALOAD, 0);
            bind.visitTypeInsn(CHECKCAST, superName);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            bind.visitMethodInsn(
                INVOKESTATIC,
                texReflectionName,
                "setTexObjId",
                "(Ljava/lang/Object;I)V",
                false
            );
        
            /* Global.getSector().addTransientScript(this) */
            bind.visitMethodInsn(INVOKESTATIC, globalName, "getSector", "()Lcom/fs/starfarer/api/campaign/SectorAPI;", false);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitMethodInsn(
                INVOKEINTERFACE,
                "com/fs/starfarer/api/campaign/SectorAPI",
                "addTransientScript",
                "(Lcom/fs/starfarer/api/EveryFrameScript;)V",
                true
            );
        
            /* Global.getCombatEngine().addPlugin(this) */
            bind.visitMethodInsn(INVOKESTATIC, globalName, "getCombatEngine", "()Lcom/fs/starfarer/api/combat/CombatEngineAPI;", false);
            bind.visitVarInsn(ALOAD, 0);
            bind.visitMethodInsn(
                INVOKEINTERFACE,
                "com/fs/starfarer/api/combat/CombatEngineAPI",
                "addPlugin",
                "(Lcom/fs/starfarer/api/combat/EveryFrameCombatPlugin;)V",
                true
            );
        
            bind.visitLabel(decoderExists);
        
            /* super.bind() */
            // bind.visitFieldInsn(GETSTATIC, "org/lwjgl/opengl/GL11", "GL_TEXTURE_2D", "I");
            // bind.visitVarInsn(ALOAD, 0);
            // bind.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            // bind.visitMethodInsn(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glBindTexture", "(II)V", false);

            bind.visitVarInsn(ALOAD, 0);
            bind.visitMethodInsn(
                INVOKESPECIAL,
                superName,
                bindMethodName,
                "()V",
                false
            );

            // bind.visitLdcInsn("test");
            // bind.visitMethodInsn(
            //     INVOKESTATIC,
            //     Type.getInternalName(TransientTexProjector.class),
            //     "print",
            //     "(Ljava/lang/String;)V",
            //     false
            // );
        
            bind.visitInsn(RETURN);
            bind.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "advance", "(F)V", null, null);
            mv.visitCode();

            Label lCheckNullOrPaused = new Label();
            Label lReturn = new Label();
            Label lSkipDelete = new Label();

            // if (decoder != null && timeoutFrames++ > TIMEOUT_FRAMES)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitJumpInsn(IFNULL, lCheckNullOrPaused);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, className, "timeoutFrames", "I");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");

            mv.visitFieldInsn(GETSTATIC, className, "TIMEOUT_FRAMES", "I");
            mv.visitJumpInsn(IF_ICMPLE, lCheckNullOrPaused);

            // finish();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "finish", "()V", false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");

            mv.visitInsn(RETURN);

            mv.visitLabel(lCheckNullOrPaused);

            // if (decoder == null || paused) return;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitJumpInsn(IFNULL, lReturn);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "paused", "Z");
            mv.visitJumpInsn(IFNE, lReturn);

            // int newId = decoder.getCurrentVideoTextureId(dt);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitVarInsn(FLOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, decoderName, "getCurrentVideoTextureId", "(F)I", true);
            mv.visitVarInsn(ISTORE, 2);

            // if (newId != currentTextureId)
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitJumpInsn(IF_ICMPEQ, lReturn);

            // data.scripts.util.TexReflection.setTexObjId(this, newId);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, superName);
            mv.visitVarInsn(ILOAD, 2);
            // mv.visitInsn(DUP);
            // mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
            // mv.visitMethodInsn(
            //     INVOKESTATIC,
            //     Type.getInternalName(TransientTexProjector.class),
            //     "print",
            //     "(Ljava/lang/String;)V",
            //     false
            // );
            mv.visitMethodInsn(INVOKESTATIC, texReflectionInternal, "setTexObjId", "(Ljava/lang/Object;I)V", false);

            // if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitJumpInsn(IFEQ, lSkipDelete);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "textureBuffer", texBufferDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitMethodInsn(INVOKEINTERFACE, texBufferInternalName, "deleteTexture", "(I)V", true);

            // currentTextureId = newId;
            mv.visitLabel(lSkipDelete);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "currentTextureId", "I");

            mv.visitLabel(lReturn);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "advance", "(FLjava/util/List;)V", null, null);
            mv.visitCode();

            Label lCheckNullOrPaused = new Label();
            Label lReturn = new Label();
            Label lSkipDelete = new Label();

            // if (decoder != null && timeoutFrames++ > TIMEOUT_FRAMES)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitJumpInsn(IFNULL, lCheckNullOrPaused); // If null, skip timeout check

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, className, "timeoutFrames", "I");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");

            mv.visitFieldInsn(GETSTATIC, className, "TIMEOUT_FRAMES", "I");
            mv.visitJumpInsn(IF_ICMPLE, lCheckNullOrPaused);

            // finish();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "finish", "()V", false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");

            mv.visitInsn(RETURN);

            mv.visitLabel(lCheckNullOrPaused);

            // if (decoder == null || paused) return;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitJumpInsn(IFNULL, lReturn);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "paused", "Z");
            mv.visitJumpInsn(IFNE, lReturn);

            // int newId = decoder.getCurrentVideoTextureId(dt);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitVarInsn(FLOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, decoderName, "getCurrentVideoTextureId", "(F)I", true);
            mv.visitVarInsn(ISTORE, 2);

            // if (newId != currentTextureId)
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitJumpInsn(IF_ICMPEQ, lReturn);

            // data.scripts.util.TexReflection.setTexObjId(this, newId);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, superName);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, texReflectionInternal, "setTexObjId", "(Ljava/lang/Object;I)V", false);

            // if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitJumpInsn(IFEQ, lSkipDelete);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "textureBuffer", texBufferDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitMethodInsn(INVOKEINTERFACE, texBufferInternalName, "deleteTexture", "(I)V", true);

            // currentTextureId = newId;
            mv.visitLabel(lSkipDelete);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "currentTextureId", "I");

            mv.visitLabel(lReturn);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "finish", "()V", null, null);
            mv.visitCode();

            // if (currentTextureId != 0)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            Label lSkipDelete = new Label();
            mv.visitJumpInsn(IFEQ, lSkipDelete);

            // textureBuffer.deleteTexture(currentTextureId);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "textureBuffer", texBufferDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitMethodInsn(INVOKEINTERFACE, texBufferInternalName, "deleteTexture", "(I)V", true);

            // currentTextureId = 0;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "currentTextureId", "I");

            mv.visitLabel(lSkipDelete);

            // isDone = true;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "isDone", "Z");

            // decoder.finish();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitMethodInsn(INVOKEINTERFACE, decoderName, "finish", "()V", true);

            // decoder = null;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ACONST_NULL);
            mv.visitFieldInsn(PUTFIELD, className, "decoder", decoderDesc);

            // Global.getSector().removeTransientScript(this);
            mv.visitMethodInsn(INVOKESTATIC, globalName, "getSector", "()Lcom/fs/starfarer/api/campaign/SectorAPI;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(SectorAPI.class), "removeTransientScript", "(Lcom/fs/starfarer/api/EveryFrameScript;)V", true);

            // Global.getCombatEngine().removePlugin(this);
            mv.visitMethodInsn(INVOKESTATIC, globalName, "getCombatEngine", "()Lcom/fs/starfarer/api/combat/CombatEngineAPI;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(CombatEngineAPI.class), "removePlugin", "(Lcom/fs/starfarer/api/combat/EveryFrameCombatPlugin;)V", true);

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isRendering", "()Z", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0); // load 'this'
            mv.visitFieldInsn(GETFIELD, className, "isRendering", "Z"); // get field
            mv.visitInsn(IRETURN); // return boolean
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "paused", "()Z", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "paused", "Z");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "play", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z"); // paused = false
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "stop", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z"); // paused = true
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "pause", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z"); // paused = true
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "unpause", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z"); // paused = false
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getWidth", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "width", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getHeight", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "height", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isDone", "()Z", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "isDone", "Z");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "runWhilePaused", "()Z", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "runWhilePaused", "Z");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getDecoder", "()" + decoderDesc, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getPlayMode", "()" + playModeDesc, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "MODE", playModeDesc);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setPlayMode", "(" + playModeDesc + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "MODE", playModeDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getEOFMode", "()" + eofModeDesc, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "EOF_MODE", eofModeDesc);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setEOFMode", "(" + eofModeDesc + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "EOF_MODE", eofModeDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getSpeakers", "()" + Type.getDescriptor(Speakers.class), null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getControlPanel", "()" + Type.getDescriptor(PlayerControlPanel.class), null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "init", "(" + combatEngineDesc + ")V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "processInputPreCoreControls", "(F" + listDesc + ")V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "renderInUICoords", "(" + viewPortDesc + ")V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "renderInWorldCoords", "(" + viewPortDesc + ")V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "restart", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();
        transientTexClass = Inherit.inheritCl.define(classBytes, className.replace("/", "."));
        Inherit.dumpClass(classBytes, "DSADSADSADSA.class");

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // public class UiUtilInterface extends Object implements this crap
        cw.visit(
            V17,
            ACC_PUBLIC,
            "data/scripts/projector/TransientTexInstantiator",
            null,
            "java/lang/Object",
            new String[] {Type.getInternalName(TransientTexInstantiator.class)}
        );

        // public TransientTexInstantiator() {
        //     super(); // Object()
        // }
        ctor = cw.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        );
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        {
            String transientTexClassName = Type.getInternalName(transientTexClass);
            // public Projector instantiate(String videoId, int width, int height);
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiate",
                "(Ljava/lang/String;II)" + Type.getDescriptor(Projector.class),
                null,
                null
            );
            
            mv.visitCode();
            
            mv.visitTypeInsn(NEW, transientTexClassName);
            mv.visitInsn(DUP);
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            
            mv.visitMethodInsn(
                INVOKESPECIAL,
                transientTexClassName,
                "<init>",
                "(Ljava/lang/String;II)V",
                false
            );
            
            mv.visitInsn(ARETURN);
            
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        Class<?> instantiatorClass = Inherit.inheritCl.define(cw.toByteArray(), "data/scripts/projector/TransientTexInstantiator".replace("/", "."));
        instantiator = (TransientTexInstantiator) RolfLectionUtil.instantiateClass(instantiatorClass.getConstructors()[0]);
    }

    public static void init() {}

    // private static final int TIMEOUT_FRAMES = 4;

    // private int timeoutFrames;
    // private boolean isDone;
    // private boolean runWhilePaused;

    // private String videoFilePath;
    // private int width;
    // private int height;

    // private boolean paused = false;
    // private boolean isRendering = true;

    // private PlayMode MODE;
    // private EOFMode EOF_MODE;

    // private Decoder decoder;
    // private TexBuffer textureBuffer;
    // private int currentTextureId;

    // public TransientTexProjector(String videoId, int width, int height) {
    //     super(GL11.GL_TEXTURE_2D, 0);

    //     this.videoFilePath = VideoPaths.getVideoPath(videoId);
    //     this.width = width;
    //     this.height = height;

    //     this.MODE = PlayMode.PLAYING;
    //     this.EOF_MODE = EOFMode.LOOP;

    //     this.isDone = true;
    //     this.runWhilePaused = true;
    //     this.paused = false;
    //     this.isRendering = true;
    //     this.timeoutFrames = 0;
    // }
    
    // @Override // bind
    // public void Ø00000() {
    //     timeoutFrames = 0;

    //     if (decoder == null && isDone) {
    //         decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
    //         decoder.start(0);
    //         textureBuffer = decoder.getTextureBuffer();
    //         isDone = false;
    //         currentTextureId = decoder.getCurrentVideoTextureId();
    //         data.scripts.util.TexReflection.setTexObjId((com.fs.graphics.Object)this, currentTextureId);
    //         Global.getSector().addTransientScript(this);
    //         Global.getCombatEngine().addPlugin(this);
    //     }

    //     super.Ø00000();
    // }

    // @Override
    // public void advance(float dt) {
    //     if (decoder != null && timeoutFrames++ > TIMEOUT_FRAMES) {
    //         finish();
    //         timeoutFrames= 0;
    //         return;
    //     }
    //     if (decoder == null || paused) return;

    //     int newId = decoder.getCurrentVideoTextureId(dt);
    //     if (newId != currentTextureId) {
    //         data.scripts.util.TexReflection.setTexObjId((com.fs.graphics.Object)this, newId);
    //         print(newId, data.scripts.util.TexReflection.getTexObjId(this));

    //         if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
    //         currentTextureId = newId;
    //     }
    // }

    // @Override
    // public void advance(float dt, List<InputEventAPI> arg1) {
    //     if (decoder != null && timeoutFrames++ > TIMEOUT_FRAMES) {
    //         finish();
    //         timeoutFrames= 0;
    //         return;
    //     }
    //     if (decoder == null || paused) return;

    //     int newId = decoder.getCurrentVideoTextureId(dt);
    //     if (newId != currentTextureId) {
    //         data.scripts.util.TexReflection.setTexObjId((com.fs.graphics.Object)this, newId);

    //         if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
    //         currentTextureId = newId;
    //     }
    // }

    // @Override
    // public boolean isRendering() {
    //     return isRendering;
    // }

    // @Override
    // public boolean paused() {
    //     return paused;
    // }

    // @Override
    // public void play() {
    //     paused = false;
    // }

    // @Override
    // public void stop() {
    //     paused = true;
    // }

    // @Override
    // public void pause() {
    //     paused = true;
    // }

    // @Override
    // public void unpause() {
    //     paused = false;
    // }

    // @Override
    // public void setIsRendering(boolean isRendering) {
    //     this.isRendering = isRendering;
    // }

    // @Override
    // public void restart() {

    // }

    // @Override
    // public void finish() {
    //     if (currentTextureId != 0) {
    //         textureBuffer.deleteTexture(currentTextureId);
    //         currentTextureId = 0;
    //     }

    //     isDone = true;
    //     decoder.finish();
    //     decoder = null;
    //     Global.getSector().removeTransientScript(this);
    //     Global.getCombatEngine().removePlugin(this);
    // }

    // @Override
    // public Decoder getDecoder() {
    //     return decoder;
    // }

    // @Override
    // public PlayMode getPlayMode() {
    //     return MODE;
    // }

    // @Override
    // public void setPlayMode(PlayMode mode) {
    //     this.MODE = mode;
    // }

    // @Override
    // public EOFMode getEOFMode() {
    //     return EOF_MODE;
    // }

    // @Override
    // public void setEOFMode(EOFMode mode) {
    //     this.EOF_MODE = mode;
    // }

    // @Override
    // public int getWidth() {
    //     return width;
    // }

    // @Override
    // public int getHeight() {
    //     return height;
    // }

    // @Override
    // public boolean isDone() {
    //     return isDone;
    // }

    // @Override
    // public boolean runWhilePaused() {
    //     return runWhilePaused;
    // }

    // @Override
    // public Speakers getSpeakers() {
    //     return null;
    // }

    // @Override
    // public PlayerControlPanel getControlPanel() {
    //     return null;
    // }

    // @Override public void init(CombatEngineAPI arg0) {}
    // @Override public void processInputPreCoreControls(float arg0, List<InputEventAPI> arg1) {}
    // @Override public void renderInUICoords(ViewportAPI arg0) {}
    // @Override public void renderInWorldCoords(ViewportAPI arg0) {}
}
