package sunny.example.indoorlocation;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;


public class BaseActivity extends ActionBarActivity{

    //返回键
   // final long backSpaceTimeIntervalMilliSecond = 1000;
    //long lastTimeclickBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //
        /*Window android.app.Activity.getWindow()
Retrieve the current android.view.Window for the activity.
*///returns the current Window
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
