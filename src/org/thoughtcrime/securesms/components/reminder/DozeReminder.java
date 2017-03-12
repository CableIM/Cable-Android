package org.thoughtcrime.securesms.components.reminder;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

public class DozeReminder extends Reminder {

  @RequiresApi(api = Build.VERSION_CODES.M)
  public DozeReminder(@NonNull final Context context) {
    super(context.getString(R.string.DozeReminder_optimize_for_missing_play_services),
          context.getString(R.string.DozeReminder_this_device_does_not_support_play_services_tap_to_disable_system_battery));

    setOkListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        TextSecurePreferences.setPromptedOptimizeDoze(context, true);
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                   Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
      }
    });

    setDismissListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        TextSecurePreferences.setPromptedOptimizeDoze(context, true);
      }
    });
  }

  public static boolean isEligible(Context context) {
    return !TextSecurePreferences.hasPromptedOptimizeDoze(context) &&
           Build.VERSION.SDK_INT >= Build.VERSION_CODES.M          &&
           !((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getPackageName());
  }

}
