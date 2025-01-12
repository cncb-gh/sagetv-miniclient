package sagex.miniclient.android.opengl.shapes;

import android.opengl.GLES20;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import sagex.miniclient.android.opengl.OpenGLSurface;
import sagex.miniclient.android.opengl.OpenGLUtils;

public class FillRectangle {
    static final Logger log = LoggerFactory.getLogger(FillRectangle.class);

    private FloatBuffer vertexBuffer;
    static private ShortBuffer drawListBuffer;
    private FloatBuffer colorBuffer;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;

    static short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

    static {
        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
    }

    float squareCoords[] = {
            0, 0,   // top left
            0, 0,   // bottom left
            0, 0,   // bottom right
            0, 0}; // top right

    public FillRectangle() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        colorBuffer = ByteBuffer.allocateDirect(squareCoords.length * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.position(0);
    }

    public void draw(int x1, int y1, int width, int height, int argbTL, int argbTR, int argbBR, int argbBL, OpenGLSurface surface) {
        draw(x1, y1, width, height, argbTL, argbTR, argbBR, argbBL, surface, true);
    }

    public void draw(int x1, int y1, int width, int height, int argbTL, int argbTR, int argbBR, int argbBL, OpenGLSurface surface, boolean blend) {
//        log.debug("FILL: {},{} {},{} - TL:{}, BL:{}, TR: {}, BR: {}",
//            x1,y1,width,height, OpenGLUtils.hexColor(argbTL), OpenGLUtils.hexColor(argbBL), OpenGLUtils.hexColor(argbTR),
//                OpenGLUtils.hexColor(argbBR)
//        );
        // Add program to OpenGL ES environment
        OpenGLUtils.useProgram(OpenGLUtils.defaultShader);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(OpenGLUtils.defaultShader.a_myVertex);
        GLES20.glEnableVertexAttribArray(OpenGLUtils.defaultShader.a_myColor);

        // top/left
        squareCoords[0] = x1;
        squareCoords[1] = y1;

        // bottom/left
        squareCoords[2] = x1;
        squareCoords[3] = y1 + height;

        // bottom/right
        squareCoords[4] = x1 + width;
        squareCoords[5] = y1 + height;

        // top/right
        squareCoords[6] = x1 + width;
        squareCoords[7] = y1;

        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(OpenGLUtils.defaultShader.a_myVertex, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        // set the colors per vertex
        colorBuffer.position(0);
        OpenGLUtils.putToFloatBuffer(argbTL, colorBuffer);
        OpenGLUtils.putToFloatBuffer(argbBL, colorBuffer);
        OpenGLUtils.putToFloatBuffer(argbBR, colorBuffer);
        OpenGLUtils.putToFloatBuffer(argbTR, colorBuffer);
        colorBuffer.position(0);
        GLES20.glVertexAttribPointer(
                OpenGLUtils.defaultShader.a_myColor, 4,
                GLES20.GL_FLOAT, true, 0, colorBuffer);

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(OpenGLUtils.defaultShader.u_myPMVMatrix,
                1, false, surface.viewMatrix, 0);


        if (blend) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // Draw the triangles with a draw list buffer
        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(OpenGLUtils.defaultShader.a_myVertex);
        GLES20.glDisableVertexAttribArray(OpenGLUtils.defaultShader.a_myColor);

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}