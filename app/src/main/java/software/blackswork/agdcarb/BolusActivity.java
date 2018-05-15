package software.blackswork.agdcarb;


import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import software.blackswork.agdcarb.utils.KeyboardUtils;

/**
 * Created by Ezio Ladda  on 4/29/2018.
 */

public class BolusActivity extends AppCompatActivity {
    private EditText txtGlucose;
    private Spinner selEvent;
    private EditText txtCho;
    private EditText txtCalcCHO;
    private EditText txtCalcGluco;
    private EditText txtCalcTotal;
    private Button btnCalcBol;
    private TextView txtGlucRate;
    private TextView txtCHORate;
    public float CHORatio = 0;
    public float FSIRatio = 0;
    public float FSIthreshold = 0;
    public float FSItarget = 0;
    public float PenRound = 0;

    private TextView txtR1C1;
    private TextView txtR1C2;
    private TextView txtR1C3;
    private TextView txtR1C4;
    private TextView txtR2C1;
    private TextView txtR2C2;
    private TextView txtR2C3;
    private TextView txtR2C4;

    public  SharedPreferences prefs;
    private ArrayList<String> arEvents;

    public BolusActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolus);

        //setUp Variables associated to UI items
        arEvents = new ArrayList<String>();
        txtGlucose = (EditText) findViewById(R.id.txtGluc);
        selEvent = (Spinner) findViewById(R.id.selEvent);
        txtCho = (EditText) findViewById(R.id.txtCHO);

        btnCalcBol = (Button) findViewById(R.id.btnCalcBol);

        txtCalcCHO = (EditText) findViewById(R.id.txtCalcCHO);
        txtCalcGluco =(EditText) findViewById(R.id.txtCalcGluco);
        txtCalcTotal = (EditText) findViewById(R.id.txtCalcTotal);
        txtCHORate = (TextView) findViewById(R.id.txtCHORate);
        txtGlucRate = (TextView) findViewById(R.id.txtGlucRate);

        txtR1C1 = (TextView) findViewById(R.id.txtR1C1);
        txtR1C2 = (TextView) findViewById(R.id.txtR1C2);
        txtR1C3 = (TextView) findViewById(R.id.txtR1C3);
        txtR1C4 = (TextView) findViewById(R.id.txtR1C4);

        txtR2C1 = (TextView) findViewById(R.id.txtR2C1);
        txtR2C2 = (TextView) findViewById(R.id.txtR2C2);
        txtR2C3 = (TextView) findViewById(R.id.txtR2C3);
        txtR2C4 = (TextView) findViewById(R.id.txtR2C4);

        //The following lines read dynamiccally all the events stored in the shared preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        StringBuilder strBld;         int j=1;        int evt=0;
        String event = "";
        arEvents.add(getString(R.string.edtEvent));
        for (int i=0; i< prefs.getAll().size(); i++){
            strBld = new StringBuilder(); strBld.append("Event"); strBld.append(j); strBld.append("_Name");
            event = prefs.getString( strBld.toString(), "none");
            if (event.equals("none")) {
                //Do nothing if the event is set to none, default value
            } else {
               arEvents.add(event);
                evt++;
            }
            j++;
        }

        //Assign the Events to the spinner for select it
        ArrayAdapter<String> events = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, arEvents);
        // Specify the layout to use when the list of choices appears
        events.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        selEvent.setAdapter(events);

        selEvent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // First item will be gray
                if (position == 0) {
                    //Nothing to do; // ((TextView) view).setTextColor(ContextCompat.getColor(parent, R.color.login_input_hint_color));
                } else {
                    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getParent().getBaseContext());
                    String SelectedEv = selEvent.getItemAtPosition(position).toString();

                    StringBuilder strBld1 = new StringBuilder(); strBld1.append("Event"); strBld1.append(position); strBld1.append("_Name");
                    String SelEvent = prefs.getString(strBld1.toString(), "none");
                    if (SelEvent.equals(SelectedEv)){
                        strBld1 = new StringBuilder(); strBld1.append("Event"); strBld1.append(position); strBld1.append("_Rate");
                        CHORatio = Float.valueOf( prefs.getString(strBld1.toString(), "10.0"));
                    } else {
                        CHORatio = 0;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        btnCalcBol.requestFocus();
    }

   public void onClickClean(View item){
        if (item != null){
            TextView tv = (TextView) findViewById(item.getId());
            if (tv != null){
                tv.setText(null);
            }
        }
    }

    public void Calculate(View view) {
        float iGlucose = 0.0f;
        float iCarbo = 0.0f;
        float CHOBolus = 0.0f;
        float FSIBolus = 0.0f;
        float TotalBolus = 0.0f;

        try {
            iGlucose = Float.valueOf(txtGlucose.getText().toString());
            iCarbo = Float.valueOf(txtCho.getText().toString());

            FSIRatio =  Float.valueOf(prefs.getString("FSI_Rate", "0.0"));
            FSIthreshold = Float.valueOf(prefs.getString("FSI_Over", "0.0"));
            FSItarget = Float.valueOf(prefs.getString("FSI_Target", "100.0"));
            PenRound = Float.valueOf(prefs.getString("Round_Pen", "1.0"));


            if (iCarbo==0.0f || CHORatio==0.0f) {
                txtCalcCHO.setText("0.0");
                txtCHORate.setText("CHO Rate 0.0");
            } else {
                CHOBolus = iCarbo / CHORatio;
                txtCalcCHO.setText(String.format("%1$.3f", CHOBolus));
                txtCHORate.setText(String.format("CHO Rate %1$.2f [g/UI]", CHORatio));
            }

            if (FSIRatio==0.0f){
                txtCalcGluco.setText("0.0");
                txtGlucRate.setText("FSI Rate 0.0");
            } else if (iGlucose >= FSIthreshold){
                FSIBolus = (iGlucose - FSItarget) / FSIRatio;
                txtCalcGluco.setText(String.format("%1$.3f", FSIBolus));
                txtGlucRate.setText(String.format("FSI Rate %1$.2f [mg/Dl / UI]", FSIRatio));
            } else {
                txtCalcGluco.setText("0.0");
                txtGlucRate.setText("FSI Rate 0.0");
            }

            TotalBolus = CHOBolus + FSIBolus;
            if (TotalBolus == 0.0f) {
                txtCalcTotal.setText("0.0");
            } else {
                txtCalcTotal.setText(String.format("%1$.3f", TotalBolus));
            }


            /*Calculate estimation table for pens or Specific bolus Rounding*/
            if (PenRound!=0){
                //Define a base rounded number to use for write the table
                float base = 0;
                base = RoundNumber(TotalBolus,PenRound);

                //calculate the 4 steps to print
                float col1 = base - PenRound;
                float col2 = base;
                float col3 = base + PenRound;
                float col4 = base + (2* PenRound);
                txtR1C1.setText(String.format("%1$.2f", col1));
                txtR1C2.setText(String.format("%1$.2f", col2));
                txtR1C3.setText(String.format("%1$.2f", col3));
                txtR1C4.setText(String.format("%1$.2f", col4));

                //COL1 for each column the difference will be used for estimate the rounding according FSI
                float dif = Math.abs(TotalBolus - col1);

                if (TotalBolus > col1)
                    col1 = FSItarget + (dif*FSIRatio);
                else
                    col1 = FSItarget - (dif*FSIRatio);

                //COL2 for each column the difference will be used for estimate the rounding according FSI
                dif = Math.abs(TotalBolus - col2);

                if (TotalBolus > col2)
                    col2 = FSItarget + (dif*FSIRatio);
                else
                    col2 = FSItarget - (dif*FSIRatio);

                //COL3 for each column the difference will be used for estimate the rounding according FSI
                dif = Math.abs(TotalBolus - col3);

                if (TotalBolus > col3)
                    col3 = FSItarget + (dif*FSIRatio);
                else
                    col3 = FSItarget - (dif*FSIRatio);

                //COL4 for each column the difference will be used for estimate the rounding according FSI
                dif = Math.abs(TotalBolus - col4);

                if (TotalBolus > col4)
                    col4 = FSItarget + (dif*FSIRatio);
                else
                    col4 = FSItarget - (dif*FSIRatio);

                txtR2C1.setText(String.format("%1$.2f", col1));
                txtR2C2.setText(String.format("%1$.2f", col2));
                txtR2C3.setText(String.format("%1$.2f", col3));
                txtR2C4.setText(String.format("%1$.2f", col4));

                KeyboardUtils.hideKeyboard(this);
            }

        } catch (Error e) {

        }

    }


    private float RoundNumber(float n, float div) {
            BigDecimal origVal = new BigDecimal(n);
            BigDecimal rounder = new BigDecimal(div);
            BigDecimal retVal = origVal.divide(rounder, 9, RoundingMode.HALF_EVEN);
            retVal = retVal.setScale(0, RoundingMode.HALF_UP).multiply(rounder);
            return retVal.floatValue();
    }
}

