package com.singly.android.component;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.singly.android.client.SinglyClient;
import com.singly.android.sdk.R;

/**
 * An activity that prompts the user for their name, phone number, and email 
 * address.  The only reliable way to gather this type of information across 
 * phones and network providers is to ask for it.
 * 
 * The information input is then stored in shared preferences.
 */
public class DeviceOwnerActivity
  extends Activity {

  // android OK result code is -1, so we use -50 and less
  public static final int RESULT_DONT_SYNC = -50;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.singly_device_owner);

    final SharedPreferences prefs = getSharedPreferences("singly",
      Context.MODE_PRIVATE);
    String ownerName = prefs.getString(SinglyClient.OWNER_NAME, null);
    String ownerPhone = prefs.getString(SinglyClient.OWNER_PHONE_NUMBER, null);
    String ownerEmail = prefs.getString(SinglyClient.OWNER_EMAIL_ADDRESS, null);

    // populate the fields if they exist
    final EditText ownerNameEdit = (EditText)findViewById(R.id.ownerNameEdit);
    if (StringUtils.isNotBlank(ownerName)) {
      ownerNameEdit.setText(ownerName);
    }
    final EditText ownerPhoneEdit = (EditText)findViewById(R.id.ownerPhoneEdit);
    if (StringUtils.isNotBlank(ownerPhone)) {
      ownerPhoneEdit.setText(ownerPhone);
    }
    final EditText ownerEmailEdit = (EditText)findViewById(R.id.ownerEmailEdit);
    if (StringUtils.isNotBlank(ownerEmail)) {
      ownerEmailEdit.setText(ownerEmail);
    }

    // submit button saves inputs to shared preferences if they exists and then
    // closes the activity
    Button submitButton = (Button)findViewById(R.id.ownerButtonSubmit);
    submitButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        SharedPreferences.Editor editor = prefs.edit();

        String nameInput = ownerNameEdit.getText().toString();
        if (StringUtils.isNotBlank(nameInput)) {
          editor.putString(SinglyClient.OWNER_NAME, nameInput);
        }
        String phoneInput = ownerPhoneEdit.getText().toString();
        if (StringUtils.isNotBlank(phoneInput)) {
          editor.putString(SinglyClient.OWNER_PHONE_NUMBER, phoneInput);
        }
        String emailInput = ownerEmailEdit.getText().toString();
        if (StringUtils.isNotBlank(emailInput)) {
          editor.putString(SinglyClient.OWNER_EMAIL_ADDRESS, emailInput);
        }
        editor.commit();

        setResult(0);
        DeviceOwnerActivity.this.finish();
      }
    });

    // cancel button just closes activity
    Button cancelButton = (Button)findViewById(R.id.ownerButtonCancel);
    cancelButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        setResult(RESULT_DONT_SYNC);
        DeviceOwnerActivity.this.finish();
      }
    });

  }
}
