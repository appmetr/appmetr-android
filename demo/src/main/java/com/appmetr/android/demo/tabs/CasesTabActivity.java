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
