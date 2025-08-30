#include <jni.h>
#include <math.h>
#include "data_scripts_FFmpeg.h"

#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/rational.h>
#include <libavutil/imgutils.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/channel_layout.h>
#include <libavutil/audio_fifo.h>
#include <pthread.h>

JNIEXPORT void JNICALL printe(JNIEnv* env, const char* msg) {
    jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/FFmpeg");
    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "print", "([Ljava/lang/Object;)V");

    jclass objectClass = (*env)->FindClass(env, "java/lang/Object");

    jobjectArray args = (*env)->NewObjectArray(env, 1, objectClass, NULL);
    jstring jmsg = (*env)->NewStringUTF(env, msg);
    (*env)->SetObjectArrayElement(env, args, 0, jmsg);

    (*env)->CallStaticVoidMethod(env, cls, mid, args);

    (*env)->DeleteLocalRef(env, jmsg);
    (*env)->DeleteLocalRef(env, args);
    (*env)->DeleteLocalRef(env, cls);
    (*env)->DeleteLocalRef(env, objectClass);
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_freeBuffer(JNIEnv *env, jclass cls, jobject buffer) {
    void *ptr = (*env)->GetDirectBufferAddress(env, buffer);
    if (ptr != NULL) {
        free(ptr);
    }
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_init(JNIEnv *env, jclass clazz) {
    avformat_network_init();
}

// for jpeg, png, webp, gif
typedef struct {
    AVFrame *rgb_frame;
    int width;
    int height;
    uint8_t *rgb_buffer;
    int rgb_size;
    jobject byte_buffer; // GlobalRef to DirectByteBuffer wrapping rgb_buffer
} FFmpegImageContext;

JNIEXPORT jlong JNICALL Java_data_scripts_ffmpeg_FFmpeg_openImage(JNIEnv *env, jclass clazz, jstring jfilename, jint width, jint height) {
    const char *filename = (*env)->GetStringUTFChars(env, jfilename, NULL);

    AVFormatContext *fmt_ctx = NULL;
    AVCodecContext *codec_ctx = NULL;
    AVFrame *decoded = NULL;
    AVFrame *rgb_frame = NULL;
    struct SwsContext *sws_ctx = NULL;
    uint8_t *rgb_buffer = NULL;
    int rgb_size = 0;
    int video_stream_index = -1;

    if (avformat_open_input(&fmt_ctx, filename, NULL, NULL) < 0) {
        printe(env, "openImage: failed to open input");
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        printe(env, "openImage: failed to find stream info");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    video_stream_index = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (video_stream_index < 0) {
        printe(env, "openImage: no video stream");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    AVCodecParameters *codecpar = fmt_ctx->streams[video_stream_index]->codecpar;
    const AVCodec *codec = avcodec_find_decoder(codecpar->codec_id);
    if (!codec) {
        printe(env, "openImage: decoder not found");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    codec_ctx = avcodec_alloc_context3(codec);
    if (!codec_ctx) {
        printe(env, "openImage: alloc codec ctx failed");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avcodec_parameters_to_context(codec_ctx, codecpar) < 0) {
        printe(env, "openImage: copy params failed");
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avcodec_open2(codec_ctx, codec, NULL) < 0) {
        printe(env, "openImage: open codec failed");
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    decoded = av_frame_alloc();
    rgb_frame = av_frame_alloc();
    if (!decoded || !rgb_frame) {
        printe(env, "openImage: alloc frames failed");
        if (decoded) av_frame_free(&decoded);
        if (rgb_frame) av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    // Create scaler to requested size
    sws_ctx = sws_getContext(
        codec_ctx->width, codec_ctx->height, codec_ctx->pix_fmt,
        width, height, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, NULL, NULL, NULL
    );
    if (!sws_ctx) {
        printe(env, "openImage: create sws failed");
        av_frame_free(&decoded);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    rgb_size = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
    rgb_buffer = (uint8_t*)av_malloc(rgb_size);
    if (!rgb_buffer) {
        printe(env, "openImage: alloc rgb buffer failed");
        sws_freeContext(sws_ctx);
        av_frame_free(&decoded);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, rgb_buffer, AV_PIX_FMT_RGB24, width, height, 1);

    // Decode first frame
    AVPacket *pkt = av_packet_alloc();
    int got = 0;
    while (av_read_frame(fmt_ctx, pkt) >= 0 && !got) {
        if (pkt->stream_index == video_stream_index) {
            if (avcodec_send_packet(codec_ctx, pkt) >= 0) {
                if (avcodec_receive_frame(codec_ctx, decoded) >= 0) {
                    sws_scale(sws_ctx,
                              (const uint8_t * const *)decoded->data,
                              decoded->linesize,
                              0,
                              codec_ctx->height,
                              rgb_frame->data,
                              rgb_frame->linesize);
                    got = 1;
                }
            }
        }
        av_packet_unref(pkt);
    }
    av_packet_free(&pkt);

    // We only need the RGB buffer after this; release decoding resources
    av_frame_free(&decoded);
    avcodec_free_context(&codec_ctx);
    avformat_close_input(&fmt_ctx);
    sws_freeContext(sws_ctx);
    codec_ctx = NULL;
    fmt_ctx = NULL;
    sws_ctx = NULL;

    if (!got) {
        printe(env, "openImage: failed to decode frame");
        if (rgb_frame) av_frame_free(&rgb_frame);
        if (rgb_buffer) av_free(rgb_buffer);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    FFmpegImageContext *ctx = (FFmpegImageContext *)malloc(sizeof(FFmpegImageContext));
    if (!ctx) {
        printe(env, "openImage: alloc ctx failed");
        av_frame_free(&rgb_frame);
        av_free(rgb_buffer);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    ctx->rgb_frame = rgb_frame;
    ctx->width = width;
    ctx->height = height;
    ctx->rgb_buffer = rgb_buffer;
    ctx->rgb_size = rgb_size;
    ctx->byte_buffer = (*env)->NewDirectByteBuffer(env, ctx->rgb_buffer, ctx->rgb_size);
    if (ctx->byte_buffer) {
        ctx->byte_buffer = (*env)->NewGlobalRef(env, ctx->byte_buffer);
        if (!ctx->byte_buffer) {
            printe(env, "openImage: failed to create GlobalRef for ByteBuffer");
        }
    } else {
        printe(env, "openImage: failed to create DirectByteBuffer");
    }

    (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_closeImage(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegImageContext *ctx = (FFmpegImageContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "closeImage: null context pointer");
        return;
    }

    if (ctx->byte_buffer) {
        (*env)->DeleteGlobalRef(env, ctx->byte_buffer);
        ctx->byte_buffer = NULL;
    }

    if (ctx->rgb_buffer) av_free(ctx->rgb_buffer);
    if (ctx->rgb_frame) av_frame_free(&ctx->rgb_frame);
    free(ctx);
}

JNIEXPORT jobject JNICALL Java_data_scripts_ffmpeg_FFmpeg_getImageBuffer(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegImageContext *ctx = (FFmpegImageContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getImageBuffer: null context pointer");
        return NULL;
    }
    if (!ctx->rgb_buffer || ctx->rgb_size <= 0) {
        printe(env, "getImageBuffer: invalid buffer state");
        return NULL;
    }

    if (!ctx->byte_buffer) {
        jobject bb = (*env)->NewDirectByteBuffer(env, ctx->rgb_buffer, ctx->rgb_size);
        if (!bb) {
            printe(env, "getImageBuffer: failed to create DirectByteBuffer");
            return NULL;
        }
        ctx->byte_buffer = (*env)->NewGlobalRef(env, bb);
        if (!ctx->byte_buffer) {
            printe(env, "getImageBuffer: failed to create GlobalRef");
            return NULL;
        }
    }

    return ctx->byte_buffer;
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_resizeImage(JNIEnv *env, jclass clazz, jlong ptr, jint newWidth, jint newHeight) {
    FFmpegImageContext *ctx = (FFmpegImageContext *)(intptr_t)ptr;
    if (!ctx || !ctx->rgb_buffer || ctx->width <= 0 || ctx->height <= 0) {
        printe(env, "resizeImage: invalid context or buffer state");
        return;
    }
    if (newWidth <= 0 || newHeight <= 0) {
        printe(env, "resizeImage: invalid dimensions");
        return;
    }

    // Source frame backed by existing RGB24 buffer
    AVFrame *src = av_frame_alloc();
    AVFrame *dst = av_frame_alloc();
    if (!src || !dst) {
        printe(env, "resizeImage: failed to allocate frames");
        if (src) av_frame_free(&src);
        if (dst) av_frame_free(&dst);
        return;
    }
    src->data[0] = ctx->rgb_buffer;
    src->linesize[0] = ctx->width * 3;
    src->width = ctx->width;
    src->height = ctx->height;
    src->format = AV_PIX_FMT_RGB24;

    struct SwsContext *sws = sws_getContext(
        ctx->width, ctx->height, AV_PIX_FMT_RGB24,
        newWidth, newHeight, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, NULL, NULL, NULL
    );
    if (!sws) {
        printe(env, "resizeImage: failed to create scaler context");
        av_frame_free(&src);
        av_frame_free(&dst);
        return;
    }

    int new_size = newWidth * newHeight * 3;
    // If shrinking or same size, reuse existing buffer; otherwise allocate new
    uint8_t *target_buffer = NULL;
    int target_linesize = newWidth * 3;
    int will_realloc = (new_size > ctx->rgb_size);

    if (!will_realloc) {
        // scale into a temporary buffer then copy into existing to avoid overlapping issues
        target_buffer = (uint8_t*)av_malloc(new_size);
        if (!target_buffer) {
            printe(env, "resizeImage: failed to allocate temporary buffer");
            sws_freeContext(sws);
            av_frame_free(&src);
            av_frame_free(&dst);
            return;
        }
    } else {
        target_buffer = (uint8_t*)av_malloc(new_size);
        if (!target_buffer) {
            printe(env, "resizeImage: failed to allocate new buffer");
            sws_freeContext(sws);
            av_frame_free(&src);
            av_frame_free(&dst);
            return;
        }
    }
    dst->width = newWidth;
    dst->height = newHeight;
    dst->format = AV_PIX_FMT_RGB24;
    dst->linesize[0] = target_linesize;
    dst->data[0] = target_buffer;

    sws_scale(sws,
              (const uint8_t * const *)src->data,
              src->linesize,
              0,
              ctx->height,
              dst->data,
              dst->linesize);

    // Replace or copy into buffer in context
    if (will_realloc) {
        // Need to replace backing buffer and ByteBuffer
        if (ctx->byte_buffer) {
            (*env)->DeleteGlobalRef(env, ctx->byte_buffer);
            ctx->byte_buffer = NULL;
        }
        av_free(ctx->rgb_buffer);
        ctx->rgb_buffer = target_buffer;
        ctx->rgb_size = new_size;
        // Create new DirectByteBuffer and promote to GlobalRef
        jobject bb = (*env)->NewDirectByteBuffer(env, ctx->rgb_buffer, ctx->rgb_size);
        if (bb) {
            ctx->byte_buffer = (*env)->NewGlobalRef(env, bb);
            if (!ctx->byte_buffer) {
                printe(env, "resizeImage: failed to create GlobalRef for new buffer");
            }
        } else {
            printe(env, "resizeImage: failed to create DirectByteBuffer for new buffer");
        }
    } else {
        // Copy resized content back into existing buffer
        memcpy(ctx->rgb_buffer, target_buffer, new_size);
        av_free(target_buffer);
        ctx->rgb_size = new_size; // logical size; capacity remains larger
    }
    ctx->width = newWidth;
    ctx->height = newHeight;
    if (ctx->rgb_frame) {
        if (av_image_fill_arrays(ctx->rgb_frame->data, ctx->rgb_frame->linesize,
                                 ctx->rgb_buffer, AV_PIX_FMT_RGB24,
                                 ctx->width, ctx->height, 1) < 0) {
            printe(env, "resizeImage: failed to update frame arrays");
        }
    }

    sws_freeContext(sws);
    av_frame_free(&src);
    av_frame_free(&dst);
}

typedef struct {
    AVFormatContext *fmt_ctx;

    // video
    int video_stream_index;
    AVCodecContext *video_ctx;
    AVFrame *video_frame;
    AVFrame *rgb_frame;
    struct SwsContext *sws_ctx;
    int width;
    int height;
    uint8_t *rgb_buffer;
    int rgb_size;
    int64_t frame_count;
    float fps;
    double duration_seconds;
    int64_t duration_us;

    // audio
    int audio_stream_index;
    AVCodecContext *audio_ctx;
    AVFrame *audio_frame;
    struct SwrContext *swr_ctx;
    AVAudioFifo *audio_fifo;
    AVChannelLayout out_ch_layout;
    enum AVSampleFormat out_sample_fmt;
    int out_sample_rate;
    int out_channels;

    // audio output chunking and pts tracking
    int64_t audio_next_pts_us;
    int audio_target_samples;
    uint8_t *audio_out_buffer;
    int audio_out_capacity_bytes;

    int64_t seek_target_us;
    int seeking;

    int error_status;

    pthread_mutex_t mutex;
} FFmpegPipeContext;


JNIEXPORT jlong JNICALL Java_data_scripts_ffmpeg_FFmpeg_openPipeNoSound(JNIEnv *env, jclass clazz, jstring jfilename, jint width, jint height, jint startFrame) {
    const char *filename = (*env)->GetStringUTFChars(env, jfilename, NULL);

    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, filename, NULL, NULL) < 0) {
        printe(env, "openPipeNoSound: failed to open input");
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        printe(env, "openPipeNoSound: failed to find stream info");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    int video_stream_index = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (video_stream_index < 0) {
        printe(env, "openPipeNoSound: no video stream found");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    AVCodecParameters *codecpar = fmt_ctx->streams[video_stream_index]->codecpar;
    const AVCodec *codec = avcodec_find_decoder(codecpar->codec_id);
    if (!codec) {
        printe(env, "openPipeNoSound: decoder not found");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    AVCodecContext *codec_ctx = avcodec_alloc_context3(codec);
    if (!codec_ctx) {
        printe(env, "openPipeNoSound: failed to allocate codec context");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avcodec_parameters_to_context(codec_ctx, codecpar) < 0) {
        printe(env, "openPipeNoSound: failed to copy codec parameters");
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (avcodec_open2(codec_ctx, codec, NULL) < 0) {
        printe(env, "openPipeNoSound: failed to open codec");
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    AVFrame *frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();
    if (!frame || !rgb_frame) {
        printe(env, "openPipeNoSound: failed to allocate frames");
        if (frame) av_frame_free(&frame);
        if (rgb_frame) av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    struct SwsContext *sws_ctx = sws_getContext(
        codec_ctx->width, codec_ctx->height, codec_ctx->pix_fmt,
        width, height, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, NULL, NULL, NULL
    );
    if (!sws_ctx) {
        printe(env, "openPipeNoSound: failed to create scaler context");
        av_frame_free(&frame);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    int rgb_size = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
    uint8_t *rgb_buffer = (uint8_t *)av_malloc(rgb_size);
    if (!rgb_buffer) {
        printe(env, "openPipeNoSound: failed to allocate RGB buffer");
        sws_freeContext(sws_ctx);
        av_frame_free(&frame);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    if (av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, rgb_buffer, AV_PIX_FMT_RGB24, width, height, 1) < 0) {
        printe(env, "openPipeNoSound: failed to fill image arrays");
        av_free(rgb_buffer);
        sws_freeContext(sws_ctx);
        av_frame_free(&frame);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    // Calculate FPS
    AVStream *video_stream = fmt_ctx->streams[video_stream_index];
    AVRational r_frame_rate = video_stream->r_frame_rate;
    float fps = 0.0f;
    if (r_frame_rate.den != 0) {
        fps = (float) r_frame_rate.num / (float) r_frame_rate.den;
    }

    FFmpegPipeContext *ctx = (FFmpegPipeContext *)malloc(sizeof(FFmpegPipeContext));
    if (!ctx) {
        printe(env, "openPipeNoSound: failed to allocate context structure");
        av_free(rgb_buffer);
        sws_freeContext(sws_ctx);
        av_frame_free(&frame);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    ctx->fmt_ctx = fmt_ctx;
    ctx->video_ctx = codec_ctx;
    ctx->video_frame = frame;
    ctx->rgb_frame = rgb_frame;
    ctx->sws_ctx = sws_ctx;
    ctx->video_stream_index = video_stream_index;
    ctx->width = width;
    ctx->height = height;
    ctx->frame_count = 0;
    ctx->rgb_buffer = rgb_buffer;
    ctx->rgb_size = rgb_size;
    ctx->fps = fps;

    if (fmt_ctx->duration > 0) {
        ctx->duration_us = fmt_ctx->duration;
        ctx->duration_seconds = (double)fmt_ctx->duration / (double)AV_TIME_BASE;
    } else {
        ctx->duration_us = 0;
        ctx->duration_seconds = 0.0;
    }

    ctx->seek_target_us = -1;
    ctx->seeking = 0;
    ctx->error_status = 0;
    
    int mutex_init_result = pthread_mutex_init(&ctx->mutex, NULL);
    if (mutex_init_result != 0) {
        printe(env, "openPipeNoSound: failed to initialize mutex");
        free(ctx);
        av_free(rgb_buffer);
        sws_freeContext(sws_ctx);
        av_frame_free(&frame);
        av_frame_free(&rgb_frame);
        avcodec_free_context(&codec_ctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    (*env)->ReleaseStringUTFChars(env, jfilename, filename);

    return (jlong)(intptr_t)ctx;
}

JNIEXPORT jobject JNICALL Java_data_scripts_ffmpeg_FFmpeg_readFrameNoSound(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "readFrameNoSound: null context pointer");
        return NULL;
    }

    AVPacket *pkt = av_packet_alloc();
    if (!pkt) {
        printe(env, "readFrameNoSound: failed to allocate packet");
        return NULL;
    }

    jobject result = NULL;

    pthread_mutex_lock(&ctx->mutex);

    AVFrame *last_frame = NULL;
    while (1) {
        int ret = av_read_frame(ctx->fmt_ctx, pkt);
        if (ret < 0) {
            // EOF or error
            ctx->error_status = ret;

            if (ret == AVERROR_EOF) {
                if (ctx->seeking && last_frame) {
                    sws_scale(ctx->sws_ctx,
                              (const uint8_t * const *)last_frame->data,
                              last_frame->linesize,
                              0,
                              ctx->video_ctx->height,
                              ctx->rgb_frame->data,
                              ctx->rgb_frame->linesize);
            
                    void *copy = malloc(ctx->rgb_size);
                    if (!copy) {
                        ctx->seeking = 0;
                        printe(env, "returning null last frame");
                        av_frame_unref(last_frame);
                        pthread_mutex_unlock(&ctx->mutex);
                        av_packet_free(&pkt);
                        return NULL;
                    }
                    memcpy(copy, ctx->rgb_buffer, ctx->rgb_size);
            
                    jobject byteBuffer = (*env)->NewDirectByteBuffer(env, copy, ctx->rgb_size);
            
                    jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/VideoFrame");
                    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/nio/ByteBuffer;J)V");
            
                    jobject result = (*env)->NewObject(env, cls, ctor, byteBuffer, (jlong)last_frame->pts);
                    
                    ctx->seeking = 0;
                    av_frame_unref(last_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return result;
                }

            } else {
                char error_msg[256];
                snprintf(error_msg, sizeof(error_msg), "readFrameNoSound: failed to read frame, error %d", ret);
                printe(env, error_msg);
            }

            av_packet_free(&pkt);
            pthread_mutex_unlock(&ctx->mutex);
            return NULL;
        }

        if (pkt->stream_index == ctx->video_stream_index) {
            ret = avcodec_send_packet(ctx->video_ctx, pkt);
            av_packet_unref(pkt);
            if (ret < 0) {
                char error_msg[256];
                snprintf(error_msg, sizeof(error_msg), "readFrameNoSound: failed to send packet, error %d", ret);
                printe(env, error_msg);
                continue;
            }

            while ((ret = avcodec_receive_frame(ctx->video_ctx, ctx->video_frame)) >= 0) {
                // compute PTS in microseconds
                int64_t pts = av_rescale_q(ctx->video_frame->pts,
                                           ctx->fmt_ctx->streams[ctx->video_stream_index]->time_base,
                                           AV_TIME_BASE_Q);

                // if seeking: skip until first frame >= seek target
                if (ctx->seeking && pts < ctx->seek_target_us) {
                    if (last_frame) av_frame_free(&last_frame);
                    last_frame = av_frame_clone(ctx->video_frame);

                    av_frame_unref(ctx->video_frame);
                    continue;
                }
                ctx->seeking = 0;
                if (last_frame) av_frame_free(&last_frame);

                // convert to RGB
                sws_scale(ctx->sws_ctx,
                          (const uint8_t * const *)ctx->video_frame->data,
                          ctx->video_frame->linesize,
                          0,
                          ctx->video_ctx->height,
                          ctx->rgb_frame->data,
                          ctx->rgb_frame->linesize);

                void *copy = malloc(ctx->rgb_size);
                if (!copy) {
                    printe(env, "readFrameNoSound: failed to allocate frame copy buffer");
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }
                memcpy(copy, ctx->rgb_buffer, ctx->rgb_size);
                
                jobject byteBuffer = (*env)->NewDirectByteBuffer(env, copy, ctx->rgb_size);
                if (!byteBuffer) {
                    printe(env, "readFrameNoSound: failed to create DirectByteBuffer");
                    free(copy);
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }
                
                jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/VideoFrame");
                if (!cls) {
                    printe(env, "readFrameNoSound: failed to find VideoFrame class");
                    free(copy);
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }
                
                jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/nio/ByteBuffer;J)V");
                if (!ctor) {
                    printe(env, "readFrameNoSound: failed to get VideoFrame constructor");
                    free(copy);
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }
                
                result = (*env)->NewObject(env, cls, ctor, byteBuffer, (jlong)pts);
                if (!result) {
                    printe(env, "readFrameNoSound: failed to create VideoFrame object");
                    free(copy);
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }

                av_frame_unref(ctx->video_frame);
                pthread_mutex_unlock(&ctx->mutex);
                av_packet_free(&pkt);
                return result;
            }
            
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                char error_msg[256];
                snprintf(error_msg, sizeof(error_msg), "readFrameNoSound: failed to receive frame, error %d", ret);
                printe(env, error_msg);
            }
        } else {
            av_packet_unref(pkt);
        }
    }

    pthread_mutex_unlock(&ctx->mutex);
    av_packet_free(&pkt);
    return NULL;
}

JNIEXPORT jlong JNICALL Java_data_scripts_ffmpeg_FFmpeg_openPipe(JNIEnv *env, jclass clazz,
    jstring jfilename, jint width, jint height, jint startFrame) {
    const char *filename = (*env)->GetStringUTFChars(env, jfilename, NULL);

    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, filename, NULL, NULL) < 0) {
        printe(env, "Failed to open input");
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    int stream_info_result = avformat_find_stream_info(fmt_ctx, NULL);
    if (stream_info_result < 0) {
        char error_msg[256];
        snprintf(error_msg, sizeof(error_msg), "openPipe: failed to find stream info, error %d", stream_info_result);
        printe(env, error_msg);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    int video_stream_index = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    int audio_stream_index = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (video_stream_index < 0) {
        avformat_close_input(&fmt_ctx);
        printe(env, "Failed to get stream index");
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    // Video decoder
    AVCodecParameters *vpar = fmt_ctx->streams[video_stream_index]->codecpar;
    const AVCodec *vcodec = avcodec_find_decoder(vpar->codec_id);
    if (!vcodec) {
        avformat_close_input(&fmt_ctx);
        printe(env, "Failed to get vcodec");
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    AVCodecContext *vctx = avcodec_alloc_context3(vcodec);
    if (!vctx) {
        printe(env, "openPipe: failed to allocate video codec context");
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    if (avcodec_parameters_to_context(vctx, vpar) < 0) {
        printe(env, "openPipe: failed to copy video codec parameters");
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    if (avcodec_open2(vctx, vcodec, NULL) < 0) {
        printe(env, "Failed to open codec context with codec");
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    // Audio decoder (optional)
    AVCodecContext *actx = NULL;
    AVFrame *aframe = NULL;
    struct SwrContext *swr = NULL;
    AVAudioFifo *fifo = NULL;
    AVChannelLayout out_ch_layout;
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    int out_sample_rate = 0;
    int out_channels = 0;

    if (audio_stream_index >= 0) {
        AVCodecParameters *apar = fmt_ctx->streams[audio_stream_index]->codecpar;
        const AVCodec *acodec = avcodec_find_decoder(apar->codec_id);
        if (acodec) {
            actx = avcodec_alloc_context3(acodec);
            if (!actx) {
                printe(env, "openPipe: failed to allocate audio codec context");
            } else {
                if (avcodec_parameters_to_context(actx, apar) < 0) {
                    printe(env, "openPipe: failed to copy audio codec parameters");
                    avcodec_free_context(&actx);
                    actx = NULL;
                } else if (avcodec_open2(actx, acodec, NULL) < 0) {
                    printe(env, "openPipe: failed to open audio codec");
                    avcodec_free_context(&actx);
                    actx = NULL;
                }
            }
        } else {
            printe(env, "openPipe: audio decoder not found");
        }

        if (actx) {
            aframe = av_frame_alloc();
            if (!aframe) {
                printe(env, "openPipe: failed to allocate audio frame");
                avcodec_free_context(&actx);
                actx = NULL;
            } else {
                // setup resampler to signed 16-bit interleaved at input sample rate, same channels
                AVChannelLayout in_layout = actx->ch_layout;
                out_ch_layout = in_layout;
                out_sample_rate = actx->sample_rate;
                out_channels = in_layout.nb_channels;

                if (out_channels <= 0) {
                    av_channel_layout_default(&out_ch_layout, actx->ch_layout.nb_channels > 0 ? actx->ch_layout.nb_channels : 2);
                    out_channels = out_ch_layout.nb_channels;
                }
                if (out_channels > 2) {
                    av_channel_layout_default(&out_ch_layout, 2);
                    out_channels = 2;
                }

                if (swr_alloc_set_opts2(&swr, &out_ch_layout, out_sample_fmt, out_sample_rate,
                                         &in_layout, actx->sample_fmt, actx->sample_rate, 0, NULL) < 0) {
                    printe(env, "openPipe: failed to create audio resampler");
                    swr = NULL;
                } else if (swr_init(swr) < 0) {
                    printe(env, "openPipe: failed to initialize audio resampler");
                    swr_free(&swr);
                    swr = NULL;
                }

                if (swr) {
                    fifo = av_audio_fifo_alloc(out_sample_fmt, out_channels, 1024);
                    if (!fifo) {
                        printe(env, "openPipe: failed to allocate audio FIFO");
                        swr_free(&swr);
                        swr = NULL;
                    }
                }
            }
        }
    }

    // Video frames and scaler
    AVFrame *vframe = av_frame_alloc();
    AVFrame *rgb = av_frame_alloc();
    if (!vframe || !rgb) {
        printe(env, "openPipe: failed to allocate video frames");
        if (vframe) av_frame_free(&vframe);
        if (rgb) av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    struct SwsContext *sws = sws_getContext(
        vctx->width, vctx->height, vctx->pix_fmt,
        width, height, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, NULL, NULL, NULL
    );
    if (!sws) {
        printe(env, "openPipe: failed to create video scaler context");
        av_frame_free(&vframe);
        av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    int rgb_size = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
    uint8_t *rgb_buffer = (uint8_t *)av_malloc(rgb_size);
    if (!rgb_buffer) {
        printe(env, "openPipe: failed to allocate RGB buffer");
        sws_freeContext(sws);
        av_frame_free(&vframe);
        av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    if (av_image_fill_arrays(rgb->data, rgb->linesize, rgb_buffer, AV_PIX_FMT_RGB24, width, height, 1) < 0) {
        printe(env, "openPipe: failed to fill image arrays");
        av_free(rgb_buffer);
        sws_freeContext(sws);
        av_frame_free(&vframe);
        av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }

    // Calculate FPS
    AVStream *video_stream = fmt_ctx->streams[video_stream_index];
    AVRational r_frame_rate = video_stream->r_frame_rate;
    float fps = 0.0f;
    if (r_frame_rate.den != 0) {
        fps = (float) r_frame_rate.num / (float) r_frame_rate.den;
    }

    FFmpegPipeContext *ctx = (FFmpegPipeContext *)malloc(sizeof(FFmpegPipeContext));
    if (!ctx) {
        printe(env, "openPipe: failed to allocate context structure");
        av_free(rgb_buffer);
        sws_freeContext(sws);
        av_frame_free(&vframe);
        av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    ctx->fmt_ctx = fmt_ctx;

    ctx->video_stream_index = video_stream_index;
    ctx->video_ctx = vctx;
    ctx->video_frame = vframe;
    ctx->rgb_frame = rgb;

    ctx->sws_ctx = sws;
    ctx->width = width;
    ctx->height = height;

    ctx->rgb_buffer = rgb_buffer;
    ctx->rgb_size = rgb_size;

    ctx->frame_count = 0;
    ctx->fps = fps;

    // duration in seconds and microseconds
    if (fmt_ctx->duration > 0) {
        ctx->duration_us = fmt_ctx->duration;
        ctx->duration_seconds = (double)fmt_ctx->duration / (double)AV_TIME_BASE;
    } else {
        ctx->duration_us = 0;
        ctx->duration_seconds = 0.0;
    }

    ctx->audio_stream_index = audio_stream_index;
    ctx->audio_ctx = actx;
    ctx->audio_frame = aframe;
    ctx->swr_ctx = swr;
    ctx->audio_fifo = fifo;
    if (swr) ctx->out_ch_layout = out_ch_layout; else av_channel_layout_uninit(&ctx->out_ch_layout);

    ctx->out_sample_fmt = out_sample_fmt;
    ctx->out_sample_rate = out_sample_rate;
    ctx->out_channels = out_channels;
    ctx->audio_next_pts_us = -1;
    ctx->audio_target_samples = (out_sample_rate > 0 ? out_sample_rate / 4 : 0);
    ctx->audio_out_buffer = NULL;
    ctx->audio_out_capacity_bytes = 0;

    ctx->audio_next_pts_us = -1;
    ctx->audio_target_samples = (out_sample_rate > 0 ? out_sample_rate / 4 : 0);
    ctx->audio_out_buffer = NULL;
    ctx->audio_out_capacity_bytes = 0;

    ctx->seek_target_us = -1;
    ctx->seeking = 0;
    ctx->error_status = 0;
    
    int mutex_init_result = pthread_mutex_init(&ctx->mutex, NULL);
    if (mutex_init_result != 0) {
        printe(env, "openPipe: failed to initialize mutex");
        free(ctx);
        av_free(rgb_buffer);
        sws_freeContext(sws);
        av_frame_free(&vframe);
        av_frame_free(&rgb);
        if (actx) avcodec_free_context(&actx);
        if (aframe) av_frame_free(&aframe);
        if (swr) swr_free(&swr);
        if (fifo) av_audio_fifo_free(fifo);
        avcodec_free_context(&vctx);
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, jfilename, filename);
        return 0;
    }
    
    (*env)->ReleaseStringUTFChars(env, jfilename, filename);
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_closePipe(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ctx_ptr;
    if (!ctx) {
        printe(env, "closePipe: null context pointer");
        return;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    pthread_mutex_destroy(&ctx->mutex);

    if (ctx->video_ctx) {
        avcodec_free_context(&ctx->video_ctx);
    }
    if (ctx->video_frame) {
        av_frame_free(&ctx->video_frame);
    }
    if (ctx->rgb_frame) {
        av_frame_free(&ctx->rgb_frame);
    }
    if (ctx->rgb_buffer) {
        av_free(ctx->rgb_buffer);
    }
    if (ctx->sws_ctx) {
        sws_freeContext(ctx->sws_ctx);
    }

    if (ctx->audio_ctx) {
        avcodec_free_context(&ctx->audio_ctx);
    }
    if (ctx->audio_frame) {
        av_frame_free(&ctx->audio_frame);
    }
    if (ctx->swr_ctx) {
        swr_free(&ctx->swr_ctx);
    }
    if (ctx->audio_fifo) {
        av_audio_fifo_free(ctx->audio_fifo);
    }
    if (ctx->audio_out_buffer) {
        av_free(ctx->audio_out_buffer);
    }
    av_channel_layout_uninit(&ctx->out_ch_layout);

    if (ctx->fmt_ctx) {
        avformat_close_input(&ctx->fmt_ctx);
    }

    free(ctx);
}

JNIEXPORT void JNICALL Java_data_scripts_ffmpeg_FFmpeg_seek(JNIEnv *env, jclass clazz, jlong ptr, jlong targetUs) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "seek: null context pointer");
        return;
    }

    pthread_mutex_lock(&ctx->mutex);

    int64_t ts = av_rescale_q(
        targetUs,
        (AVRational){1, 1000000},
        ctx->fmt_ctx->streams[ctx->video_stream_index]->time_base
    );

    // Seek specifically on the video stream
    int seek_result = av_seek_frame(ctx->fmt_ctx, ctx->video_stream_index, ts, AVSEEK_FLAG_BACKWARD);
    if (seek_result < 0) {
        char error_msg[256];
        snprintf(error_msg, sizeof(error_msg), "seek: failed to seek to position %lld us, error %d", (long long)targetUs, seek_result);
        printe(env, error_msg);
        pthread_mutex_unlock(&ctx->mutex);
        return;
    }

    // Flush decoder state
    avcodec_flush_buffers(ctx->video_ctx);
    if (ctx->audio_ctx) avcodec_flush_buffers(ctx->audio_ctx);
    if (ctx->audio_fifo) av_audio_fifo_reset(ctx->audio_fifo);

    ctx->audio_next_pts_us = -1;
    ctx->seek_target_us = targetUs;
    ctx->seeking = 1;

    pthread_mutex_unlock(&ctx->mutex);
}

JNIEXPORT jobject JNICALL Java_data_scripts_ffmpeg_FFmpeg_read(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ctx_ptr;
    if (!ctx) {
        printe(env, "read: null context pointer");
        return NULL;
    }

    AVPacket *pkt = av_packet_alloc();
    if (!pkt) {
        printe(env, "read: failed to allocate packet");
        return NULL;
    }

    jobject result = NULL;

    pthread_mutex_lock(&ctx->mutex);

    AVFrame *last_frame = NULL; // store last frame while seeking

    while (1) {
        int ret = av_read_frame(ctx->fmt_ctx, pkt);
        if (ret < 0) {
            // EOF or error
            ctx->error_status;

            if (ret == AVERROR_EOF) {
                if (ctx->seeking && last_frame) {
                    // Convert last frame to RGB
                    sws_scale(ctx->sws_ctx,
                              (const uint8_t * const *)last_frame->data,
                              last_frame->linesize,
                              0,
                              ctx->video_ctx->height,
                              ctx->rgb_frame->data,
                              ctx->rgb_frame->linesize);

                    void *copy = malloc(ctx->rgb_size);
                    if (!copy) {
                        ctx->seeking = 0;
                        printe(env, "read: failed to allocate last frame copy buffer");
                        av_frame_unref(last_frame);
                        pthread_mutex_unlock(&ctx->mutex);
                        av_packet_free(&pkt);
                        return NULL;
                    }
                    memcpy(copy, ctx->rgb_buffer, ctx->rgb_size);

                    jobject byteBuffer = (*env)->NewDirectByteBuffer(env, copy, ctx->rgb_size);

                    jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/VideoFrame");
                    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/nio/ByteBuffer;J)V");
                    jobject videoResult = (*env)->NewObject(env, cls, ctor, byteBuffer, (jlong)last_frame->pts);

                    ctx->seeking = 0;
                    av_frame_unref(last_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return videoResult;
                }
            } else {
                char error_msg[256];
                snprintf(error_msg, sizeof(error_msg), "read: failed to read frame, error %d", ret);
                printe(env, error_msg);
            }
            av_packet_free(&pkt);
            pthread_mutex_unlock(&ctx->mutex);
            return NULL;
        }

        if (pkt->stream_index == ctx->video_stream_index) {
            ret = avcodec_send_packet(ctx->video_ctx, pkt);
            av_packet_unref(pkt);
            if (ret < 0) {
                char error_msg[256];
                snprintf(error_msg, sizeof(error_msg), "read: failed to send video packet, error %d", ret);
                printe(env, error_msg);
                continue;
            }

            while ((ret = avcodec_receive_frame(ctx->video_ctx, ctx->video_frame)) >= 0) {
                int64_t pts = av_rescale_q(ctx->video_frame->pts,
                                           ctx->fmt_ctx->streams[ctx->video_stream_index]->time_base,
                                           AV_TIME_BASE_Q);

                if (ctx->seeking && pts < ctx->seek_target_us) {
                    if (last_frame) av_frame_free(&last_frame);
                    last_frame = av_frame_clone(ctx->video_frame);
                    av_frame_unref(ctx->video_frame);
                    continue;
                }
                ctx->seeking = 0;
                if (last_frame) av_frame_free(&last_frame);

                sws_scale(ctx->sws_ctx,
                          (const uint8_t * const *)ctx->video_frame->data,
                          ctx->video_frame->linesize,
                          0,
                          ctx->video_ctx->height,
                          ctx->rgb_frame->data,
                          ctx->rgb_frame->linesize);

                void *copy = malloc(ctx->rgb_size);
                if (!copy) {
                    printe(env, "read: failed to allocate video frame copy buffer");
                    av_frame_unref(ctx->video_frame);
                    pthread_mutex_unlock(&ctx->mutex);
                    av_packet_free(&pkt);
                    return NULL;
                }
                memcpy(copy, ctx->rgb_buffer, ctx->rgb_size);

                jobject byteBuffer = (*env)->NewDirectByteBuffer(env, copy, ctx->rgb_size);

                jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/VideoFrame");
                jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/nio/ByteBuffer;J)V");
                result = (*env)->NewObject(env, cls, ctor, byteBuffer, (jlong)pts);

                av_frame_unref(ctx->video_frame);
                pthread_mutex_unlock(&ctx->mutex);
                av_packet_free(&pkt);
                return result;
            }

        } else if (pkt->stream_index == ctx->audio_stream_index && ctx->audio_ctx) {
            ret = avcodec_send_packet(ctx->audio_ctx, pkt);
            av_packet_unref(pkt);
            if (ret < 0) continue;

            while ((ret = avcodec_receive_frame(ctx->audio_ctx, ctx->audio_frame)) >= 0) {
                int dst_nb_samples = av_rescale_rnd(
                    swr_get_delay(ctx->swr_ctx, ctx->audio_ctx->sample_rate) + ctx->audio_frame->nb_samples,
                    ctx->out_sample_rate,
                    ctx->audio_ctx->sample_rate,
                    AV_ROUND_UP
                );

                uint8_t **converted = NULL;
                if (av_samples_alloc_array_and_samples(&converted, NULL, ctx->out_channels,
                                                       dst_nb_samples, ctx->out_sample_fmt, 0) < 0) {
                    av_frame_unref(ctx->audio_frame);
                    continue;
                }

                int converted_samples = swr_convert(ctx->swr_ctx, converted, dst_nb_samples,
                                                    (const uint8_t **)ctx->audio_frame->data,
                                                    ctx->audio_frame->nb_samples);
                if (converted_samples < 0) {
                    av_freep(&converted[0]);
                    av_freep(&converted);
                    av_frame_unref(ctx->audio_frame);
                    continue;
                }

                int buffer_size = av_samples_get_buffer_size(NULL, ctx->out_channels,
                                                             converted_samples, ctx->out_sample_fmt, 1);
                if (buffer_size < 0) {
                    av_freep(&converted[0]);
                    av_freep(&converted);
                    av_frame_unref(ctx->audio_frame);
                    continue;
                }

                if (ctx->audio_out_capacity_bytes < buffer_size) {
                    if (ctx->audio_out_buffer) av_free(ctx->audio_out_buffer);
                    ctx->audio_out_buffer = (uint8_t *)av_malloc(buffer_size);
                    ctx->audio_out_capacity_bytes = buffer_size;
                }
                memcpy(ctx->audio_out_buffer, converted[0], buffer_size);

                jobject byteBuffer = (*env)->NewDirectByteBuffer(env, ctx->audio_out_buffer, buffer_size);

                jclass cls = (*env)->FindClass(env, "data/scripts/ffmpeg/AudioFrame");
                jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/nio/ByteBuffer;IJ)V");
                result = (*env)->NewObject(env, cls, ctor, byteBuffer, buffer_size,
                                           (jlong)av_rescale_q(ctx->audio_frame->pts,
                                           ctx->fmt_ctx->streams[ctx->audio_stream_index]->time_base,
                                           AV_TIME_BASE_Q));

                av_freep(&converted[0]);
                av_freep(&converted);
                av_frame_unref(ctx->audio_frame);

                pthread_mutex_unlock(&ctx->mutex);
                av_packet_free(&pkt);
                return result;
            }
        } else {
            av_packet_unref(pkt);
        }
    }

    pthread_mutex_unlock(&ctx->mutex);
    av_packet_free(&pkt);
    return NULL;
}

JNIEXPORT jint JNICALL Java_data_scripts_ffmpeg_FFmpeg_getAudioSampleRate(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getAudioSampleRate: null context pointer");
        return 0;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    int result = ctx->out_sample_rate;
    pthread_mutex_unlock(&ctx->mutex);
    return result;
}

JNIEXPORT jint JNICALL Java_data_scripts_ffmpeg_FFmpeg_getAudioChannels(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getAudioChannels: null context pointer");
        return 0;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    int result = ctx->out_channels;
    pthread_mutex_unlock(&ctx->mutex);
    return result;
}

JNIEXPORT jfloat JNICALL Java_data_scripts_ffmpeg_FFmpeg_getVideoFps(JNIEnv *env, jclass clazz, jlong ptr) {
    if (!ptr) {
        printe(env, "getVideoFps: null context pointer");
        return 0.0f;
    }
    
    // Try to cast as FFmpegPipeContext first
    FFmpegPipeContext *ctx_no_sound = (FFmpegPipeContext *)(intptr_t)ptr;
    if (ctx_no_sound && ctx_no_sound->fps > 0.0f) {
        return ctx_no_sound->fps;
    }
    
    // Try to cast as FFmpegPipeContext
    FFmpegPipeContext *ctx_with_sound = (FFmpegPipeContext *)(intptr_t)ptr;
    if (ctx_with_sound && ctx_with_sound->fps > 0.0f) {
        pthread_mutex_lock(&ctx_with_sound->mutex);
        float result = ctx_with_sound->fps;
        pthread_mutex_unlock(&ctx_with_sound->mutex);
        return result;
    }
    
    printe(env, "getVideoFps: invalid context or no FPS information");
    return 0.0f;
}

JNIEXPORT jdouble JNICALL Java_data_scripts_ffmpeg_FFmpeg_getDurationSeconds(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getDurationSeconds: null context pointer");
        return 0.0;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    double result = ctx->duration_seconds;
    pthread_mutex_unlock(&ctx->mutex);

    return result;
}

JNIEXPORT jlong JNICALL Java_data_scripts_ffmpeg_FFmpeg_getDurationUs(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getDurationUs: null context pointer");
        return 0;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    int64_t result = ctx->duration_us;
    pthread_mutex_unlock(&ctx->mutex);

    return result;
}

JNIEXPORT jint JNICALL Java_data_scripts_ffmpeg_FFmpeg_getErrorStatus(JNIEnv *env, jclass clazz, jlong ptr) {
    FFmpegPipeContext *ctx = (FFmpegPipeContext *)(intptr_t)ptr;
    if (!ctx) {
        printe(env, "getDurationUs: null context pointer");
        return 42069;
    }
    
    pthread_mutex_lock(&ctx->mutex);
    int result = ctx->error_status;
    pthread_mutex_unlock(&ctx->mutex);

    return result;
}