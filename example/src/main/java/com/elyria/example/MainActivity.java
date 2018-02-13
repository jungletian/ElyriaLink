package com.elyria.example;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.elyria.elyrialink.ElyriaLink;
import com.elyria.elyrialink.ElyriaLinkQuery;
import com.elyria.elyrialink.ElyriaLinkResolver;
import com.elyria.elyrialink.Json;
import com.elyria.elyrialink.builders.ElyriaLinkBuilders;
import timber.log.Timber;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class MainActivity extends AppCompatActivity {
  private ElyriaLinkResolver elyriaLinkResolver;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    elyriaLinkResolver = ObjectGraph.get(this).linkResolver();
  }

  public void showErrorUrl(View view) {
    show(null);
  }

  public void showHttpUrl(View view) {
    show("https://www.baidu.com");
  }

  public void showProductUrl(View view) {
    String productUrl = ElyriaLinkBuilders.productDetailLinkBuilder()
        .withId(10010L)
        .withPrice(1024.2f)
        .withProductName("product name")
        .withIds(new long[] { 1, 2, 3, 4, 5 })
        .build();
    Timber.d("productUrl = %s", productUrl);
    show(productUrl);
  }

  private void show(String url) {
    startActivity(elyriaLinkResolver.resolve(url));
  }
}
