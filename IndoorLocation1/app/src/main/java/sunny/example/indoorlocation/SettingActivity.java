package sunny.example.indoorlocation;

import android.annotation.SuppressLint;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import utils.ObtainStepData;

public class SettingActivity extends BaseActivity {

    //sStepThreshold  mFrequencyThreshold步频阈值
    public static float mFrequencyThreshold = 0.65f;
    //winberg模型系数k
    public static float k_wein = 45f;
    public static int sMapId = 0;//实验室

    private EditText mStepThresholdEditText;
    private EditText mCo_k_weinEditText;
    private RadioGroup mRadioGroup;

    private Button mOffLineLearnButton;
    private Button mShowPositionButton;
    private Button mSettingsButton;
    private BottomButtonClickListener mBottomButtonClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initBottomButton();

        mStepThresholdEditText = (EditText) findViewById(R.id.ed_step_threshold);
        mCo_k_weinEditText = (EditText) findViewById(R.id.ed_Co_K_wein);
    /*void android.widget.TextView.addTextChangedListener(TextWatcher watcher)
Adds a TextWatcher to the list of those whose methods are called whenever this
 TextView's text changes.
*/
        mStepThresholdEditText.addTextChangedListener(new myTextWatcher(mStepThresholdEditText));
        mCo_k_weinEditText.addTextChangedListener(new myTextWatcher(mCo_k_weinEditText));

        mRadioGroup = (RadioGroup) findViewById(R.id.check_radio_group);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_button_1) {
                    sMapId = 0;//实验室
                } else {
                    sMapId = 1;//走廊
                }
            }
        });
    }
    /*android.text.TextWatcher
    When an object of a type is attached to an Editable, its methods will be called when
    the text is changed.
    */
    private class myTextWatcher implements TextWatcher {

        private View view;

        private myTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        /**
         * 可以更改步频和Winberg系数*/
        @SuppressLint("NewApi")
        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();
            if (!text.isEmpty()) {
                switch (view.getId()) {
                    case R.id.ed_Co_K_wein:
                        k_wein = Float.parseFloat(text);
                        break;
                    case R.id.ed_step_threshold:
                        mFrequencyThreshold = Float.parseFloat(text);
                        break;

                }
            }
        }
    }

    private void initBottomButton() {
        mOffLineLearnButton = (Button) findViewById(R.id.bt_offline_learn);
        mShowPositionButton = (Button) findViewById(R.id.bt_show_position);
        mSettingsButton = (Button) findViewById(R.id.bt_settings);

        resetBottom();
        mSettingsButton.setTextColor(getResources().getColor(R.color.my_green));

        mBottomButtonClickListener = new BottomButtonClickListener();
        mOffLineLearnButton.setOnClickListener(mBottomButtonClickListener);
        mShowPositionButton.setOnClickListener(mBottomButtonClickListener);
//        mSettingsButton.setOnClickListener(mBottomButtonClickListener);
    }

    private class BottomButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {


            switch (v.getId()) {

                case R.id.bt_offline_learn:
                    startActivity(new Intent(SettingActivity.this, OffLineLearnActivity.class));
                    finish();
                    break;
                case R.id.bt_show_position:
                    startActivity(new Intent(SettingActivity.this, ShowPositionActivity.class));
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
