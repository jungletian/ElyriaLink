package com.elyria.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import static com.elyria.elyrialink.ElyriaLinker.getRedirectLink;
import static com.elyria.example.Utils.valueOrDefault;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class LoginActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    String redirectUrl = getRedirectLink(getIntent());
    Toast.makeText(this, "Redirect: " + valueOrDefault(redirectUrl, "N/A"), Toast.LENGTH_LONG)
        .show();

    if (!TextUtils.isEmpty(redirectUrl)) {
      ObjectGraph.get(this).setHasUser(true);
      Intent intent = ObjectGraph.get(this).linkResolver().resolve(redirectUrl);
      startActivity(intent);
      this.finish();
    }
  }
}
