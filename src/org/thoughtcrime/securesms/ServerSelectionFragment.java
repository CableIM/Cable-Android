package org.thoughtcrime.securesms;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.thoughtcrime.securesms.database.loaders.ServerListLoader;

import java.util.ArrayList;
import java.util.Map;

public class ServerSelectionFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<Map<String, String>>> {

  private EditText serverFilter;
  private ServerSelectedListener listener;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    return inflater.inflate(R.layout.server_selection_fragment, container, false);
  }

  @Override
  public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);
    this.serverFilter = (EditText)getView().findViewById(R.id.server_search);
    this.serverFilter.addTextChangedListener(new FilterWatcher());
    getLoaderManager().initLoader(0, null, this).forceLoad();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    this.listener = (ServerSelectedListener)activity;
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    Map<String, String> item = (Map<String, String>)this.getListAdapter().getItem(position);
    if (this.listener != null) {
      this.listener.serverSelected(item.get("server_name"), item.get("server_code"));
    }
  }

  @Override
  public Loader<ArrayList<Map<String, String>>> onCreateLoader(int arg0, Bundle arg1) {
    return new ServerListLoader(getActivity());
  }

  @Override
  public void onLoadFinished(Loader<ArrayList<Map<String, String>>> loader,
                             ArrayList<Map<String, String>> results)
  {
    String[] from = {"server_name", "server_code"};
    int[] to      = {R.id.server_name, R.id.server_code};
    this.setListAdapter(new SimpleAdapter(getActivity(), results, R.layout.server_list_item, from, to));

    if (this.serverFilter != null && this.serverFilter.getText().length() != 0) {
      ((SimpleAdapter)getListAdapter()).getFilter().filter(this.serverFilter.getText().toString());
    }
  }

  @Override
  public void onLoaderReset(Loader<ArrayList<Map<String, String>>> arg0) {
    this.setListAdapter(null);
  }

  public interface ServerSelectedListener {
    public void serverSelected(String serverName, String serverCode);
  }

  private class FilterWatcher implements TextWatcher {
    @Override
    public void afterTextChanged(Editable s) {
      if (getListAdapter() != null) {
        ((SimpleAdapter)getListAdapter()).getFilter().filter(s.toString());
      }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
  }
}
