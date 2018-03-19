/*
   You can modify and use this source freely
   only for the development of application related Live2D.

   (c) Live2D Inc. All rights reserved.
 */
package jp.live2d.utils.android;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import jp.live2d.util.UtSystem;

/*
 * 加速度センサの情報の管理。
 */
public final class AccelerationHelper {
    private float acceleration_x = 0;
    private float acceleration_y = 0;
    private float acceleration_z = 0;
    private float dst_acceleration_x = 0;
    private float dst_acceleration_y = 0;
    private float dst_acceleration_z = 0;
    private float last_dst_acceleration_x = 0;
    private float last_dst_acceleration_y = 0;
    private float last_dst_acceleration_z = 0;
    private float lastMove = 0;
    private long lastTimeMSec = -1;
    private final Activity activity;
    private final Sensor accelerometer;
    private final Sensor magneticField;
    private final MySensorListener sensorListener;
    private final SensorManager sensorManager;
    private float[] accelerometerValues = new float[3];
    private float[] geomagneticMatrix = new float[3];
    private final float[] acceleration = new float[3];
    private boolean sensorReady;

    public AccelerationHelper(Activity activity) {
        sensorListener = new MySensorListener();
        sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

        this.activity = activity;
        if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0 && sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).size() > 0) {
            accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            magneticField = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
        } else {
            accelerometer = null;
            magneticField = null;
        }
        start();
    }

    /*
     * デバイスを振ったときなどにどのくらい揺れたかを取得。
     * 1を超えるとそれなりに揺れた状態。
     * resetShake()を使ってリセットできる。
     * @return lastMove
     */
    public final float getShake() {
        return lastMove;
    }


    /*
     * シェイクイベントが連続で発生しないように揺れをリセットする。
     */
    public final void resetShake() {
        lastMove = 0;
    }

    /*
     * 計測を開始する
     */
    public final void start() {
        try {
            if (accelerometer == null || magneticField == null)
                return;
            sensorManager.registerListener(sensorListener, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 計測を停止する
     */
    public final void stop() {
        try {
            sensorManager.unregisterListener(sensorListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 画面の回転を取得。
     * @param act
     * @return
     */
    private int getDispRotation(Activity act) {
        Display d = act.getWindowManager().getDefaultDisplay();
        return DisplayRotateGetter.getInstance().getRotate(d);
    }

    /*
     * 加速度が更新された時に呼ばれる
     */
    private void setCurAcceleration(float a1, float a2, float a3) {
        dst_acceleration_x = a1;
        dst_acceleration_y = a2;
        dst_acceleration_z = a3;

        //  以下はシェイクイベント用の処理
        float move =
                fabs(dst_acceleration_x - last_dst_acceleration_x) +
                        fabs(dst_acceleration_y - last_dst_acceleration_y) +
                        fabs(dst_acceleration_z - last_dst_acceleration_z);
        lastMove = lastMove * 0.7f + move * 0.3f;

        last_dst_acceleration_x = dst_acceleration_x;
        last_dst_acceleration_y = dst_acceleration_y;
        last_dst_acceleration_z = dst_acceleration_z;
    }

    /*
     * 更新
     */
    public void update() {
        final float MAX_ACCEL_D = 0.04f; // setCurAccelerationの間隔が長い場合は、最大値を小さくする必要がある
        float dx = dst_acceleration_x - acceleration_x;
        float dy = dst_acceleration_y - acceleration_y;
        float dz = dst_acceleration_z - acceleration_z;

        if (dx > MAX_ACCEL_D)
            dx = MAX_ACCEL_D;
        if (dx < -MAX_ACCEL_D)
            dx = -MAX_ACCEL_D;

        if (dy > MAX_ACCEL_D)
            dy = MAX_ACCEL_D;
        if (dy < -MAX_ACCEL_D)
            dy = -MAX_ACCEL_D;

        if (dz > MAX_ACCEL_D)
            dz = MAX_ACCEL_D;
        if (dz < -MAX_ACCEL_D)
            dz = -MAX_ACCEL_D;

        acceleration_x += dx;
        acceleration_y += dy;
        acceleration_z += dz;

        long time = UtSystem.getUserTimeMSec();
        long diff = time - lastTimeMSec;

        lastTimeMSec = time;

        float scale = 0.2f * diff * 60 / (1000.0f); // 経過時間に応じて、重み付けをかえる
        final float MAX_SCALE_VALUE = 0.5f;
        if (scale > MAX_SCALE_VALUE)
            scale = MAX_SCALE_VALUE;

        acceleration[0] = (acceleration_x * scale) + (acceleration[0] * (1.0f - scale));
        acceleration[1] = (acceleration_y * scale) + (acceleration[1] * (1.0f - scale));
        acceleration[2] = (acceleration_z * scale) + (acceleration[2] * (1.0f - scale));
    }

    /*
     * 絶対値計算
     * @param v
     * @return
     */
    private float fabs(float v) {
        return v > 0 ? v : -v;
    }

    /*
     * 横方向の回転を取得。
     * 寝かせた状態で0。(表裏関係なく)
     * 左に回転させると-1,右に回転させると1になる。
     *
     * @return
     */
    public final float getAccelerationX() {
        return acceleration[0];
    }

    /*
     * 上下の回転を取得。
     * 寝かせた状態で0。(表裏関係なく)
     * デバイスが垂直に立っているときに-1、逆さまにすると1になる。
     *
     * @return
     */
    public final float getAccelerationY() {
        return acceleration[1];
    }

    /*
     * 上下の回転を取得。
     * 立たせた状態で0。
     * 表向きに寝かせると-1、裏向きに寝かせると1になる
     * @return
     */
    public float getAccelerationZ() {
        return acceleration[2];
    }

    /*
     * 画面の回転を取得するクラス。
     *
     */
    private static final class DisplayRotateGetter {
        private static IDisplayRotateGetter getInstance() {
            // 2.2からのAPIを使っているのでOSのバージョンによって処理を分ける
            if (Build.VERSION.SDK_INT >= 8) {
                // for 2.2 or higher
                return new DisplayRotateGetterV8();
            } else {
                // for 2.1 or lower
                return new DisplayRotateGetterV1();
            }
        }

        private interface IDisplayRotateGetter {
            int getRotate(Display d);
        }

        private static final class DisplayRotateGetterV8 implements IDisplayRotateGetter {
            public int getRotate(Display d) {
                return d.getRotation();
            }
        }

        private static final class DisplayRotateGetterV1 implements IDisplayRotateGetter {
            public int getRotate(Display d) {
                int r = d.getOrientation();
                return (r == 0 ? Surface.ROTATION_0 : Surface.ROTATION_90);
            }
        }
    }

    /*
     * イベントリスナー
     */
    private final class MySensorListener implements SensorEventListener {
        @Override
        public final void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override
        public final void onSensorChanged(SensorEvent e) {
            switch (e.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accelerometerValues = e.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomagneticMatrix = e.values.clone();
                    sensorReady = true;
                    break;
            }

            if (geomagneticMatrix != null && accelerometerValues != null && sensorReady) {
                sensorReady = false;

                float[] R = new float[16];
                float[] I = new float[16];

                SensorManager.getRotationMatrix(R, I, accelerometerValues, geomagneticMatrix);
                //  画面の回転状態を取得する
                int dr = getDispRotation(activity);
                float x = 0;
                float y = 0;
                float z = 0;
                if (dr == Surface.ROTATION_0) {
                    // 回転無し
                    // アンドロイド版はiPhoneと逆になるようなので - を設定
                    x = -accelerometerValues[0] / SensorManager.GRAVITY_EARTH;
                    y = -accelerometerValues[1] / SensorManager.GRAVITY_EARTH;
                    z = -accelerometerValues[2] / SensorManager.GRAVITY_EARTH;
                } else if (dr == Surface.ROTATION_90) {
                    x = accelerometerValues[1] / SensorManager.GRAVITY_EARTH;
                    y = -accelerometerValues[0] / SensorManager.GRAVITY_EARTH;
                    z = -accelerometerValues[2] / SensorManager.GRAVITY_EARTH;
                } else if (dr == Surface.ROTATION_180) {
                    x = accelerometerValues[0] / SensorManager.GRAVITY_EARTH;
                    y = accelerometerValues[1] / SensorManager.GRAVITY_EARTH;
                    z = -accelerometerValues[2] / SensorManager.GRAVITY_EARTH;
                } else if (dr == Surface.ROTATION_270) {
                    // タブレットで回転軸がずれているときは、正面から見てピッチ、ヨーを逆転
                    x = -accelerometerValues[1] / SensorManager.GRAVITY_EARTH;
                    y = accelerometerValues[0] / SensorManager.GRAVITY_EARTH;
                    z = -accelerometerValues[2] / SensorManager.GRAVITY_EARTH;
                }

                //  更新
                setCurAcceleration(x, y, z);
            }
        }
    }
}