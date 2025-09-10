# **WIP**
- Audio support coming soon™, hopefully. impl TBD. Do NOT use the current audio stuff; it does not work in any way.

## Video Lib
### A video library for Starsector with a few rudimentary implementations that render videos or images utilizing some basic? JNI-FFmpeg C glue code

- Each video frame is converted into an OpenGL texture in real time via a ringbuffer wired to a decoder on a worker thread calling from static FFmpeg-JNI glue binaries
- Supports H264, HEVC, VP8, VP9, PNG, JPEG, GIF, and WEBP. Support for alpha channel YUVA420P (WEBM/VP9 - See data/videos/convert_to_alpha.py) and animated GIF conversion to OpenGL RGBA textures also.
- Protip: Do not use (loosely) variable framerate videos or else you will probably encounter issues; if they are very tight variables it probably won't matter though.
- Anything you can put a `CustomPanelAPI` on, you can put a video on.
- Anything that uses a `Sprite` object you can turn into a video with a bit of finesse.
- Planet Projectors also work, we can replace any layer of planet textures with a video (Planet, Cloud, Atmosphere, Glow, Shield, Shield2). Ringband textures can also be replaced with videos. More to come maybe?

## Example Usage for a Video UI panel

[Examples](./src/data/scripts/console/)

[For image/video file path resolution use settings.json](./data/config/settings.json)

- Example usage for a Video UI Component

```java
CustomPanelAPI parentPanel = ...
VideoPlayer videoplayer;

// with controls
if (splitArgs.contains("wc")) {
                                                // file ID defined in data/config/settings.json | starting PlayMode | starting EOFMode | keepAlive?
    videoPlayer = VideoPlayerFactory.createMutePlayerWithControls("vl_demo", videoWidth, videoHeight, PlayMode.PAUSED, EOFMode.LOOP, false, Color.WHITE, Misc.getDarkPlayerColor());
    videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

    videoPlayer.addTo(parentPanel).inTL(0f, 0f); // add to parent
    videoPlayer.init(); // init projector and controls so they know where/height/width to render. *Make sure 'parentPanel' is positioned properly before calling this

// no controls, just a video by itself on loop
} else {                                 // file ID defined in data/config/settings.json | starting PlayMode | starting EOFMode | keepAlive?
    videoPlayer = VideoPlayerFactory.createMutePlayer("vl_demo", videoWidth, videoHeight, PlayMode.PLAYING, EOFMode.LOOP, false);
    videoPlayer.setClickToPause(true); // setClickToPause on the video so user can click it to pause/unpause it

    videoPlayer.addTo(parentPanel).inTL(0f, 0f); // add to parent
    videoPlayer.init(); // init projector so it knows where/width/height to render *Make sure 'parentPanel' is positioned properly before calling this
}
```

- Also supported: Projecting videos onto [planets](./src/data/scripts/projector/PlanetProjector.java) using different planet texture types (Planet, Cloud, Atmosphere, Shield, Shield2, (last constructor parameter)), the [RingBand](./src/data/scripts/projector/RingBandProjector.java) and [Sprite](./src/data/scripts/projector/SpriteProjector.java) classes can also be projected onto. Similar implementations could also be done for more entities such as asteroids. Although concurrent scaling with many projectors going on at once will probably require manual management with viewport/loc parameters (use pause/unpause functions to throttle) as to not incur too much of an overhead penalty.

- Tips
  - Use videos with framerates lower than 60 fps to give more breathing room for texture upload and seeking
  - Test with different encodings, use videos with a good balance of bitrate and keyframe density - will possibly see dramatic differences in seeking performance
  - Only use the `PlanetTexType.SHIELD` parameter for PlanetProjector if you are absolutely sure the planet wont be using, or obtaining a Planetary Shield during the projector's lifetime. Use `PlanetTexType.SHIELD2` if the planet has a shield, and it will replace the texture layer below it.
  - if using `PlanetTexType.SHIELD` or `PlanetTexType.SHIELD2`, black pixels are chroma keyed by the Planet Renderer and rendered transparent. Use this to your advantage for some cool effects, although alpha channel support for YUVA420P (only vp9/webm tested) and GIF has since been added.
  - ***IMPORTANT***: As the projectors use decoders that use worker threads, it is absolutely imperative to call the `finish()` method on the projector when you are done with it. Failure to do so will result in the decoder thread running indefinitely, the ringbuffer leaking memory, and the variables not being reset in the case of `RingBandProjector`, `PlanetProjector`, `SpriteProjector` etc. It is especially important to do this for the `PlanetProjector` to reset the instance specific `PlanetSpec` clones. For standard Video UI Panels such as in the above example, if the `keepAlive` parameter is specified `true`, then you must manually call `finish()` on the projector to clean up when desired. Otherwise, if `keepAlive` is specified `false` then the projector will finish and clean itself up automatically as soon as it stops advancing (usually when the panel component is removed from its parent and stops rendering).

## TODO
- Audio support coming soon™, hopefully. impl TBD `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA` Do NOT use the current audio stuff; it does not work.
- Documentation, Examples?
- Polish gui?
- Thorough testing

## Notes:
- Sound buffers cannot be queued to AL10 device buffers from outside the main thread as they will then conflict with the game's own music player
- I believe some form of interpolation algorithm must be determined for the video frames as a result of this and texture processing ultimately being pegged to the game's framerate. We need to take into account variable framerates of videos and the game's frametime/framerate itself in order to sync correctly with audio frames. I am too much of a brainlet to figure this out thus far.
- Have tried audio and video frames in lockstep but while they were synced it wasn't feasible as the audio had to wait for the video to catch up slightly periodically