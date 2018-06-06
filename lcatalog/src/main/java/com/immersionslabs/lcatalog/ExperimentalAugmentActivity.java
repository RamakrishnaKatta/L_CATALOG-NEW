package com.immersionslabs.lcatalog;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ExperimentalAugmentActivity extends AppCompatActivity {

    private static final String TAG = "ExperimentalAugmentActivity";

    String article_augment_file_data;
    String WEB_URL_AUGMENT = "https://lcatalog.immersionslabs.com/#/articleAr/";

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_augment);

        Toolbar toolbar = findViewById(R.id.toolbar_exp_augment);
        toolbar.setTitleTextAppearance(this, R.style.LCatalogCustomText_ToolBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        final Bundle article_augment_data = getIntent().getExtras();
        assert article_augment_data != null;
        article_augment_file_data = (String) article_augment_data.getCharSequence("article_augment_file");

        WEB_URL_AUGMENT += article_augment_file_data;
        Log.e(TAG, "VENDOR_URL--" + WEB_URL_AUGMENT);

        WebView webView_augment = findViewById(R.id.webView_augment);
        webView_augment.loadUrl(WEB_URL_AUGMENT);
        webView_augment.clearCache(true);

        WebSettings webSettings = webView_augment.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
        finish();
    }
}
