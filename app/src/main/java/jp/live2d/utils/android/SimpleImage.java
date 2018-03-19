/*
   You can modify and use this source freely
   only for the development of application related Live2D.

   (c) Live2D Inc. All rights reserved.
 */
package jp.live2d.utils.android;

import javax.microedition.khronos.opengles.GL10;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/*
 * 背景などの画像を表示する。
 *
 */
public final class SimpleImage {
    private FloatBuffer drawImageBufferUv = null;
    private FloatBuffer drawImageBufferVer = null;
    private ShortBuffer drawImageBufferIndex = null;
    private float imageLeft;
    private float imageRight;
    private float imageTop;
    private float imageBottom;
    private float uvLeft;
    private float uvRight;
    private float uvTop;
    private float uvBottom;
    private int texture;

    public SimpleImage(GL10 gl, InputStream in) {
        texture = LoadUtil.loadTexture(gl, in, true);

        // 初期設定
        this.uvLeft = 0;
        this.uvRight = 1;
        this.uvBottom = 0;
        this.uvTop = 1;

        this.imageLeft = -1;
        this.imageRight = 1;
        this.imageBottom = -1;
        this.imageTop = 1;
    }

    public final void draw(GL10 gl) {
        float uv[] = {uvLeft, uvBottom, uvRight, uvBottom, uvRight, uvTop, uvLeft, uvTop};
        float ver[] = {imageLeft, imageTop, imageRight, imageTop, imageRight, imageBottom, imageLeft, imageBottom};
        short index[] = {0, 1, 2, 0, 2, 3};

        drawImageBufferUv = BufferUtil.setupFloatBuffer(drawImageBufferUv, uv);
        drawImageBufferVer = BufferUtil.setupFloatBuffer(drawImageBufferVer, ver);
        drawImageBufferIndex = BufferUtil.setupShortBuffer(drawImageBufferIndex, index);

        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, drawImageBufferUv);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, drawImageBufferVer);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
        gl.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_SHORT, drawImageBufferIndex);
    }

    /*
     * テクスチャの描画先の座標を設定(デフォルトは 0,0,1,1 に描かれる)
     *
     * @param left
     * @param right
     * @param bottom
     * @param top
     */
    public final void setDrawRect(float left, float right, float bottom, float top) {
        this.imageLeft = left;
        this.imageRight = right;
        this.imageBottom = bottom;
        this.imageTop = top;
    }

    /*
     * テクスチャの使用範囲を設定（テクスチャは0..1座標）
     * @param left
     * @param right
     * @param bottom
     * @param top
     */
    public final void setUVRect(float left, float right, float bottom, float top) {
        this.uvLeft = left;
        this.uvRight = right;
        this.uvBottom = bottom;
        this.uvTop = top;
    }
}