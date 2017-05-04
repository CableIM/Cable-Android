package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.util.Dialogs;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter;

/**
 * The register account activity.  Prompts ths user for their registration information
 * and begins the account registration process.
 *
 * @author Moxie Marlinspike
 *
 */
public class RegistrationActivity extends BaseActionBarActivity {

  private static final int PICK_COUNTRY = 1;
  private static final int PICK_SERVER = 2;
  private static final String TAG = RegistrationActivity.class.getSimpleName();

  private ArrayAdapter<String> serverSpinnerAdapter;
  private Spinner              serverSpinner;
  private TextView             serverCode;

  private AsYouTypeFormatter   countryFormatter;
  private ArrayAdapter<String> countrySpinnerAdapter;
  private Spinner              countrySpinner;
  private TextView             countryCode;
  private TextView             number;
  private TextView             createButton;
  private TextView             skipButton;
  private TextView             informationView;
  private View                 informationToggle;
  private TextView             informationToggleText;

  private MasterSecret masterSecret;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.registration_activity);

    getSupportActionBar().setTitle(getString(R.string.RegistrationActivity_connect_with_signal));

    initializeResources();
    initializeSpinner();
    initializeNumber();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == PICK_COUNTRY && resultCode == RESULT_OK && data != null) {
      this.countryCode.setText(data.getIntExtra("country_code", 1)+"");
      setCountryDisplay(data.getStringExtra("country_name"));
      setCountryFormatter(data.getIntExtra("country_code", 1));
    }

    if (requestCode == PICK_SERVER && resultCode == RESULT_OK && data != null) {
      this.serverCode.setText(data.getStringExtra("server_code"));
      setServerDisplay(data.getStringExtra("server_name"));
    }
  }

  private void initializeResources() {
    this.masterSecret   = getIntent().getParcelableExtra("master_secret");
    this.serverSpinner         = (Spinner)findViewById(R.id.server_spinner);
    this.serverCode            = (TextView)findViewById(R.id.server_code);
    this.countrySpinner        = (Spinner) findViewById(R.id.country_spinner);
    this.countryCode           = (TextView) findViewById(R.id.country_code);
    this.number                = (TextView) findViewById(R.id.number);
    this.createButton          = (TextView) findViewById(R.id.registerButton);
    this.skipButton            = (TextView) findViewById(R.id.skipButton);
    this.informationView       = (TextView) findViewById(R.id.registration_information);
    this.informationToggle     =            findViewById(R.id.information_link_container);
    this.informationToggleText = (TextView) findViewById(R.id.information_label);

    DrawableCompat.setTint(this.createButton.getBackground(), getResources().getColor(R.color.signal_primary));
    DrawableCompat.setTint(this.skipButton.getBackground(), getResources().getColor(R.color.grey_400));

    this.serverCode.addTextChangedListener(new ServerChangedListener());
    this.countryCode.addTextChangedListener(new CountryCodeChangedListener());
    this.number.addTextChangedListener(new NumberChangedListener());
    this.createButton.setOnClickListener(new CreateButtonListener());
    this.skipButton.setOnClickListener(new CancelButtonListener());
    this.informationToggle.setOnClickListener(new InformationToggleListener());

    if (getIntent().getBooleanExtra("cancel_button", false)) {
      this.skipButton.setVisibility(View.VISIBLE);
    } else {
      this.skipButton.setVisibility(View.INVISIBLE);
    }
  }

  private void initializeSpinner() {    //Server
    this.serverSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
    this.serverSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    setServerDisplay(getString(R.string.RegistrationActivity_select_your_server));

    this.serverSpinner.setAdapter(this.serverSpinnerAdapter);
    this.serverSpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          Intent intent = new Intent(RegistrationActivity.this, ServerSelectionActivity.class);
          startActivityForResult(intent, PICK_SERVER);
        }
        return true;
      }
    });
    this.serverSpinner.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_UP) {
          Intent intent = new Intent(RegistrationActivity.this, ServerSelectionActivity.class);
          startActivityForResult(intent, PICK_SERVER);
          return true;
        }
        return false;
      }
    });

    //Country
    this.countrySpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
    this.countrySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    setCountryDisplay(getString(R.string.RegistrationActivity_select_your_country));

    this.countrySpinner.setAdapter(this.countrySpinnerAdapter);
    this.countrySpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          Intent intent = new Intent(RegistrationActivity.this, CountrySelectionActivity.class);
          startActivityForResult(intent, PICK_COUNTRY);
        }
        return true;
      }
    });
    this.countrySpinner.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_UP) {
          Intent intent = new Intent(RegistrationActivity.this, CountrySelectionActivity.class);
          startActivityForResult(intent, PICK_COUNTRY);
          return true;
        }
        return false;
      }
    });
  }
  //TODO: initialize Server
  private void initializeNumber() {
    PhoneNumberUtil numberUtil  = PhoneNumberUtil.getInstance();
    String          localNumber = Util.getDeviceE164Number(this);

    try {
      if (!TextUtils.isEmpty(localNumber)) {
        Phonenumber.PhoneNumber localNumberObject = numberUtil.parse(localNumber, null);

        if (localNumberObject != null) {
          this.countryCode.setText(String.valueOf(localNumberObject.getCountryCode()));
          this.number.setText(String.valueOf(localNumberObject.getNationalNumber()));
        }
      } else {
        String simCountryIso = Util.getSimCountryIso(this);

        if (!TextUtils.isEmpty(simCountryIso)) {
          this.countryCode.setText(numberUtil.getCountryCodeForRegion(simCountryIso)+"");
        }
      }
    } catch (NumberParseException npe) {
      Log.w(TAG, npe);
    }
  }

  private void setServerDisplay(String value) {
    this.serverSpinnerAdapter.clear();
    this.serverSpinnerAdapter.add(value);
  }

  private void setCountryDisplay(String value) {
    this.countrySpinnerAdapter.clear();
    this.countrySpinnerAdapter.add(value);
  }

  private void setCountryFormatter(int countryCode) {
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();
    String regionCode    = util.getRegionCodeForCountryCode(countryCode);

    if (regionCode == null) this.countryFormatter = null;
    else                    this.countryFormatter = util.getAsYouTypeFormatter(regionCode);
  }

  private String getConfiguredE164Number() {
    return PhoneNumberFormatter.formatE164(countryCode.getText().toString(),
                                           number.getText().toString());
  }

  private class CreateButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      final RegistrationActivity self = RegistrationActivity.this;

      if (TextUtils.isEmpty(countryCode.getText())) {
        Toast.makeText(self,
                       getString(R.string.RegistrationActivity_you_must_specify_your_country_code),
                       Toast.LENGTH_LONG).show();
        return;
      }

      if (TextUtils.isEmpty(serverCode.getText())) {
        Toast.makeText(self,
                getString(R.string.RegistrationActivity_you_must_specify_your_server),
                Toast.LENGTH_LONG).show();
        return;
      }

      if (TextUtils.isEmpty(number.getText())) {
        Toast.makeText(self,
                       getString(R.string.RegistrationActivity_you_must_specify_your_phone_number),
                       Toast.LENGTH_LONG).show();
        return;
      }

      final String e164number = getConfiguredE164Number();
      final String serverUrl = serverCode.getText().toString();
      if (!PhoneNumberFormatter.isValidNumber(e164number)) {
        Dialogs.showAlertDialog(self,
                             getString(R.string.RegistrationActivity_invalid_number),
                             String.format(getString(R.string.RegistrationActivity_the_number_you_specified_s_is_invalid),
                                           e164number));
        return;
      }

      promptForRegistrationStart(self, e164number, serverUrl, false);
    }

    private void promptForRegistrationStart(final Context context, final String e164number, final String serverUrl, final boolean gcmSupported) {
      AlertDialog.Builder dialog = new AlertDialog.Builder(context);
      dialog.setTitle(PhoneNumberFormatter.getInternationalFormatFromE164(e164number));
      dialog.setMessage(R.string.RegistrationActivity_we_will_now_verify_that_the_following_number_is_associated_with_your_device_s);
      dialog.setPositiveButton(getString(R.string.RegistrationActivity_continue),
                               new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                   Intent intent = new Intent(context, RegistrationProgressActivity.class);
                                   intent.putExtra(RegistrationProgressActivity.NUMBER_EXTRA, e164number);
                                   intent.putExtra(RegistrationProgressActivity.SERVER_URL_EXTRA, serverUrl);
                                   intent.putExtra(RegistrationProgressActivity.MASTER_SECRET_EXTRA, masterSecret);
                                   startActivity(intent);
                                   finish();
                                 }
                               });
      dialog.setNegativeButton(getString(R.string.RegistrationActivity_edit), null);
      dialog.show();
    }
  }

  private class ServerChangedListener implements TextWatcher {
    @Override
    public void afterTextChanged(Editable s) {
      if (TextUtils.isEmpty(s)) {
        setServerDisplay(getString(R.string.RegistrationActivity_select_your_server));
        return;
      }

      String serverCode   = s.toString();
      setServerDisplay(serverCode);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
  }

  private class CountryCodeChangedListener implements TextWatcher {
    @Override
    public void afterTextChanged(Editable s) {
      if (TextUtils.isEmpty(s)) {
        setCountryDisplay(getString(R.string.RegistrationActivity_select_your_country));
        countryFormatter = null;
        return;
      }

      int countryCode   = Integer.parseInt(s.toString());
      String regionCode = PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(countryCode);

      setCountryFormatter(countryCode);
      setCountryDisplay(PhoneNumberFormatter.getRegionDisplayName(regionCode));

      if (!TextUtils.isEmpty(regionCode) && !regionCode.equals("ZZ")) {
        number.requestFocus();
      }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
  }

  private class NumberChangedListener implements TextWatcher {

    @Override
    public void afterTextChanged(Editable s) {
      if (countryFormatter == null)
        return;

      if (TextUtils.isEmpty(s))
        return;

      countryFormatter.clear();

      String number          = s.toString().replaceAll("[^\\d.]", "");
      String formattedNumber = null;

      for (int i=0;i<number.length();i++) {
        formattedNumber = countryFormatter.inputDigit(number.charAt(i));
      }

      if (formattedNumber != null && !s.toString().equals(formattedNumber)) {
        s.replace(0, s.length(), formattedNumber);
      }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }
  }

  private class CancelButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      TextSecurePreferences.setPromptedPushRegistration(RegistrationActivity.this, true);
      Intent nextIntent = getIntent().getParcelableExtra("next_intent");

      if (nextIntent == null) {
        nextIntent = new Intent(RegistrationActivity.this, ConversationListActivity.class);
      }

      startActivity(nextIntent);
      finish();
    }
  }

  private class InformationToggleListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      if (informationView.getVisibility() == View.VISIBLE) {
        informationView.setVisibility(View.GONE);
        informationToggleText.setText(R.string.RegistrationActivity_more_information);
      } else {
        informationView.setVisibility(View.VISIBLE);
        informationToggleText.setText(R.string.RegistrationActivity_less_information);
      }
    }
  }
}
