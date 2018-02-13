package com.elyria.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.elyria.elyrialink.ElyriaLinker;

public class ErrorLinkActivity extends AppCompatActivity {

  public static Intent create(Context context, String url) {
    return new Intent(context, ErrorLinkActivity.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_error_link);
    String errorLink = ElyriaLinker.getElyriaLink(getIntent());
    TextView errorView = (TextView) findViewById(R.id.error_text_view);
    errorView.setText(Utils.valueOrDefault(errorLink, "N/A"));
  }
}
