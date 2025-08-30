package data.scripts.projector;

import java.nio.FloatBuffer;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import data.scripts.VideoMode;
import data.scripts.VideoPaths;
import data.scripts.buffers.TextureBuffer;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.NoSoundDecoder;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class NoSoundVideoProjector extends VideoProjector {
    private static final Logger logger = Logger.getLogger(VideoProjector.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private String videoFilePath;
    private int width, height;
    private VideoMode MODE;
    private VideoMode OLD_MODE;

    private CustomPanelAPI panel;
    private NoSoundDecoder decoder;

    // private final int vboId;
    // private final FloatBuffer quadBuffer;

    // playback/texture state
    private int currentTextureId = 0;
    private volatile boolean isPlaying = false;
    private boolean paused = false;

    private float x = 0f;
    private float y = 0f;

    private int advancingValue = 0;
    private int checkAdvancing = 0;

    public NoSoundVideoProjector(String videoFilename, int width, int height, VideoMode mode) {
        this.videoFilePath = VideoPaths.map.get(videoFilename);
        this.MODE = mode;

        this.width = width;
        this.height = height;

        this.decoder = new NoSoundDecoder(this, videoFilePath, width, height, mode);
        this.decoder.start();

        // this.vboId = textureBuffer.getVboId();
        // this.quadBuffer = textureBuffer.getQuadBuffer();

        if (mode == VideoMode.PAUSED) {
            paused = true;
            currentTextureId = decoder.requestCurrentVideoTextureId();
            isPlaying = true;
            MODE = VideoMode.LOOP;
            decoder.setMode(VideoMode.LOOP);
        }

        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean isDone = false;

            @Override
            public void advance(float arg0) {
                checkAdvancing ^= 1;

                if (!(checkAdvancing == advancingValue)) {
                    finish();
                    isDone = true;
                    Global.getSector().removeTransientScript(this);
                }
            }

            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }
            
        });
    }

    public void init(PositionAPI panelPos, CustomPanelAPI panel) {
        this.x = panelPos.getX();
        this.y = panelPos.getY();
        this.panel = panel;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {}

    @Override
    public void positionChanged(PositionAPI position) {
        this.x = position.getX();
        this.y = position.getY();
        this.width = (int) position.getWidth();
        this.height = (int) position.getHeight();
    }

    @Override
    public void advance(float amount) {
    	// advance is called by game once per frame
        advancingValue ^= 1;
        if (paused) {
            if (MODE == VideoMode.SEEKING) {
                currentTextureId = decoder.requestCurrentVideoTextureId();
                MODE = OLD_MODE;
            }
            return;
        } 
        currentTextureId = decoder.requestCurrentVideoTextureId(amount);
    }

    @Override
    public void render(float alphaMult) {
        if (!isPlaying) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
    
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, alphaMult);
    
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, y);
    
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x + width, y);
    
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x + width, y + height);
    
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    
        GL11.glColor4f(1f, 1f, 1f, 1f); // reset color
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // GL11.glEnable(GL11.GL_TEXTURE_2D);
        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureId);
    
        // GL11.glEnable(GL11.GL_BLEND);
        // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // GL11.glColor4f(1f, 1f, 1f, alphaMult);
    
        // quadBuffer.clear();
        // quadBuffer.put(new float[] {
        //     x, y + height, 0f, 0f,         // top-left
        //     x + width, y + height, 1f, 0f, // top-right
        //     x + width, y, 1f, 1f,          // bottom-right
        //     x, y, 0f, 1f                   // bottom-left
        // });
        // quadBuffer.flip();
    
        // GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        // GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, quadBuffer);
    
        // GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        // GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    
        // GL11.glVertexPointer(2, GL11.GL_FLOAT, 4 * Float.BYTES, 0);
        // GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 4 * Float.BYTES, 2 * Float.BYTES);
    
        // GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
    
        // GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        // GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    
        // GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        // GL11.glColor4f(1f, 1f, 1f, 1f);
        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        // GL11.glDisable(GL11.GL_BLEND);
        // GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public void start() {
        isPlaying = true;
    }

    public void pause() {
        paused = true;
        this.MODE = VideoMode.PAUSED;
    }

    public void unpause() {
        paused = false;
    }

    public void play() {
        paused = false;

        this.MODE = VideoMode.PLAYING;
        decoder.setMode(VideoMode.LOOP);
        start();
    }

    public void stop() {
        isPlaying = false;
        paused = true;

        if (currentTextureId != 0) GL11.glDeleteTextures(currentTextureId);
        decoder.stop();
    }

    public void seek(double targetSecond) {
        decoder.seek(targetSecond);
    }

    public void restart() {
        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }
    }

    public void finish() {
        isPlaying = false;

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
	}

    public Decoder getDecoder() {
        return this.decoder;
    }

    public boolean paused() {
        return this.paused;
    }

    public void setCurrentTextureId(int id) {
        this.currentTextureId = id;
    }

    public VideoMode getMode() {
        return this.MODE;
    }

    public void setMode(VideoMode mode) {
        this.OLD_MODE = this.MODE;
        this.MODE = mode;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }
}