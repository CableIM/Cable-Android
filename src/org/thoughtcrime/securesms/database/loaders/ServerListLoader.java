package org.thoughtcrime.securesms.database.loaders;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.thoughtcrime.securesms.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerListLoader extends AsyncTaskLoader<ArrayList<Map<String, String>>> {
  ArrayList<Map<String, String>> results;
  private String[] serverRegEntries;
  private String[] serverRegEntryValues;

  public ServerListLoader(Context context) {
    super(context);

    serverRegEntries     = context.getResources().getStringArray(R.array.pref_server_entries);
    serverRegEntryValues = context.getResources().getStringArray(R.array.pref_server_values);
  }

  @Override
  public ArrayList<Map<String, String>> loadInBackground() {
    results = new ArrayList<Map<String, String>>(serverRegEntries.length);

    int i = 0;
    for (String server : serverRegEntries) {
      Map<String, String> data = new HashMap<String, String>(2);
      data.put("server_name", server);
      data.put("server_code", serverRegEntryValues[i]);
      results.add(data);
      i++;
    }

    return results;
  }
}
