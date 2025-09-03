package data.scripts.projector;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.combat.entities.terrain.Planet;

import data.scripts.VideoPaths;
import data.scripts.VideoModes.EOFMode;
import data.scripts.VideoModes.PlayMode;
import data.scripts.decoder.Decoder;
import data.scripts.decoder.MuteDecoder;
import data.scripts.playerui.PlayerControlPanel;
import data.scripts.util.TexReflection;

// THIS IS SCUFFED BUT THE CONCEPT HAS PROMISE
/**It is imperative to call this class's finish() method if the player leaves the system its in or something to close the ffmpeg pipe*/
public class PlanetProjector implements EveryFrameScript, Projector {
    private static final Logger logger = Logger.getLogger(VideoProjector.class);
    public static void print(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] instanceof String ? (String) args[i] : String.valueOf(args[i]));
            if (i < args.length - 1) sb.append(' ');
        }
        logger.info(sb.toString());
    }

    private boolean isDone;

    private String videoFilePath;

    private boolean paused = false;

    private PlayMode MODE;
    private PlayMode OLD_MODE;
    private EOFMode EOF_MODE;

    private final MuteDecoder decoder;

    private final Object originalPlanetTexObj;
    private final Planet planet;

    private TextureObjBuffer texObjBuffer = new TextureObjBuffer(10);
    private Object currentTexObj;
    private int currentTextureId;
    private int oldTextureId = 0;

    private float gameFps;
    private float timeAccumulator = 0;
    private final float spf;
    private final float videoFps;

    public PlanetProjector(PlanetAPI campaignPlanet, String videoId, int width, int height) {
        this.videoFilePath = VideoPaths.get(videoId);

        this.planet = TexReflection.getPlanetFromCampaignPlanet(campaignPlanet);
        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet);

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start();

        this.spf = decoder.getSpf();
        this.videoFps = decoder.getVideoFps();

        this.currentTextureId = decoder.getCurrentVideoTextureId();
        this.currentTexObj = TexReflection.instantiateTexObj(currentTextureId, GL11.GL_TEXTURE_2D);
    }

    public PlanetProjector(Planet planet, String videoId, int width, int height) {
        this.videoFilePath = VideoPaths.get(videoId);
        this.planet = planet;

        this.originalPlanetTexObj = TexReflection.getPlanetTex(this.planet);

        this.MODE = PlayMode.PLAYING;
        this.EOF_MODE = EOFMode.LOOP;
        this.decoder = new MuteDecoder(this, videoFilePath, width, height, this.MODE, this.EOF_MODE);
        this.decoder.start();

        this.spf = decoder.getSpf();
        this.videoFps = decoder.getVideoFps();

        this.texObjBuffer.add(decoder.getCurrentVideoTextureId(), GL11.GL_TEXTURE_2D);
    }
    
    @Override
    public void advance(float deltaTime) {
        if (paused) return;

        gameFps = 1 / deltaTime;
        timeAccumulator += deltaTime;

        boolean switched = false;

        while (timeAccumulator >= spf) {
            timeAccumulator -= spf;
            
            Object textureObj = texObjBuffer.popFront();

            if (textureObj != null) {
                switched = true;
                currentTexObj = textureObj;
                TexReflection.setPlanetTex(planet, currentTexObj);
                if (oldTextureId != 0) GL11.glDeleteTextures(oldTextureId);
            }
        }

        if ((!switched && !texObjBuffer.isFull()) || texObjBuffer.isEmpty()) {
            bufferTipple();
        }
    }

    private void bufferTipple() {
        if (gameFps <= videoFps) {
            texObjBuffer.add(decoder.getCurrentVideoTextureId(), GL11.GL_TEXTURE_2D);

        } else {
            for (int i = 0; i < Math.min(4, texObjBuffer.getSpace() + 1); i++) {
                texObjBuffer.add(decoder.getCurrentVideoTextureId(), GL11.GL_TEXTURE_2D);
            }
        }
    }

    @Override
    public void finish() {
        TexReflection.setPlanetTex(this.planet, this.originalPlanetTexObj);
        texObjBuffer.clear();

        if (currentTextureId != 0) {
            GL11.glDeleteTextures(currentTextureId);
            currentTextureId = 0;
        }

        decoder.finish();
        isDone = true;
        Global.getSector().removeTransientScript(this);
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public boolean isRendering() {
        return true;
    }

    @Override
    public void play() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {
        this.paused = true;
    }

    public void unpause() {
        this.paused = false;
    }

    @Override
    public boolean paused() {
        return this.paused;
    }

    @Override
    public void setIsRendering(boolean isRendering) {}

    @Override
    public Decoder getDecoder() {
        return this.decoder;
    }

    @Override
    public PlayerControlPanel getControlPanel() {
        return null;
    }
    

    private class TextureObjBuffer {
        private Object[] buffer;
        private int[] idBuffer;

        private int head = 0;
        private int tail = 0;
        private int count = 0;
        private int capacity;

        public TextureObjBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
            this.idBuffer = new int[capacity];
        }

        public void add(int id, int glBindType) {
            Object texObj = TexReflection.instantiateTexObj(glBindType, id);
            buffer[tail] = texObj;
            idBuffer[tail] = id;

            tail = (tail + 1) % capacity;
            count++;
        }

        public Object popFront() {
            if (isEmpty()) return null;

            Object texObj = buffer[head];
            oldTextureId = currentTextureId;
            currentTextureId = idBuffer[head];

            buffer[head] = null;
            idBuffer[head] = 0;
            head = (head + 1) % capacity;
            count--;

            return texObj;
        }

        public void clear() {
            int idx = head;
            for (int i = 0; i < count; i++) {
                buffer[idx] = null;
                idBuffer[idx] = 0;
                idx = (idx + 1) % capacity;
            }
            count = 0;
            head = 0;
            tail = 0;
        }

        public int getSpace() {
            return this.capacity - this.count;
        }

        public boolean isFull() {
            return count >= capacity;
        }

        public boolean isEmpty() {
            return count == 0;
        }
    }
}
