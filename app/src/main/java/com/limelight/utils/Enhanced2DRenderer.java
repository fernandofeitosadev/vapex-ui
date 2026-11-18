package com.limelight.utils;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.limelight.LimeLog;
import com.limelight.preferences.PreferenceConfiguration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Enhanced2DRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "EnhancedRenderer";

    private static final float[] QUAD_VERTICES = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
    };
    private static final float[] TEXTURE_VERTICES = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    private final GLSurfaceView glSurfaceView;
    private final OnSurfaceReadyListener onSurfaceReadyListener;
    private final PreferenceConfiguration prefConfig;

    private final FloatBuffer quadBuffer;
    private final FloatBuffer texBuffer;

    private int program;
    private int textureId;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private final AtomicBoolean frameAvailable = new AtomicBoolean(false);

    public interface OnSurfaceReadyListener {
        void onEnhancedSurfaceReady(Surface surface);
    }

    public Enhanced2DRenderer(GLSurfaceView view, OnSurfaceReadyListener listener, PreferenceConfiguration prefConfig) {
        this.glSurfaceView = view;
        this.onSurfaceReadyListener = listener;
        this.prefConfig = prefConfig;

        quadBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        quadBuffer.put(QUAD_VERTICES).position(0);

        texBuffer = ByteBuffer.allocateDirect(TEXTURE_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(TEXTURE_VERTICES).position(0);
        
        LimeLog.info("[" + TAG + "] Enhanced2DRenderer initialized");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LimeLog.info("[" + TAG + "] onSurfaceCreated");
        
        textureId = createTexture();
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);

        program = createProgram(ShaderUtils.VERTEX_SHADER, ShaderUtils.SHARPEN_FRAGMENT_SHADER);

        if (onSurfaceReadyListener != null) {
            onSurfaceReadyListener.onEnhancedSurfaceReady(surface);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LimeLog.info("[" + TAG + "] onSurfaceChanged: " + width + "x" + height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (frameAvailable.compareAndSet(true, false)) {
            surfaceTexture.updateTexImage();
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);

        int posHandle = GLES20.glGetAttribLocation(program, "a_Position");
        int texHandle = GLES20.glGetAttribLocation(program, "a_TexCoord");
        int texUniform = GLES20.glGetUniformLocation(program, "s_Texture");
        int sizeUniform = GLES20.glGetUniformLocation(program, "u_TexelSize");
        int sharpUniform = GLES20.glGetUniformLocation(program, "u_Sharpness");

        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, quadBuffer);

        GLES20.glEnableVertexAttribArray(texHandle);
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(texUniform, 0);

        // Define a nitidez (0.3 é o valor ideal para evitar ruído)
        float sharpness = 0.3f;
        GLES20.glUniform1f(sharpUniform, sharpness);
        GLES20.glUniform2f(sizeUniform, 1.0f / glSurfaceView.getWidth(), 1.0f / glSurfaceView.getHeight());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        frameAvailable.set(true);
        glSurfaceView.requestRender();
    }

    private int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return textures[0];
    }

    private int createProgram(String vertex, String fragment) {
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vShader);
        GLES20.glAttachShader(prog, fShader);
        GLES20.glLinkProgram(prog);
        LimeLog.info("[" + TAG + "] Shader program created");
        return prog;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void onDestroy() {
        LimeLog.info("[" + TAG + "] onDestroy called");
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }
}
