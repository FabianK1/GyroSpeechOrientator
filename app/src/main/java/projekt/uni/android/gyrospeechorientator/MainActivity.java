package projekt.uni.android.gyrospeechorientator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int REQ_CODE_SPEECH_INPUT = 100;

    private SensorManager sensor                = null;
    private int xDevice,yDevice                 = 0;
    private boolean switchBallPosition          = false;
    private String orientationVoiceCommand      = "";
    private ImageView ballLeft,ballRight,ballTop,ballBottom;
    private List<View> relatedViews             = new ArrayList<>();
    private Map<String,View> relatedViews2      = new HashMap<String, View>();

    private VoiceLookup voiceOrientationLookup  = new VoiceLookup();
    private Side deviceOrientationLookup        = new Side();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        setBallViews();


    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                xDevice = (int) values[0];// this part of code is only test to see int x and y on Activity
                yDevice = (int) values[1];
                if(switchBallPosition){
                    updateUIElements(orientationVoiceCommand);
                }

            }
        }
    }

    // needed to be implemented because of interface
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    private void setBallViews(){
        ballLeft = (ImageView) findViewById(R.id.ballLeft);
        ballTop = (ImageView) findViewById(R.id.ballTop);
        ballRight = (ImageView) findViewById(R.id.ballRight);
        ballBottom = (ImageView) findViewById(R.id.ballBottom);

        relatedViews.add(ballLeft);
        relatedViews.add(ballTop);
        relatedViews.add(ballRight);
        relatedViews.add(ballBottom);
        relatedViews2.put("ballLeft",ballLeft);
        relatedViews2.put("ballRight",ballRight);
        relatedViews2.put("ballTop",ballTop);
        relatedViews2.put("ballBottom",ballBottom);
    }

    private void changeVisibility(List<View> views,int visibility){
        for (View view : views){
            view.setVisibility(visibility);
        }

    }

    public void ballOrientation(String orientation ){

        View ball;
        changeVisibility(relatedViews,View.INVISIBLE);
        ball = relatedViews2.get(orientation);
        ball.setVisibility(View.VISIBLE);

    }

    public void testText(View v){

        if (SpeechRecognizer.isRecognitionAvailable(this)) promptSpeechInput();
        else Toast.makeText(this,"irgedwas geht nisch",Toast.LENGTH_SHORT).show();
        showToastMessage("blub");

    }
    public void promptSpeechInput(){
        //1. create new intent which shows mic dialog box to recognize speech input
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // Simply takes user’s speech input and returns it to same activity

        //2. add some extra parameters
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); // Considers input in free form
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); // use default language
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"You may give a command for orientation now"); // text hint for the user to speak
        try {
            //3. start the speech recognizer intent using startActivityForResult()
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException e) {
            showToastMessage("test");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the request code is equal to ours
        if(requestCode == REQ_CODE_SPEECH_INPUT) {

            // If Voice recognition is successful then it returns RESULT_OK
            if (resultCode == RESULT_OK) {

                if (data != null) {
                    // 5. Get result list
                    ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    // 6. Update UI elements with our result list
                    //updateUIElements(results.get(0));

                    // 7. Analyze result string
                    analyzeSpeechInput(results.get(0));
                }

                //Result code for various error.
            } else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
                showToastMessage("Audio Error");
            } else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
                showToastMessage("Client Error");
            } else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
                showToastMessage("Network Error");
            } else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
                showToastMessage("No Match");
            } else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
                showToastMessage("Server Error");
            }
        }
    }

    public void analyzeSpeechInput(String result){
        ArrayList<String> orientation = new ArrayList();
        String orientationCommand = "";
        orientation.add("oben");
        orientation.add("unten");
        orientation.add("links");
        orientation.add("rechts");

        for (String direction : orientation){
            if(result.contains(direction))
            {
                orientationCommand = direction;
                break;
            }
        }
        if(orientationCommand == ""){
            showToastMessage("There was no discernible direction input, please try again");
        }
        else if(orientationVoiceCommand!=orientationCommand){
            //then call ballUI changer
            orientationVoiceCommand = orientationCommand;
            showToastMessage("Success! Now you will be shot to the moon, buckle up.");
            switchBallPosition = true;
            //updateUIElements(orientationVoiceCommand);
        }



    }

    public void updateUIElements(String voiceCommand ){

        Map<String,String> deviceOrientation = deviceOrientationLookup.getOrientation();
        String newBallView;
        deviceOrientationLookup.setAngles(xDevice,yDevice);
        newBallView = voiceOrientationLookup.getVoiceOrientation(voiceCommand,deviceOrientation.get("angle"),deviceOrientation.get("sign"));
        ballOrientation(newBallView);

    }

    public void showToastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    class Side {

        private String angle;
        private String sign;
        private Map<String,Integer> inputAngles = new HashMap<String, Integer>();
        private Map<String,String> outputAngles = new HashMap<String, String>();

        public void setAngles(int x, int y){

            inputAngles.put("x",x);
            inputAngles.put("y",y);

            angle   = Math.abs(inputAngles.get("x")) > Math.abs(inputAngles.get("y")) ? "x" : "y";
            sign    = inputAngles.get(angle) < 0 ? "-" : "+";
            outputAngles.put("angle",angle);
            outputAngles.put("sign",sign);

        }
        public Map<String,String> getOrientation(){
            return this.outputAngles;
        }

    }

    class VoiceLookup{
        private Map<String,HashMap<String,HashMap<String,String>>> commandMap = new HashMap<String,HashMap<String,HashMap<String,String>>>();

        VoiceLookup(){

            commandMap.put("oben",new HashMap<String, HashMap<String, String>>());
            commandMap.get("oben").put("y",new HashMap<String, String>());
            commandMap.get("oben").get("y").put("+","ballTop");
            commandMap.get("oben").get("y").put("-","ballBottom");
            commandMap.get("oben").put("x",new HashMap<String, String>());
            commandMap.get("oben").get("x").put("+","ballRight");
            commandMap.get("oben").get("x").put("-","ballLeft");

            commandMap.put("unten",new HashMap<String, HashMap<String, String>>());
            commandMap.get("unten").put("y",new HashMap<String, String>());
            commandMap.get("unten").get("y").put("+","ballBottom");
            commandMap.get("unten").get("y").put("-","ballTop");
            commandMap.get("unten").put("x",new HashMap<String, String>());
            commandMap.get("unten").get("x").put("+","ballLeft");
            commandMap.get("unten").get("x").put("-","ballRight");

            commandMap.put("rechts",new HashMap<String, HashMap<String, String>>());
            commandMap.get("rechts").put("y",new HashMap<String, String>());
            commandMap.get("rechts").get("y").put("+","ballRight");
            commandMap.get("rechts").get("y").put("-","ballLeft");
            commandMap.get("rechts").put("x",new HashMap<String, String>());
            commandMap.get("rechts").get("x").put("+","ballTop");
            commandMap.get("rechts").get("x").put("-","ballBottom");

            commandMap.put("links",new HashMap<String, HashMap<String, String>>());
            commandMap.get("links").put("y",new HashMap<String, String>());
            commandMap.get("links").get("y").put("+","ballLeft");
            commandMap.get("links").get("y").put("-","ballRight");
            commandMap.get("links").put("x",new HashMap<String, String>());
            commandMap.get("links").get("x").put("+","ballBottom");
            commandMap.get("links").get("x").put("-","ballTop");
        }

        public String getVoiceOrientation(String voiceCommand,String angle, String sign){

            return this.commandMap.get(voiceCommand).get(angle).get(sign);

        }
    }

    protected void onResume() {
        super.onResume();
        Sensor Accel = sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // register this class as a listener for the orientation and accelerometer sensors
        sensor.registerListener((SensorEventListener) this, Accel,SensorManager.SENSOR_DELAY_FASTEST);
    }
}
