package id.co.plnelectricityservices.wablast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://script.google.com/macros/s/AKfycbybh1IEv2ERNW_j7f9PS0pCyhDYNdGatfSumuL_7IC3nenwXHYqkbpwQ2xGy3DcgL0_/exec";
    private static final int NAVY = Color.rgb(8, 77, 119);
    private static final int TEAL = Color.rgb(0, 151, 155);
    private WebView webView;
    private ProgressBar loading;
    private LinearLayout errorPanel;

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.rgb(8, 37, 64));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 249, 252));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(14), dp(10), dp(10), dp(10));
        header.setBackgroundColor(NAVY);
        TextView title = text("⚡  PLN WA BLASH", 17, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
        Button refresh = new Button(this);
        refresh.setText("↻"); refresh.setTextColor(Color.WHITE); refresh.setTextSize(22);
        refresh.setAllCaps(false); refresh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(TEAL));
        header.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(42)));
        root.addView(header);

        loading = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        loading.setMax(100); loading.setIndeterminate(true);
        root.addView(loading, new LinearLayout.LayoutParams(-1, dp(4)));

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        webView = new WebView(this);
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(30), dp(28), dp(30));
        errorPanel.setBackgroundColor(Color.rgb(244, 249, 252));
        TextView h = text("Tidak dapat membuka aplikasi", 20, NAVY);
        h.setTypeface(null, Typeface.BOLD);
        h.setGravity(Gravity.CENTER);
        TextView description = text("Periksa koneksi internet dan izin akses Web App, lalu coba lagi.", 14, Color.DKGRAY);
        description.setGravity(Gravity.CENTER);
        Button retry = new Button(this); retry.setText("Coba lagi");
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(TEAL));
        errorPanel.addView(h);
        errorPanel.addView(description);
        errorPanel.addView(retry);
        content.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        errorPanel.setVisibility(View.GONE);
        retry.setOnClickListener(v -> openApp());
        refresh.setOnClickListener(v -> { if (webView != null) webView.reload(); });
        setContentView(root);
        configureWebView();
        openApp();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) { loading.setVisibility(View.VISIBLE); loading.setIndeterminate(false); loading.setProgress(progress); }
                else loading.setVisibility(View.GONE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String u = req.getUrl().toString();
                Uri uri = req.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                if (!"https".equals(scheme) && !"http".equals(scheme)) {
                    openOutside(uri); return true;
                }
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                // Google authentication is handled by the normal browser to avoid insecure embedded sign-in.
                if (host.equals("accounts.google.com") || host.equals("myaccount.google.com")) {
                    openOutside(uri); return true;
                }
                return false;
            }
            @Override public void onPageFinished(WebView view, String url) { errorPanel.setVisibility(View.GONE); }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, android.webkit.WebResourceError error) {
                if (req.isForMainFrame()) { errorPanel.setVisibility(View.VISIBLE); loading.setVisibility(View.GONE); }
            }
        });
        webView.setDownloadListener((url, userAgent, disposition, mimeType, contentLength) -> {
            // For authenticated/private documents, leave downloading to a browser.
            openOutside(Uri.parse(url));
        });
    }

    private void openOutside(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Tidak ditemukan aplikasi untuk membuka tautan", Toast.LENGTH_LONG).show(); }
    }
    private void openApp() { errorPanel.setVisibility(View.GONE); loading.setVisibility(View.VISIBLE); webView.loadUrl(APP_URL); }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
