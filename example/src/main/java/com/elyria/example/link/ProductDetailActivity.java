package com.elyria.example.link;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.elyria.elyrialink.ElyriaLink;
import com.elyria.elyrialink.ElyriaLinkQuery;
import com.elyria.elyrialink.ElyriaLinker;
import com.elyria.elyrialink.IllegalElyriaLinkException;
import com.elyria.elyrialink.Json;
import com.elyria.example.R;
import timber.log.Timber;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
@ElyriaLink(value = "elyria://product/detail", userRequired = true)
public class ProductDetailActivity extends AppCompatActivity {

  @ElyriaLinkQuery(value = ElyriaLinker.EXTRA_ELYRIA_LINK) String link;
  @ElyriaLinkQuery(value = "product_name", optional = true) String productName;
  @ElyriaLinkQuery("id") long productId;
  @ElyriaLinkQuery("price") float productPrice;
  @Json @ElyriaLinkQuery("ids") long[] ids;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_product);
    try {
      ElyriaLinker.bind(this);
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("link: ").append(link);
      stringBuilder.append("productName: ").append(productName);
      stringBuilder.append("\n").append("productId: ").append(productId);
      stringBuilder.append("\n").append("price: ").append(productPrice);
      stringBuilder.append("\n").append("products: ");
      stringBuilder.append(getIntent().getStringExtra("ids"));
      stringBuilder.append("\n");
      for (long id : ids) {
        stringBuilder.append("id: ").append(id);
      }
      Timber.d("link query result:\n%s", stringBuilder.toString());
      //getFragmentManager().beginTransaction().add(new DetailFragment(), "detail_fragment").commit();

      TextView textView = findViewById(R.id.product_detail);
      textView.setText(stringBuilder.toString());
    } catch (IllegalElyriaLinkException e) {
      Timber.e(e, "ERROR");
    }
  }
}
