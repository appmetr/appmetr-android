package com.appmetr.android.demo.tabs;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.appmetr.android.AppMetr;
import com.appmetr.android.demo.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.zip.DataFormatException;

public class CasesTabActivity extends AbstractTabActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cases);

        initializeListeners();
    }

    private void initializeListeners() {
        Button buttonTrackStart = (Button) findViewById(R.id.buttonTrackStart);
        buttonTrackStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                EditText editExperimentName = (EditText) findViewById(R.id.editExperimentName);
                EditText editGroupName = (EditText) findViewById(R.id.editGroupName);

                String experimentName = editExperimentName.getText().toString();
                String groupName = editGroupName.getText().toString();

                if (experimentName.length() > 0 && groupName.length() > 0) {
                    AppMetr.trackExperimentStart(experimentName, groupName);
                    logMessage(String.format("Track experiment start \"%1$s\" group \"%2$s\"", experimentName, groupName));
                }
            }
        });

        Button buttonTrackEnd = (Button) findViewById(R.id.buttonTrackEnd);
        buttonTrackEnd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                EditText editExperimentName = (EditText) findViewById(R.id.editExperimentName);

                String experimentName = editExperimentName.getText().toString();

                if (experimentName.length() > 0) {
                    AppMetr.trackExperimentEnd(experimentName);
                    logMessage(String.format("Track experiment end \"%1$s\"", experimentName));
                }
            }
        });

        Button buttonTrackPayment = (Button) findViewById(R.id.buttonTrackPayment);
        buttonTrackPayment.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                EditText editPaymentUSD = (EditText) findViewById(R.id.editPaymentUSD);
                EditText editPaymentCountry = (EditText) findViewById(R.id.editPaymentCountry);
                CheckBox checkPaymentIsSandbox = (CheckBox) findViewById(R.id.checkPaymentIsSandbox);

                Double usdAmount = Double.valueOf(editPaymentUSD.getText().toString());
                String country = editPaymentCountry.getText().toString();
                Boolean isSandbox = checkPaymentIsSandbox.isChecked();

                JSONObject payment = new JSONObject();
                try {
                    payment.put("psUserSpentCurrencyCode", "USD");
                    payment.put("psUserSpentCurrencyAmount", usdAmount);
                    payment.put("orderId", "test.payment");
                    payment.put("processor", "google_checkout");
                    payment.put("transactionId", UUID.randomUUID().toString());
                    payment.put("appCurrencyCode", "coins");
                    payment.put("appCurrencyAmount", usdAmount * 5);

                    if (country != null && country.length() > 0) {
                        payment.put("psUserStoreCountryCode", country);
                    }

                    if (isSandbox) {
                        payment.put("isSandbox", isSandbox);
                    }

                    AppMetr.trackPayment(payment);
                    logMessage("Payment tracked");
                } catch (JSONException e) {
                    logMessage(e.getMessage());
                } catch (DataFormatException e) {
                    logMessage(e.getMessage());
                }

            }
        });

        Button buttonIdentify = (Button) findViewById(R.id.buttonIdentify);
        buttonIdentify.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                EditText editIdentifyUserId = (EditText) findViewById(R.id.editIdentifyUserId);

                String userId = editIdentifyUserId.getText().toString();

                if (userId.length() > 0) {
                    AppMetr.identify(userId);
                    logMessage(String.format("Identify for user id \"%1$s\"", userId));
                }
            }
        });
    }
}