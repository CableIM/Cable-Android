package org.thoughtcrime.securesms.push;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.internal.push.SignalServiceUrl;

public class AccountManagerFactory {

  private static final String TAG = AccountManagerFactory.class.getName();

  public static SignalServiceAccountManager createManager(Context context) {
    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(context),
                                           TextSecurePreferences.getLocalServerUrl(context),
                                           TextSecurePreferences.getLocalNumber(context),
                                           TextSecurePreferences.getPushServerPassword(context)
                                           );
  }

  public static SignalServiceAccountManager createManager(final Context context, String serverurl, String number, String password) {
    if (new SignalServiceNetworkAccess(context).isCensored(serverurl, number)) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
//NOGMS            ProviderInstaller.installIfNeeded(context);
          } catch (Throwable t) {
            Log.w(TAG, t);
          }
          return null;
        }
      }.execute();
    }

    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(serverurl, number),
                                           number, password, BuildConfig.USER_AGENT);
  }

}
