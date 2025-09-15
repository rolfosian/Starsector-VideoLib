## VideoLib (Beta)
### A Starsector video/image rendering library powered by JNI-FFmpeg

- **Video codecs**: AV1, H.264, HEVC, VP8, VP9, GIF. Audio: AAC, Opus, Vorbis, MP3.
- **Alpha support**: YUVA420P (WEBM/VP9/VP8). See `data/videos/convert_to_alpha.py` for usage and compatibility notes. Animated GIFs are also converted to OpenGL RGBA textures.
- **Image formats**: PNG, JPEG, WEBP, GIF. Alpha supported for PNG and GIF (WEBP untested).
- **UI embedding**: Anything that can host a `CustomPanelAPI` can host a video.
- **Texture overrides**: Most texture wrappers can be overridden via `TexProjector`. List available texture wrapper ids with console command: `runcode data.scripts.util.TexReflection.printTexWrapperIds()`.
- **Sprite support**: Anything using a `Sprite` can be overridden (may require cloning/setting depending on context).
- **Planet/Ringband support**: Replace planet texture layers (Planet, Cloud, Atmosphere, Glow, Shield, Shield2) and Ringband textures with videos.

## Example: Video UI panel

[Examples](./src/data/scripts/console/)

[For image/video file path resolution use settings.json](./data/config/settings.json)

- **Basic**: Video UI component (no audio)

```java
import data.scripts.VideoPlayerFactory;
// ...

CustomPanelAPI parentPanel = /* your parent panel */;
VideoPlayer videoPlayer;

// With controls
if (splitArgs.contains("wc")) {
    // fileId | width | height | PlayMode | EOFMode | keepAlive
    videoPlayer = VideoPlayerFactory.createMutePlayerWithControls(
        "vl_demo", videoWidth, videoHeight,
        PlayMode.PAUSED, EOFMode.LOOP, false,
        Color.WHITE, Misc.getDarkPlayerColor()
    );
    videoPlayer.setClickToPause(true);
    videoPlayer.addTo(parentPanel).inTL(0f, 0f);
    // Ensure parentPanel is positioned before init()
    videoPlayer.init();
} else {
    // No controls, looping video
    videoPlayer = VideoPlayerFactory.createMutePlayer(
        "vl_demo", videoWidth, videoHeight,
        PlayMode.PLAYING, EOFMode.LOOP, false
    );
    videoPlayer.setClickToPause(true);
    videoPlayer.addTo(parentPanel).inTL(0f, 0f);
    // Ensure parentPanel is positioned before init()
    videoPlayer.init();
}
```

- **Also supported**: Project videos onto [planets](./src/data/scripts/projector/PlanetProjector.java) by choosing the planet texture layer via the constructor's last parameter (Planet, Cloud, Atmosphere, Shield, Shield2). The [RingBand](./src/data/scripts/projector/RingBandProjector.java) and [Sprite](./src/data/scripts/projector/SpriteProjector.java) classes are also supported. Similar patterns can be extended to other entities (e.g., asteroids). For many concurrent projectors, consider viewport/location to manage performance and throttle with pause/unpause.

## Tips
- **Prefer 20–40 fps videos**: Leaves headroom for texture upload to gpu and seeking.
- **Tune encodes**: Balance bitrate and keyframe density; seeking performance varies dramatically by encode and codec balancing resolution. AV1 seems to work well at 720p for example whereas VP9 is belligerent while seeking at this res
- **Planet shields**: Only use `PlanetTexType.SHIELD` if the planet will not gain a planetary shield during the projector's lifetime, or you have preprocessed a blended video with it overlayed. Prefer `PlanetTexType.SHIELD2` to replace the layer beneath an existing shield.
- **Chroma keying**: With `SHIELD`/`SHIELD2`, black pixels are chroma-keyed by the planet renderer and rendered transparent. We don't have direct access to the draw call to change this behavior afaik.
- **Cleanup is mandatory**: Projectors spawn decoder threads and manage native buffers. Always call `projector.finish()` when done (NOT `decoder.finish()`, `projector.finish()` will handle that). Not doing so can leak memory, keep threads alive, and fail to restore original textures/specs (especially for `PlanetProjector`). For standard Video UI panels: if `keepAlive=false`, the projector cleans up automatically when removed and stops advancing; if `keepAlive=true`, you must call `finish()` yourself.

## TODO
- Redo package semantics
- Audio for Planet/RingBand/Tex projectors; directional audio for `PlanetProjector` based on viewport
- Documentation and more examples
- Polish UI
- Thorough testing

## Notes
- Each decoded video frame is uploaded to an OpenGL texture via a ring buffer, driven by a worker thread through FFmpeg JNI bindings.
- A/V desync may occur and audio will probably stutter if the game fps slows to below the video framerate. It probably won't take long for the video to catch up to the audio in case of a/v desync