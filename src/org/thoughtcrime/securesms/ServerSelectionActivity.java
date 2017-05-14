package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;

public class ServerSelectionActivity extends BaseActivity
        implements ServerSelectionFragment.ServerSelectedListener
{

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.server_selection);
    }

    @Override
    public void serverSelected(String serverName, String serverCode) {
        Intent result = getIntent();
        result.putExtra("server_name", serverName);
        result.putExtra("server_code", serverCode);

        this.setResult(RESULT_OK, result);
        this.finish();
    }
}