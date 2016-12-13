package utils;
//步数、步长、方位角
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import sunny.example.indoorlocation.SettingActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by on 2016/3/22.
 */
public class ObtainStepData implements SensorEventListener {

    private float accThreshold = 0.65f, k_wein = 45f, alpha = 0.25f;
    //      public static  float[] initPoint = {550.0f, 1150.0f};
    float[] initPoint1 = {330.0f, 120.0f};//{550.0f, 1150.0f}{56,700};////走廊需要改变的初始点坐标
    float[] initPoint0 = {95.0f, 500.0f};//{56,700};////
    public static float[] curCoordsOfStep = {330.0f, 120.0f};//需要改变的初始点坐标
    static ArrayList<CoordPoint> points = new ArrayList<>();
    private final float mStepScale;
    Context context;
    TextView tvstepCount, tvstepLength, tvdegree, tvcoordinate;
    float[] mAccValues = new float[3];
    float[] mMagValues = new float[3];
    float[] mValues = new float[3];
    float[] R = new float[9];
    float[] I = new float[9];
    float mAcc = 0, mMaResult = 0;
    float mMaxVal = 0f, mMinVal = 0f, mStepLength = 0f;
    private float mStepLength1 =0f;
    int maLength = 5, stepState = 0, stepCount = 0;
    int degreeDisplay;
    DecimalFormat decimalF = new DecimalFormat("#.00");
    float offset, degree;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private long mLastTimeAcc;
    private long mLastTimeMag;
    private long mCurTimeAcc;
    private long mCurTimeMag;
    private float mDistance = 0f;
    //    private final int mOffsetDegree;


    public ObtainStepData(Context context, TextView stepCount, TextView stepLength, TextView degree, TextView coordinate) {
        this.context = context;
        this.tvstepCount = stepCount;
        this.tvstepLength = stepLength;
        this.tvdegree = degree;
        this.tvcoordinate = coordinate;
        initData();
        mStepScale = ObtainOffLineLearnStepData.getStepScale();
        Log.i("steplength", "stepScale = " + mStepScale);
        offset =ObtainOffLineLearnStepData.getOffsetDegree();
        loadSystemService();
    }

    private void initData() {
        accThreshold = SettingActivity.mFrequencyThreshold;
        k_wein = SettingActivity.k_wein;
    }
    //获取移动一步后的坐标
    public static  float[] getCurCoordsOfStep() {

        // Log.i("ObtainStepData","getCurCoordsOfStep " + curCoordsOfStep );
		/*
		 * get the current coordinate of pedestrian step.
		 */
        return curCoordsOfStep;
    }

    public static  ArrayList<CoordPoint> getPoints() {
		/*
		 * get coordinate point.
		 */
        return points;
    }

    private void loadSystemService() {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


    }

    public void obtainStep() {
        mLastTimeAcc = System.currentTimeMillis();
        mLastTimeMag = System.currentTimeMillis();
 /*Registers a SensorEventListener for the given sensor.
Note: Don't use this method with a one shot trigger sensor such as Sensor.TYPE_SIGNIFICANT_MOTION.
Use requestTriggerSensor(TriggerEventListener, Sensor) instead.
Parameters:
listener A SensorEventListener object.
sensor The Sensor to register to.
rateUs The rate sensor events are delivered at. This is only a hint to the system.*/
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    //public abstract void onSensorChanged (SensorEvent event)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mCurTimeAcc = System.currentTimeMillis();
            //开始测量到SensorChanged的时间40改为100try
            if (mCurTimeAcc - mLastTimeAcc > 100) {
                getStepAccInfo(event.values.clone());
                mLastTimeAcc = mCurTimeAcc;
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mCurTimeMag = System.currentTimeMillis();
            //40改为100try
            if (mCurTimeMag - mLastTimeMag > 100) {
                getAzimuthDegree(event.values.clone());
                mLastTimeMag = mCurTimeMag;
            }
        }
    }
    //////、步数        获取加速度传感器的数据 由加速度和stepState-->步数
    private void getStepAccInfo(float[] clone) {
        mAccValues = clone;
        //加速度 公式（x^2 + y^2 + z^2）^0.5 - g
        mAcc = (float) (Math.sqrt(Math.pow(mAccValues[0], 2) + Math.pow(mAccValues[1], 2) + Math.pow(mAccValues[2], 2)) - 9.794);
        // public static float movingAverage(float accModule, int length)
        //求maLength个加速度的平均值 maLength = 5这里求5个数的平均值
        mMaResult = MovingAverage.movingAverage(mAcc, maLength);
        //accThreshold = 0.65f 当加速度阈值>0.65判断人开始行走
        if (stepState == 0 && mMaResult > accThreshold) {
            stepState = 1;
        }
        //加速度峰值
        if (stepState == 1 && mMaResult > mMaxVal) { //find peak
            mMaxVal = mMaResult;
        }
        if (stepState == 1 && mMaResult <= 0) {
            stepState = 2;
        }
        //加速度谷值
        if (stepState == 2 && mMaResult < mMinVal) { //find bottom
            mMinVal = mMaResult;
        }
        if (stepState == 2 && mMaResult >= 0) {
            stepCount++;
            getStepLengthAndCoordinate();
            //画轨迹
            recordTrajectory(curCoordsOfStep.clone());
            mMaxVal = mMinVal = stepState = 0;
        }
        //显示步长、步数、角度
        stepViewShow();
    }
    //显示步长、步数、角度
    private void stepViewShow() {
        Log.i("steplength", "mStepLength = " + mStepLength);
        tvstepCount.setText("Step Count : " + stepCount);
        tvstepLength.setText("Step Length : " + decimalF.format(mStepLength) + " cm");
        tvdegree.setText("Distance : " + decimalF.format(mDistance) + " cm");
    }

    //记录坐标点static ArrayList<CoordPoint> points = new ArrayList<>();
    private void recordTrajectory(float[] point) {
        points.add(new CoordPoint(point[0], point[1]));
    }
    //初始点坐标
    public void initPoints() {
        /*
		 * add the initial position coordinate of pedestrian.
		 */
        if(SettingActivity.sMapId == 0) {
            //记录初始点坐标 实验室
            points.add(new CoordPoint(initPoint0[0], initPoint0[1]));
        } else {//走廊
            points.add(new CoordPoint(initPoint1[0], initPoint1[1]));
        }
    }
    /////////获取移动一步后的坐标值curCoordsOfStep[]
    private void getStepLengthAndCoordinate() {
        Log.i("steplength", "mStepScale = " + mStepScale);
        //步长计算 co_k_wein = 45,fWinberg步长模型系数K K*（加速度max - 加速度min）^0.25
        mStepLength = (float) (k_wein * Math.pow(mMaxVal - mMinVal, 1.0 / 4));
        //行人行走距离
        mDistance += mStepLength;
        mStepLength1 = mStepLength* mStepScale;
        //double java.lang.Math.toRadians(double angdeg)转化为弧度值
        ////////////////、计算移动的距离x、y////////、、、、、、、、、、、、、////
        double delta_x = Math.cos(Math.toRadians(degreeDisplay)) * mStepLength1;
        double delta_y = Math.sin(Math.toRadians(degreeDisplay)) * mStepLength1;

        // public static float[] curCoordsOfStep = {190.0f, 1040.0f};
        ////////////////////坐标的获取，计算移动后的坐标//////////////////////////////
        curCoordsOfStep[0] += delta_x;
        curCoordsOfStep[1] += delta_y;
    }

    //////、    //获取方位角
    private void getAzimuthDegree(float[] MagClone) {
		/*
		 * get the azimuth degree of the pedestrian.
		 */
        //float[] mMagValues = new float[3];
        // protected float[] lowPassFilter(float[] input, float[] output)
        //方位角经低通滤波器处理
        mMagValues = lowPassFilter(MagClone, mMagValues);
        if ( mAccValues == null || mMagValues == null) return;
 /*public static boolean getRotationMatrix (float[] R, float[] I, float[] gravity, float[] geomagnetic)

Computes the inclination matrix I as well as the rotation matrix R transforming a vector from the device coordinate system to the world's coordinate system which is defined as a direct orthonormal basis,
*/
        boolean sucess = SensorManager.getRotationMatrix(R, I, mAccValues, mMagValues);
        if (sucess) {
   /*float[] android.hardware.SensorManager.getOrientation(float[] R, float[] values)
Computes the device's orientation based on the rotation matrix.
When it returns, the array values is filled with the result:

values[0]: azimuth, rotation around the Z axis. //是我们需要的
values[1]: pitch, rotation around the X axis.
values[2]: roll, rotation around the Y axis. */
            SensorManager.getOrientation(R, mValues);
            //mValues[0]就是我们需要的转向角
            degree = (int) (Math.toDegrees(mValues[0]) + 360) % 360; // translate into (0, 360).
            degree = ((int) (degree + 2)) / 5 * 5; //？？？ the value of degree is multiples of 5.
            if (offset == 0) {
                degreeDisplay = (int) degree;
            } else {
                degreeDisplay = roomDirection(degree, offset); // user-defined room direction.
            }
//            degreeDisplay = (int)(degree - offset);
//            stepDegreeViewShow();
        }
    }

    private int roomDirection(float myDegree, float myOffset) {
		/*
		 * define room direction as 270 degree.
		 */
        int tmp = (int) (myDegree - myOffset);
        if (tmp < 0) tmp += 360;
        else if (tmp >= 360) tmp -= 360;
        Log.i("degree", "myDegree : " + myDegree + " , myOffset : " + myOffset);
        return tmp;
    }

    //低通滤波器的算法实现
    protected float[] lowPassFilter(float[] input, float[] output) {
		/*
		 * low pass filter algorithm implement.
		 */
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
//        	/alpha = 0.25f 将后面的新计算的值赋给out[i]
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
    //取消注册监听
    public void stopStep() {
		/*
		 * stop listening for sensor and recording step information.
		 */
        mSensorManager.unregisterListener(this);
    }

    //reset初始坐标，步数、步长、距离
    public void correctStep() {
		/*
		 * initialize and correct the step parameters.
		 */
        offset = degree - 270;//360;//180;//270// -90;//93;//85;//
        if (SettingActivity.sMapId == 0) {
            curCoordsOfStep[0] =95.0f;//56;//95;
            curCoordsOfStep[1] = 500.0f;//700;//1181;
        } else {
            curCoordsOfStep[0] = 330.0f;//56;//95;
            curCoordsOfStep[1] = 120.0f;//700;//1181;
        }

        stepCount = 0;
        mStepLength = 0;
        mDistance = 0;
    }

    public void obtainStepSetting() {
		/*
		 * before start listening sensor of Accelerometer and Magnetometer, we set and obtain some parameters.
		 */
        tvstepCount.setVisibility(View.VISIBLE);
        tvstepLength.setVisibility(View.VISIBLE);
        tvdegree.setVisibility(View.VISIBLE);
        tvcoordinate.setVisibility(View.VISIBLE);
    }
    //
    public void stepViewGone() {
		/*
		 * set the view to gone.
		 */
        tvstepCount.setVisibility(View.INVISIBLE);
        tvstepLength.setVisibility(View.INVISIBLE);
        tvdegree.setVisibility(View.INVISIBLE);
        tvcoordinate.setVisibility(View.INVISIBLE);

    }

    public void clearPoints() {
		/*
		 * clear coordinate point.
		 */
        points.clear();
    }
}
