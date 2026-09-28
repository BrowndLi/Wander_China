package com.browndli.wanderchina;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 游迹中国 Wander China 安卓外壳。
 * 用系统网页组件打开手机版网站。网站自带离线缓存；第一次打开又没有网络时，改用安装包里的离线备份。
 * 站外链接（高德地图、门票平台等）交给手机上的浏览器或对应 App 打开。
 */
public class MainActivity extends Activity {
    private static final String HOME = "https://browndli.github.io/Wander_China/";
    private static final String SITE_HOST = "browndli.github.io";
    private static final String SITE_PATH = "/Wander_China";
    private static final String OFFLINE = "file:///android_asset/www/index.html";
    private static final int REQ_LOCATION = 1;
    private static final int REQ_FILE = 2;

    private WebView web;
    private boolean offlineShown = false;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);
        s.setUserAgentString(s.getUserAgentString() + " WanderChinaApp/1.0");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openOutside(request.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && !offlineShown) {
                    offlineShown = true;
                    String fragment = request.getUrl().getFragment();
                    view.loadUrl(fragment == null ? OFFLINE : OFFLINE + "#" + fragment);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                    return;
                }
                geoOrigin = origin;
                geoCallback = callback;
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(HOME);
        }
    }

    /** 站内地址留在应用里，其余交给系统打开。返回 true 表示已交给系统。 */
    private boolean openOutside(Uri uri) {
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) return false;
        String path = uri.getPath();
        if ("https".equals(scheme) && SITE_HOST.equals(uri.getHost()) && path != null && path.startsWith(SITE_PATH)) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // 手机上没有能打开这个链接的应用，忽略
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && geoCallback != null) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            geoCallback.invoke(geoOrigin, granted, false);
            geoCallback = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /** 返回键：先让网页关闭打开的详情或搜索结果，再后退，最后退出。 */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("(window.YOUJI && YOUJI.back && YOUJI.back()) ? '1' : '0'", value -> {
            if (value != null && value.contains("1")) return;
            WebBackForwardList list = web.copyBackForwardList();
            int i = list.getCurrentIndex();
            if (i <= 0) {
                finish();
                return;
            }
            String prev = list.getItemAtIndex(i - 1).getUrl();
            // 离线备份页的上一页是加载失败的网址，后退只会看到报错页，直接退出
            if (offlineShown && prev != null && !prev.startsWith("file:")) {
                finish();
                return;
            }
            web.goBack();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
