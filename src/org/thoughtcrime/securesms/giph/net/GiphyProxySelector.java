package org.thoughtcrime.securesms.giph.net;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GiphyProxySelector extends ProxySelector {

  private static final String TAG = GiphyProxySelector.class.getSimpleName();
  private final Context context;

  private final    List<Proxy> EMPTY = new ArrayList<>(1);
  private volatile List<Proxy> GIPHY = null;

  public GiphyProxySelector(Context context) {
    this.context = context;

    EMPTY.add(Proxy.NO_PROXY);

    if (Util.isMainThread()) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          synchronized (GiphyProxySelector.this) {
            initializeGiphyProxy();
            GiphyProxySelector.this.notifyAll();
          }
          return null;
        }
      }.execute();
    } else {
      initializeGiphyProxy();
    }
  }

  @Override
  public List<Proxy> select(URI uri) {
    if (uri.getHost().endsWith("giphy.com")) return getOrCreateGiphyProxy();
    else                                     return EMPTY;
  }

  @Override
  public void connectFailed(URI uri, SocketAddress address, IOException failure) {
    Log.w(TAG, failure);
  }

  private void initializeGiphyProxy() {
    GIPHY = new ArrayList<Proxy>(1) {{
      add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(TextSecurePreferences.getGiphyProxyHost(context),
                                                           TextSecurePreferences.getGiphyProxyPort(context))));
    }};
  }

  private List<Proxy> getOrCreateGiphyProxy() {
    if (GIPHY == null) {
      synchronized (this) {
        while (GIPHY == null) Util.wait(this, 0);
      }
    }

    return GIPHY;
  }

}
