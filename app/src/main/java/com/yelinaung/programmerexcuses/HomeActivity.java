/*
 * Copyright 2014 Ye Lin Aung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yelinaung.programmerexcuses;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.yelinaung.programmerexcuses.model.Excuse;
import com.yelinaung.programmerexcuses.widget.SecretTextView;
import java.util.Random;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class HomeActivity extends AppCompatActivity
    implements SwipeRefreshLayout.OnRefreshListener {

  // View Injections
  @Bind(R.id.quote_text) SecretTextView mQuoteText;
  @Bind(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
  @Bind(R.id.quote_background) RelativeLayout mQuoteBackground;

  private String[] myColors;
  private SharePrefUtils sharePrefUtils;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my);
    ButterKnife.bind(this);

    mQuoteText.show();

    mSwipeRefreshLayout.setColorSchemeResources(R.color.blue, R.color.red, R.color.yellow,
        R.color.green);

    mSwipeRefreshLayout.setProgressViewOffset(true, 50, 120);

    mSwipeRefreshLayout.setEnabled(true);

    sharePrefUtils = SharePrefUtils.getInstance(HomeActivity.this);

    String[] randomQuotes = getResources().getStringArray(R.array.excuses);
    String randomQuote = randomQuotes[new Random().nextInt(randomQuotes.length)];

    // if the app is launched for the first time, then just get the random quote
    if (sharePrefUtils.isFirstTime()) {
      mQuoteText.setText(randomQuote);
      sharePrefUtils.noMoreFirstTime();
    } else {
      mQuoteText.setText(sharePrefUtils.getQuote());
    }

    // In case, if saved quote is null, just set a random quote
    if (sharePrefUtils.getQuote() == null) {
      mQuoteText.setText(randomQuote);
    }

    myColors = getResources().getStringArray(R.array.my_colors);

    String color = "#" + myColors[new Random().nextInt(myColors.length)];
    mQuoteBackground.setBackgroundColor(Color.parseColor(color));

    mSwipeRefreshLayout.setOnRefreshListener(this);
  }

  @SuppressWarnings("UnusedDeclaration") @OnClick(R.id.share_btn) public void share() {
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT,
        String.format(getString(R.string.share_text), mQuoteText.getText()));
    sendIntent.setType("text/plain");
    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_to)));
  }

  public static boolean isOnline(Context c) {
    NetworkInfo netInfo = null;
    try {
      ConnectivityManager cm =
          (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
      netInfo = cm.getActiveNetworkInfo();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    return netInfo != null && netInfo.isConnectedOrConnecting();
  }

  @Override public void onRefresh() {
    mSwipeRefreshLayout.setRefreshing(true);
    if (isOnline(this)) {
      downloadQuote();
    } else {
      mSwipeRefreshLayout.setRefreshing(false);
      Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
    }
  }

  private void downloadQuote() {
    RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(getString(R.string.api))
        .setLogLevel(RestAdapter.LogLevel.BASIC)
        .build();

    ExcuseService service = restAdapter.create(ExcuseService.class);
    service.getExcuse(new Callback<Excuse>() {
      @Override public void success(Excuse excuse, Response response) {
        mQuoteText.show();
        sharePrefUtils.saveQuote(excuse.getMessage()); // save to pref
        mQuoteText.setText(excuse.getMessage());
        String color = "#" + myColors[new Random().nextInt(myColors.length)];
        mQuoteBackground.setBackgroundColor(Color.parseColor(color));

        mSwipeRefreshLayout.setRefreshing(false);
      }

      @Override public void failure(RetrofitError error) {
        // TODO Handle it properly
        mSwipeRefreshLayout.setRefreshing(false);
      }
    });
  }
}
