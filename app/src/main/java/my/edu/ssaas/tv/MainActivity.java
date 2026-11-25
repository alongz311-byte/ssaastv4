package my.edu.ssaas.tv;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    /* Halaman dihidangkan dari asset dengan alamat https (bukan file://) supaya YouTube & fetch berfungsi */
    private static final String URL = "https://appassets.androidplatform.net/assets/index.html";
    private FrameLayout root;
    private WebView web;
    private WebViewAssetLoader loader;
    private final Handler h = new Handler(Looper.getMainLooper());
    private long lastBack = 0;
    private ImageView splash;
    private long startAt;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B1A);
        setContentView(root);
        hideUi();
        startAt = System.currentTimeMillis();
        createWeb();
        /* Skrin pembuka (wallpaper) di atas WebView */
        splash = new ImageView(this);
        splash.setImageResource(R.drawable.splash);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splash.setBackgroundColor(0xFF070B1A);
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        h.postDelayed(this::hideSplash, 12000);   /* had maksimum jika internet lambat */
    }

    private void createWeb() {
        web = new WebView(this);
        web.setBackgroundColor(0xFF070B1A);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.setKeepScreenOn(true);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);   /* autoplay dengan bunyi */
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !r.isForMainFrame() ? false : !r.getUrl().toString().startsWith("https://appassets.androidplatform.net");
            }
            @Override public void onPageFinished(WebView v, String url) {
                long wait = Math.max(1500, 4000 - (System.currentTimeMillis() - startAt));
                h.postDelayed(MainActivity.this::hideSplash, wait);
            }
            @Override public void onReceivedError(WebView v, int code, String desc, String url) {
                /* Halaman utama gagal dimuat -> cuba semula 30 saat kemudian */
                h.postDelayed(MainActivity.this::reload, 30000);
            }
            /* PENTING: jika enjin WebView ranap (punca skrin kosong), bina semula tanpa tutup app */
            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                recreateWeb();
                return true;
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        web.requestFocus();
        web.loadUrl(URL);
    }

    private void hideSplash() {
        final ImageView s = splash;
        if (s == null) return;
        splash = null;
        s.animate().alpha(0f).setDuration(800).withEndAction(() -> {
            root.removeView(s);
            s.setImageDrawable(null);   /* lepaskan memori gambar */
        }).start();
        getWindow().setBackgroundDrawableResource(android.R.color.black);
    }

    private void recreateWeb() {
        try { root.removeView(web); web.destroy(); } catch (Exception ignored) {}
        web = null;
        h.postDelayed(this::createWeb, 1000);
    }

    private void reload() { if (web != null) web.loadUrl(URL); }

    private void hideUi() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideUi(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }

    /* Butang BACK: tekan 2 kali dalam 2 saat untuk keluar (elak tertutup tidak sengaja) */
    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_BACK) {
            long n = System.currentTimeMillis();
            if (n - lastBack < 2000) { finish(); return true; }
            lastBack = n;
            Toast.makeText(this, "Tekan BACK sekali lagi untuk keluar", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(k, e);
    }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
