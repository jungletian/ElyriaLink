package com.elyria.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {
  private static final String EXTRA_URL = "com.elyria.example.EXTRA_HTTP_URL";

  public static Intent create(Context context, String url) {
    Intent intent = new Intent(context, WebViewActivity.class);
    intent.putExtra(EXTRA_URL, url);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_view);

    WebView webView = findViewById(R.id.web_view);
    webView.loadUrl(getIntent().getStringExtra(EXTRA_URL));
  }
}
