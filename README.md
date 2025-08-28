# **WIP**
## Video Lib
### Small UI lib for Starsector with a couple of rudimentary CustomUIPanelPlugin implementations that render videos or images utilizing some basic JNI+FFmpeg C glue code
- Each video is converted into an OpenGL texture in real time via a circular buffer hooked up to a threaded decoder with static ffmpeg+JNI glue binaries
- Supports H264, HEVC, VP8, VP9, PNG, JPEG, GIF, and ||WEBP||
- Protip: Do not use (loosely) variable framerate videos or else you will probably encounter issues; if they are very tight variables it probably won't matter

#TODO
- Github actions scripts for cross platform compilation of static FFmpeg binaries and JNI glue
- Clean up variables, enums usage; create different sets of enums for beginning/playing/ending modes
- Determine file path handling
- Documentation, Examples
- Polish gui
- Thorough testing
- Set up github actions for cross platform compilation of static pic enabled FFmpeg binaries and JNI glue
- Audio support coming soon™ hopefully, impl TBD

## Notes:
- Sound buffers cannot be queued to AL10 device buffers from outside the main thread as they will then conflict with the game's own music player
- I believe some form of interpolation algorithm must be determined for the video frames as a result of being this and being pegged to the game's framerate. We need to take into account variable framerates of videos and the game's frametime/framerate itself in order to sync correctly with audio frames. I am too much of a brainlet to figure this out thus far.
- Have tried audio and video frames in lockstep but while they were synced it sounded awful as the audio had to wait for the video to catch up slightly periodically