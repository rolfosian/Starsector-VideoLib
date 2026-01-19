package data.scripts.projector;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.*;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.VideoPaths;
import data.scripts.buffers.TexBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.speakers.Speakers;
import data.scripts.util.TexReflection;
// import rolflectionlib.inheritor.Inherit;

/**
 * Direct subclass of obfuscated Texture class implementing EveryFrameScript and Projector interfaces. Automatically stops and starts itself depending on if it is being rendered or not.
 */
public class AutoTexProjector implements Opcodes {
    public static interface AutoTexInstantiator {
        public AutoTexProjectorAPI instantiate(String videoId, int width, int height);
    }
    public static interface AutoTexProjectorAPI extends EveryFrameScript, Projector {
        public void changeVideo(String videoId, int width, int height, long startVideoUs);
        public void timeout();
        public void setOriginalTexture(String texturePath, Object texture);
    }

    public static void print(String msg) {
        Logger.getLogger(AutoTexProjector.class).info(msg);
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
        AutoTexClassLoader cl = new AutoTexClassLoader();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        String className = "data/scripts/projector/AutoTexProjektor";
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
        
        String texReflectionInternal = Type.getInternalName(TexReflection.class);
        String bindMethodName = TexReflection.texObjectBindMethodName;
        
        // public class AutoTexProjektor extends textureClass implements EveryFrameScript, Projector
        cw.visit(
            V17,
            ACC_PUBLIC,
            className,
            null,
            superName,
            new String[] {
                Type.getInternalName(AutoTexProjectorAPI.class)
            }
        );

        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "TIMEOUT_FRAMES", "I", null, 2);

        cw.visitField(ACC_PRIVATE, "timeoutFrames", "I", null, 0).visitEnd();
        cw.visitField(ACC_PRIVATE, "timedOut", "Z", null, null).visitEnd();
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
        cw.visitField(ACC_PRIVATE, "currentTextureId", "I", null, null).visitEnd();

        cw.visitField(ACC_PRIVATE, "originalTexture", "Ljava/lang/Object;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE, "originalTexturePath", "Ljava/lang/String;", null, null).visitEnd();
    
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

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_1);
        ctor.visitFieldInsn(PUTFIELD, className, "timedOut", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_1);
        ctor.visitFieldInsn(PUTFIELD, className, "isDone", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_1);
        ctor.visitFieldInsn(PUTFIELD, className, "runWhilePaused", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_0);
        ctor.visitFieldInsn(PUTFIELD, className, "paused", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_1);
        ctor.visitFieldInsn(PUTFIELD, className, "isRendering", "Z");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitInsn(ICONST_0);
        ctor.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitTypeInsn(NEW, muteDecoderName);
        ctor.visitInsn(DUP);
    
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "videoFilePath", "Ljava/lang/String;");
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "width", "I");
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "height", "I");
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "MODE", playModeDesc);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "EOF_MODE", eofModeDesc);
    
        ctor.visitMethodInsn(
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
    
        ctor.visitFieldInsn(
            PUTFIELD,
            className,
            "decoder",
            decoderDesc
        );

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
        ctor.visitInsn(LCONST_0);
        ctor.visitMethodInsn(
            INVOKEINTERFACE,
            decoderName,
            "start",
            "(J)V",
            true
        );

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
        ctor.visitMethodInsn(INVOKEINTERFACE, decoderName, "getTextureBuffer", "()" + texBufferDesc, true);
        ctor.visitFieldInsn(PUTFIELD, className, "textureBuffer", texBufferDesc);

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
        ctor.visitMethodInsn(
            INVOKEINTERFACE,
            decoderName,
            "getCurrentVideoTextureId",
            "()I",
            true
        );
        ctor.visitFieldInsn(
            PUTFIELD,
            className,
            "currentTextureId",
            "I"
        );

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
        ctor.visitMethodInsn(
            INVOKESTATIC,
            texReflectionName,
            "setTexObjId",
            "(Ljava/lang/Object;I)V",
            false
        );

        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
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
        
            Label notTimedOut = new Label();

            // if (!timedOut) goto notTimedOut
            bind.visitVarInsn(ALOAD, 0);
            bind.visitFieldInsn(GETFIELD, className, "timedOut", "Z");
            bind.visitJumpInsn(IFEQ, notTimedOut);

            // isDone = false;
            bind.visitVarInsn(ALOAD, 0);
            bind.visitInsn(ICONST_0);
            bind.visitFieldInsn(PUTFIELD, className, "isDone", "Z");

            // timedOut = false;
            bind.visitVarInsn(ALOAD, 0);
            bind.visitInsn(ICONST_0);
            bind.visitFieldInsn(PUTFIELD, className, "timedOut", "Z");
        
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
        
            bind.visitLabel(notTimedOut);
        
            /* super.bind() */
            // bind.visitLdcInsn(GL11.GL_TEXTURE_2D);
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
        
            bind.visitInsn(RETURN);
            bind.visitMaxs(0, 0);
            bind.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "changeVideo",
                "(Ljava/lang/String;IIJ)V",
                null,
                null
            );
            mv.visitCode();
        
            Label lSkipDelete = new Label();
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitJumpInsn(IFEQ, lSkipDelete);
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "textureBuffer", texBufferDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                texBufferInternalName,
                "deleteTexture",
                "(I)V",
                true
            );
        
            mv.visitLabel(lSkipDelete);
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "finish",
                "()V",
                true
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(
                INVOKESTATIC,
                Type.getInternalName(VideoPaths.class),
                "getVideoPath",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            );
            mv.visitFieldInsn(
                PUTFIELD,
                className,
                "videoFilePath",
                "Ljava/lang/String;"
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "width", "I");
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitFieldInsn(PUTFIELD, className, "height", "I");
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, muteDecoderName);
            mv.visitInsn(DUP);
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "videoFilePath", "Ljava/lang/String;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "width", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "height", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "MODE", playModeDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "EOF_MODE", eofModeDesc);
        
            mv.visitMethodInsn(
                INVOKESPECIAL,
                muteDecoderName,
                "<init>",
                "("
                    + "Ldata/scripts/projector/Projector;"
                    + "Ljava/lang/String;"
                    + "II"
                    + playModeDesc
                    + eofModeDesc
                    + ")V",
                false
            );
        
            mv.visitFieldInsn(
                PUTFIELD,
                className,
                "decoder",
                decoderDesc
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitVarInsn(LLOAD, 4);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "start",
                "(J)V",
                true
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "getTextureBuffer",
                "()" + texBufferDesc,
                true
            );
            mv.visitFieldInsn(
                PUTFIELD,
                className,
                "textureBuffer",
                texBufferDesc
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "decoder", decoderDesc);
            mv.visitMethodInsn(
                INVOKEINTERFACE,
                decoderName,
                "getCurrentVideoTextureId",
                "()I",
                true
            );
            mv.visitFieldInsn(
                PUTFIELD,
                className,
                "currentTextureId",
                "I"
            );
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "currentTextureId", "I");
            mv.visitMethodInsn(
                INVOKESTATIC,
                texReflectionInternal,
                "setTexObjId",
                "(Ljava/lang/Object;I)V",
                false
            );
        
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setOriginalTexture", "(Ljava/lang/String;Ljava/lang/Object;)V", null, null);
            mv.visitCode();
        
            Label isNonNull = new Label();
        
            // if (this.originalTexture != null)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexture", "Ljava/lang/Object;");
            mv.visitJumpInsn(IFNULL, isNonNull);
        
            // throw new IllegalArgumentException("originalTexture is already set for this texture override");
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("originalTexture is already set for this texture override");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
        
            mv.visitLabel(isNonNull);
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "originalTexture", "Ljava/lang/Object;");
        
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "originalTexturePath", "Ljava/lang/String;");

            mv.visitInsn(RETURN);
        
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "timeout", "()V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "timedOut", "Z");
            
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "isDone", "Z");
            
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");
            
            mv.visitMethodInsn(INVOKESTATIC, globalName, "getSector", "()Lcom/fs/starfarer/api/campaign/SectorAPI;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(SectorAPI.class), "removeTransientScript", "(Lcom/fs/starfarer/api/EveryFrameScript;)V", true);

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "advance", "(F)V", null, null);
            mv.visitCode();

            Label lContinue = new Label();
            Label lReturn = new Label();
            Label lSkipDelete = new Label();

            // if (!timedOut)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "timedOut", "Z");
            mv.visitJumpInsn(IFNE, lReturn);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, className, "timeoutFrames", "I");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, className, "timeoutFrames", "I");
            
            mv.visitFieldInsn(GETSTATIC, className, "TIMEOUT_FRAMES", "I");
            mv.visitJumpInsn(IF_ICMPLE, lContinue);
            
            // timed out
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "timeout", "()V", false);

            mv.visitInsn(RETURN);
            mv.visitLabel(lContinue);

            // if (!paused)
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
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, texReflectionInternal, "setTexObjId", "(Ljava/lang/Object;I)V", false);

            // // if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
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

            // paused = true;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z");

            // isRendering = false;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "isRendering", "Z");

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

            Label lReturn = new Label();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexturePath", "Ljava/lang/String;");
            mv.visitJumpInsn(IFNULL, lReturn);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexture", "Ljava/lang/Object;");
            mv.visitJumpInsn(IFNULL, lReturn);

            mv.visitFieldInsn(GETSTATIC, texReflectionName, "texObjectMap", "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexturePath", "Ljava/lang/String;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexture", "Ljava/lang/Object;");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "originalTexturePath", "Ljava/lang/String;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(
                INVOKESTATIC,
                Type.getInternalName(VideoPaths.class),
                "removeAutoTexOverride",
                "(Ljava/lang/String;" + Type.getDescriptor(AutoTexProjectorAPI.class) + ")V",
                false
            );

            mv.visitLabel(lReturn);

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isRendering", "()Z", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "isRendering", "Z");
            mv.visitInsn(IRETURN);

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
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z");

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "stop", "()V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z");

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "pause", "()V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z");

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "unpause", "()V", null, null);
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, className, "paused", "Z");

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
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "restart", "()V", null, null);
            mv.visitCode();
            
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();
        // Inherit.dumpClass(classBytes, "DSAIFHSAFHSA.class");
        transientTexClass = cl.define(classBytes, className.replace("/", "."));

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        // public class UiUtilInterface extends Object implements this crap
        cw.visit(
            V17,
            ACC_PUBLIC,
            "data/scripts/projector/AutoTexInstantiator",
            null,
            "java/lang/Object",
            new String[] {Type.getInternalName(AutoTexInstantiator.class)}
        );

        // public AutoTexInstantiator() {
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
            // public AutoTexProjectorAPI instantiate(String videoId, int width, int height);
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "instantiate",
                "(Ljava/lang/String;II)" + Type.getDescriptor(AutoTexProjectorAPI.class),
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

        try {
            instantiator = (AutoTexInstantiator) MethodHandles.lookup().findConstructor(
                cl.define(cw.toByteArray(), "data.scripts.projector.AutoTexInstantiator"),
                MethodType.methodType(void.class)
            ).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final AutoTexInstantiator instantiator;

    private static class AutoTexClassLoader extends ClassLoader {
        public AutoTexClassLoader() {
            super(AutoTexProjector.class.getClassLoader());
        }
        public Class<?> define(byte[] classBytes, String name) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
    }

    public static void init() {}
}

// public class AutoTexProjektor extends com.fs.graphics.Object implements AutoTexProjectorAPI {
//     private static final int TIMEOUT_FRAMES = 50;

//     private int timeoutFrames;
//     private boolean timedOut;
//     private boolean isDone;
//     private boolean runWhilePaused;

//     private String videoFilePath;
//     private int width;
//     private int height;

//     private boolean paused = false;
//     private boolean isRendering = true;

//     private PlayMode MODE;
//     private EOFMode EOF_MODE;

//     private Decoder decoder;
//     private TexBuffer textureBuffer;
//     private int currentTextureId;

//     private Object originalTexture;
//     private String originalTexturePath;

//     public AutoTexProjektor(String videoId, int width, int height) {
//         super(GL11.GL_TEXTURE_2D, 0);

//         this.videoFilePath = VideoPaths.getVideoPath(videoId);
//         this.width = width;
//         this.height = height;

//         this.MODE = PlayMode.PLAYING;
//         this.EOF_MODE = EOFMode.LOOP;

//         this.timedOut = true;
//         this.isDone = true;
//         this.runWhilePaused = true;
//         this.paused = true;
//         this.isRendering = true;
//         this.timeoutFrames = 0;

//         this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
//         this.decoder.start(0);
//         this.textureBuffer = decoder.getTextureBuffer();
//         this.currentTextureId = decoder.getCurrentVideoTextureId();
//         TexReflection.setTexObjId(this, currentTextureId);
//     }
    
//     @Override // bind
//     public void Ø00000() {
//         timeoutFrames = 0;

//        if (timedOut) {
//            isDone = false;
//            timedOut = false;
//            Global.getSector().addTransientScript(this);
//        }

//        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
//          super.Ø00000();
//          return;
//     }

//     @Override
//     public void changeVideo(String videoId, int width, int height, long videoStartUs) {
//         if (this.currentTextureId != 0) this.textureBuffer.deleteTexture(this.currentTextureId);
//         this.decoder.finish();

//         this.videoFilePath = VideoPaths.getVideoPath(videoId);
//         this.width = width;
//         this.height = height;

//         this.decoder = new MuteDecoder(this, videoFilePath, width, height, MODE, EOF_MODE);
//         this.decoder.start(videoStartUs);
//         this.textureBuffer = decoder.getTextureBuffer();
//         this.currentTextureId = decoder.getCurrentVideoTextureId();
//         TexReflection.setTexObjId(this, currentTextureId);
//     }

//     @Override
//     public void setOriginalTexture(String texturePath, Object texture) {
//         if (this.originalTexture != null) throw new IllegalArgumentException("originalTexture is already set for this texture override");
//         this.originalTexture = texture;
//         this.originalTexturePath = texturePath;
//     }

//     @Override
//     public void timeout() {
//         timedOut = true;
//         isDone = true;
//         timeoutFrames= 0;
//         Global.getSector().removeTransientScript(this);
//     }

//     @Override
//     public void advance(float dt) {
//         if (!timedOut) {
//             if (timeoutFrames++ > TIMEOUT_FRAMES) {
//                 timeout();
//                 return;
//             }

//             if (!paused) {
//                 int newId = decoder.getCurrentVideoTextureId(dt);
//                 if (newId != currentTextureId) {
//                     TexReflection.setTexObjId(this, newId);

//                     if (currentTextureId != 0) textureBuffer.deleteTexture(currentTextureId);
//                     currentTextureId = newId;
//                 }
//             }
//         }
//     }

//     @Override
//     public boolean isRendering() {
//         return isRendering;
//     }

//     @Override
//     public boolean paused() {
//         return paused;
//     }

//     @Override
//     public void play() {
//         paused = false;
//     }

//     @Override
//     public void stop() {
//         paused = true;
//     }

//     @Override
//     public void pause() {
//         paused = true;
//     }

//     @Override
//     public void unpause() {
//         paused = false;
//     }

//     @Override
//     public void setIsRendering(boolean isRendering) {
//         this.isRendering = isRendering;
//     }

//     @Override
//     public void restart() {

//     }

//     @Override
//     public void finish() {
//         if (currentTextureId != 0) {
//             textureBuffer.deleteTexture(currentTextureId);
//             currentTextureId = 0;
//         }

//         isDone = true;
//         decoder.finish();
//         decoder = null;
//         Global.getSector().removeTransientScript(this);
//         if (originalTexturePath != null && originalTexture != null) {
//              TexReflection.texObjectMap.put(originalTexturePath, originalTexture);
//              VideoPaths.removeAutoTexOverride(originalTexturePath, this);
//         }
//     }

//     @Override
//     public Decoder getDecoder() {
//         return decoder;
//     }

//     @Override
//     public PlayMode getPlayMode() {
//         return MODE;
//     }

//     @Override
//     public void setPlayMode(PlayMode mode) {
//         this.MODE = mode;
//     }

//     @Override
//     public EOFMode getEOFMode() {
//         return EOF_MODE;
//     }

//     @Override
//     public void setEOFMode(EOFMode mode) {
//         this.EOF_MODE = mode;
//     }

//     @Override
//     public int getWidth() {
//         return width;
//     }

//     @Override
//     public int getHeight() {
//         return height;
//     }

//     @Override
//     public boolean isDone() {
//         return isDone;
//     }

//     @Override
//     public boolean runWhilePaused() {
//         return runWhilePaused;
//     }

//     @Override
//     public Speakers getSpeakers() {
//         return null;
//     }

//     @Override
//     public PlayerControlPanel getControlPanel() {
//         return null;
//     }

// }