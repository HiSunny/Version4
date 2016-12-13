package utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.util.Log;
import android.widget.TextView;

import sunny.example.indoorlocation.SettingActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by on 2016/3/22.
 */
public class ObtainOffLineLearnStepData implements SensorEventListener {

    //alpha低通滤波系数
    public float accThreshold = 0.65f, k_wein = 45f, alpha = 0.25f;
    //mScale地图比例系数
    private static float mScale;
    private static int mOffsetDegree;
    Context context;
    TextView tvstepCount, tvstepLength, tvdegree, tvcoordinate;
    float[] mAccValues = new float[3];
    float[] mMagValues = new float[3];
    float[] mValues = new float[3];
    float[] R = new float[9];
    float[] I = new float[9];
    //mAcc合加速度
    float mAcc = 0, mMaResult = 0;
    float mMaxVal = 0f, mMinVal = 0f, mStepLength = 0f;
    int maLength = 5, stepState = 0, stepCount = 0;
    int degreeDisplay;
    DecimalFormat decimalF = new DecimalFormat("#.00");//保留小数点后两位
    float offset, degree;
    float mDistanceOnMap;
    /**
     * 传感器*/
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    /**
     * Acc和Mag读数时间（毫秒）*/
    private long mLastTimeAcc;
    private long mCurTimeAcc;
    private long mLastTimeMag;
    private long mCurTimeMag;

    //存储步长
    private ArrayList<Float> mStepLengthList = new ArrayList<>();
    //存储
    private ArrayList<Integer> mDegreeList = new ArrayList<>();
    /*获取步数，步长*/
    public ObtainOffLineLearnStepData(Context context, TextView stepCount, TextView stepLength, TextView mMMapStepLength, TextView mDegree, float[] coordPoint1, float[] coordPoint2) {
        this.context = context;
        this.tvstepCount = stepCount;
        this.tvstepLength = stepLength;
        this.tvdegree = mDegree;
        this.tvcoordinate = mMMapStepLength;
        mDistanceOnMap = 40.0f;//(float) Math.sqrt(Math.pow(coordPoint1[0] - coordPoint2[0], 2) + Math.pow(coordPoint1[1] - coordPoint2[1], 2));

        //设置步频阈值和Winberg系数
        initData();
        //获取加速度传感器，地磁传感器实例
        loadSystemService();
    }

    /*SettingActivity中可以更改步频阈值和Winberg系数*/
    private void initData() {
        accThreshold = SettingActivity.mFrequencyThreshold;
        k_wein = SettingActivity.k_wein;
    }

    private void loadSystemService() {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }
    //比例系数
    public static float getStepScale() {
        return mScale;
    }

    public static int getOffsetDegree() {
        return mOffsetDegree;
    }




    public void obtainStep() {
        mStepLengthList.clear();
        mDegreeList.clear();
        mLastTimeAcc = System.currentTimeMillis();
        mLastTimeMag = System.currentTimeMillis();
        /*boolean android.hardware.SensorManager.registerListener(SensorEventListener listener,
         * Sensor sensor, int rateUs)*/
        //SENSOR_DELAY_GAME 30hz——45Hz之间 一般38hz一秒钟38次SENSOR_DELAY_GAME
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //////////、
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mCurTimeAcc = System.currentTimeMillis();
            //满足时间阈值40ms获取一次步长，步数 并把步长存储到List中 传感器太灵敏改为80try100try200
            if (mCurTimeAcc - mLastTimeAcc > 100) {
                getStepAccCountLengthInfo(event.values.clone());//event.values.clone()获取加速度的值 float[]
                mLastTimeAcc = mCurTimeAcc;
            }
        }
        //////////       ////磁场传感器
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mCurTimeMag = System.currentTimeMillis();
            //40改为80改为100try200
            if (mCurTimeMag - mLastTimeMag > 100) {
                getAzimuthDegree(event.values.clone());//获取方位角float[]
                mLastTimeMag = mCurTimeMag;
            }
        }
    }//stepCount mStepLength 获取步数和步长
    private void getStepAccCountLengthInfo(float[] acc) {
        mAccValues = acc;
        //求合加速度
        mAcc = (float) (Math.sqrt(Math.pow(mAccValues[0], 2) + Math.pow(mAccValues[1], 2) + Math.pow(mAccValues[2], 2)) - 9.794);
        // public static float movingAverage(float acc, int length)
        //length个加速度的平均值（length：窗长度）
        //accThreshold加速度的阈值只是为判断是否开始走动
        //合加速度的平均值
        mMaResult = MovingAverage.movingAverage(mAcc, maLength);
        //stepState == 0尚未运动或者恰好走完一步准备走下一步这里表示尚未运动
        if (stepState == 0 && mMaResult > accThreshold) {
            stepState = 1;
        }
        //stepstate为1时，表示开始行走采集用户行走该步过程中的加速度的最大值
        //在该步过程中不断更新加速度最大值
        if (stepState == 1 && mMaResult > mMaxVal) { //find peak
            mMaxVal = mMaResult;
        }
        //stepstate为1时，表示已经采集到用户行走该步过程中的加速度的最大值
        if (stepState == 1 && mMaResult <= 0) {
            stepState = 2;
        }
        //行走该步的过程中的加速度的最小值//在该步过程中不断更新加速度最小值
        if (stepState == 2 && mMaResult < mMinVal) { //find bottom
            mMinVal = mMaResult;
        }
        //stepState == 2 合加速度大于〇 计步完成  并由Winberg计算步长
        if (stepState == 2 && mMaResult >= 0) {
            stepCount++;
            mStepLength = (float) (k_wein * Math.pow(mMaxVal - mMinVal, 1.0 / 4));
            mStepLengthList.add(mStepLength);
            mMaxVal = mMinVal = stepState = 0;
        }

        //TextView显示步长和步数
        stepViewShow();
    }

    public void stepViewShow() {
        tvstepCount.setText("Step Count : " + stepCount);
        tvstepLength.setText("Step Length : " + decimalF.format(mStepLength) + " cm");
    }

    private void getAzimuthDegree(float[] MagClone) {
		/*
		 * get the azimuth degree of the pedestrian.
		 */
        mMagValues = lowPassFilter(MagClone, mMagValues);
        if (mAccValues == null || mMagValues == null) return;
        boolean sucess = SensorManager.getRotationMatrix(R, I, mAccValues, mMagValues);
        if (sucess) {

            //float[] android.hardware.SensorManager.getOrientation(float[] R, float[] values)
            //根据旋转矩阵mValues计算方位角
        	/*values[0]: azimuth, rotation around the Z axis.
			values[1]: pitch, rotation around the X axis.
			values[2]: roll, rotation around the Y axis. */
            SensorManager.getOrientation(R, mValues);
            //将弧度转化为角度double java.lang.Math.toDegrees(double angrad)
            // Log.i("sunny", "degree1 = " + Math.toDegrees(mValues[0]));
            //只处理values[0]偏航角
            degree = (int) (Math.toDegrees(mValues[0]) + 360) % 360; // translate into (0, 360).
            // Log.i("sunny", "degree2 = " + degree);
            degree = ((int) (degree + 2)) / 5 * 5; // the value of degree is multiples of 5.
            //  Log.i("sunny", "degree3 = " + degree);
            // Log.i("sunny", "offset = " + offset);

            degreeDisplay = (int) degree;
            //获取偏航角存储在List中
            mDegreeList.add(degreeDisplay);
        }
    }

    //低通滤波器
    protected float[] lowPassFilter(float[] input, float[] output) {
		/*
		 * low pass filter algorithm implement.
		 */
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    //停止行走 计算平均步长，在室内地图上的平均步长
    public void stopStep() {
		/*
		 * stop listening for sensor and recording step information.
		 */
        mSensorManager.unregisterListener(this);
        //计算实际平均步长
        float sum = 0;
        for (float item : mStepLengthList) {
            sum += item;
        }
        float averageLength = sum / stepCount;
        float averageMapLength = mDistanceOnMap / stepCount;
        //比例系数 地图上的平均步长/实际平均步长
        mScale = averageMapLength / averageLength;
        //显示平均步长
        tvstepLength.setText("avg Length : " + decimalF.format(averageLength) + " cm");
        // Log.i("steplength", "sum = " + sum + ", averageLength =" + averageLength + ", disOnMap = " + mDistanceOnMap + ", averageMapLength = " + averageMapLength + ", mScale = " +mScale);

        for (int item : mDegreeList) {
            sum += item;
        }
        mOffsetDegree = (int) (sum / mDegreeList.size());

    }

}