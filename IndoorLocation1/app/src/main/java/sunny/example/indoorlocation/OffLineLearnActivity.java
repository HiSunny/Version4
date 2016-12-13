package sunny.example.indoorlocation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import utils.ObtainOffLineLearnStepData;

import android.view.ViewGroup;
import android.view.WindowManager;

public class OffLineLearnActivity extends BaseActivity{

    private Button mOffLineLearnButton;
    private Button mShowPositionButton;
    private Button mSettingsButton;
    private BottomButtonClickListener mBottomButtonClickListener;

    static int[] newCoords = new int[2];

    // Time variables40改为100try
    final long updateItemMilliTime = 100;
    TimerTask mTimetask;

    //实验室
    float[] coordPoint1 = {95.0f, 1181.0f};
    float[] coordPoint2 = {95.0f, 460.0f};
    //走廊
    float[] coordPoint3 = {250.0f, 500.0f};//{550.0f, 1181.0f};
    float[] coordPoint4 = {250.0f, 290.0f};//{550.0f, 460.0f};


    int[] point1;
    int[] point2;
    private ImageView mIndoorMap;
    private Bitmap mBackgroundMap;
    private Bitmap mMark;
    private Bitmap mResultBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Timer mTimer;
    private Button mStart;
    private Button mStop;
    private TextView mStepCount;
    private TextView mStepLength;
    private TextView mMMapStepLength;
    private TextView mDegree;
    private ObtainOffLineLearnStepData mObtainOffLineLearnStepData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offlinelearn);

        initBottomButton();

        mIndoorMap = (ImageView) findViewById(R.id.image_map);

        //使高度充满 day7 12.12
        WindowManager wm = this.getWindowManager();

        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        ViewGroup.LayoutParams lp = mIndoorMap.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mIndoorMap.setLayoutParams(lp);

        mIndoorMap.setMaxWidth(width);
        mIndoorMap.setMaxHeight(width * 5);
        mIndoorMap.setMaxWidth(height);
        mIndoorMap.setMaxHeight(height * 5);

        mStepCount = (TextView)findViewById(R.id.text_view_step_count);
        mStepLength = (TextView) findViewById(R.id.text_view_step_length);
        mMMapStepLength = (TextView)findViewById(R.id.text_view_step_coordinate);
        mDegree = (TextView) findViewById(R.id.text_view_step_degree);

        //sMapId ==0实验室
        if(SettingActivity.sMapId ==0) {
            // Log.i("MapId", "sMapId = " + SettingActivity.sMapId);
            mBackgroundMap = BitmapFactory.decodeResource(getResources(), R.drawable.lab);
            //public ObtainOffLineLearnStepData(Context context, TextView stepCount, TextView stepLength, TextView mMMapStepLength, TextView mDegree, float[] coordPoint1, float[] coordPoint2)
            mObtainOffLineLearnStepData = new ObtainOffLineLearnStepData(this, mStepCount, mStepLength, mMMapStepLength, mDegree,coordPoint1,coordPoint2);
            coordPoint1[0] = 95.0f;
            coordPoint1[1] = 500.0f;//起点
            coordPoint2[0] = 95.0f;
            coordPoint2[1] = 200.0f;////终点
        } else {//走廊
            Log.i("MapId", "sMapId = " + SettingActivity.sMapId);
            mBackgroundMap = BitmapFactory.decodeResource(getResources(), R.drawable.map);
            mObtainOffLineLearnStepData = new ObtainOffLineLearnStepData(this, mStepCount, mStepLength, mMMapStepLength, mDegree,coordPoint3,coordPoint4);
            coordPoint1[0] = 330.0f;//250.0f;
            coordPoint1[1] = 80.0f;//60.0f;//100.0f;//500.0f;//起点
            coordPoint2[0] = 330.0f;//250.0f;
            coordPoint2[1] = 120.0f;//290.0f;//终点
        }


        //开始测量
        mStart = (Button)findViewById(R.id.button_start);
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //obtainStep()获得系统时间，注册传感器
                mObtainOffLineLearnStepData.obtainStep();
            }
        });
        //停止
        mStop = (Button) findViewById(R.id.button_stop);
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mObtainOffLineLearnStepData.stopStep();
            }
        });

        mMark = BitmapFactory.decodeResource(getResources(), R.drawable.red_mark);
        mResultBitmap = Bitmap.createBitmap(mBackgroundMap.getWidth(), mBackgroundMap.getHeight(), mBackgroundMap.getConfig());

        mCanvas = new Canvas(mResultBitmap);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(7);

        mCanvas.drawBitmap(mBackgroundMap, new Matrix(), null);
        mIndoorMap.setImageBitmap(mResultBitmap);

        offLineLearnSchedule(updateItemMilliTime);
    }


    //这里只是把标记和指定的轨迹画到地图上,实际上并不需要重复执行
    private void offLineLearnSchedule(long updateItemMilliTime) {
        mTimer = new Timer();
        mTimetask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCanvas.drawBitmap(mBackgroundMap, new Matrix(), null);
                        point1 = convertTouchCoordinates(coordPoint1).clone();//clone!!!!!!!!!!!!!!!!!!!!
                        point2 = convertTouchCoordinates(coordPoint2).clone();
                        //void android.graphics.Canvas.drawBitmap(Bitmap bitmap, float left, float top, Paint paint)
                        mCanvas.drawBitmap(mMark, point2[0], point2[1], mPaint);////////////红点标记
                        mCanvas.drawLine(point1[0], point1[1], point2[0], point2[1], mPaint);
                    }
                });
            }
        };
        mTimer.schedule(mTimetask,0, updateItemMilliTime);
    }

    public int[] convertTouchCoordinates(float[] coors) {
        /*
         * float[] : convert coordinate to fit on the screen of mobile.
		 */
        newCoords[0] = (int) (coors[0] * ((float) mCanvas.getWidth() / mIndoorMap.getRight()));
        newCoords[1] = (int) (coors[1] * ((float) mCanvas.getHeight() / mIndoorMap.getBottom()));
        return newCoords;
    }

    private void initBottomButton() {
        mOffLineLearnButton = (Button) findViewById(R.id.bt_offline_learn);
        mShowPositionButton = (Button) findViewById(R.id.bt_show_position);
        mSettingsButton = (Button) findViewById(R.id.bt_settings);

        resetBottom();
        mOffLineLearnButton.setTextColor(getResources().getColor(R.color.my_green));

        mBottomButtonClickListener = new BottomButtonClickListener();
        mSettingsButton.setOnClickListener(mBottomButtonClickListener);
        mShowPositionButton.setOnClickListener(mBottomButtonClickListener);
//        mSettingsButton.setOnClickListener(mBottomButtonClickListener);
    }

    private class BottomButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {


            switch (v.getId()) {

                case R.id.bt_settings:
                    startActivity(new Intent(OffLineLearnActivity.this, SettingActivity.class));
                    finish();
                    break;
                case R.id.bt_show_position:
                    startActivity(new Intent(OffLineLearnActivity.this, ShowPositionActivity.class));
                    finish();
                    break;
            }
        }
    }


    private void resetBottom() {

        mShowPositionButton.setTextColor(getResources().getColor(android.R.color.black));
        mSettingsButton.setTextColor(getResources().getColor(android.R.color.black));
        mOffLineLearnButton.setTextColor(getResources().getColor(android.R.color.black));
    }
}
