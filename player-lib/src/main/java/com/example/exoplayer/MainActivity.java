package com.example.exoplayer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.exoplayer.NetflixApiUtils.ApiBaseUrls.MSL_API_URL;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    WebView mWebView;
    Button mPlayButton;

    String pendingUsername;
    String pendingPassword;

    Pattern NETFLIX_WEB_URL_PATTERN = Pattern.compile("netflix.com\\/watch\\/(.*)\\?preventIntent=true");

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.webView);
        mPlayButton = findViewById(R.id.btnPlayer);

        mPlayButton.setOnClickListener(v -> startActivity(new Intent(v.getContext(), PlayerActivity.class)));
//        mPlayButton.setOnClickListener(v -> startActivity(new Intent(v.getContext(), MediaPlayerActivtiy.class)));

        WebView.setWebContentsDebuggingEnabled(true);

        mWebView.loadUrl("https://www.netflix.com");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(false);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.96 Mobile Safari/537.36");
//        mWebView.getSettings().setUserAgentString(NetflixApiUtils.UserAgent.getFromApiUrl(MSL_API_URL));
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        CookieManager.getInstance().setCookie(".netflix.com", "forceWebsite=true; path=/");

        mWebView.addJavascriptInterface(new JavaScriptInterface(), "JSInterface");

        loadNetflix("https://netflix.com/login");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void loadNetflix(String url) {
        HashMap <String, String> extraHeaders = new HashMap<String, String>();
//        extraHeaders.put("User-Agent", "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Mobile Safari/537.36");
        extraHeaders.put("User-Agent", "Mozilla/5.0 (X11; CrOS armv7l 13020.82.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.112 Safari/537.36");
//        extraHeaders.put("User-Agent", NetflixApiUtils.UserAgent.getFromApiUrl(MSL_API_URL));
        mWebView.loadUrl(url, extraHeaders);

        mWebView.setWebViewClient(new WebViewClient() {
            //            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectJavascriptOnly();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("intent://")) {
                    try {
                        Intent parseUri = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (parseUri != null) {
                            view.stopLoading();
                            view.loadUrl(parseUri.getStringExtra("browser_fallback_url"));
                            return true;
                        }
                    } catch (URISyntaxException exception) {
                        exception.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageFinished(view, url);
                if ((url.contains("/browse"))) {
                    Log.d(TAG, "Initializing MSL Native Session...");
                    MslNativeSession.init();
                    Log.d(TAG, "MSL Native Session Initialized.");
                }

                if (url.contains("/watch")) {
                    startVideo(url);
                }
            }

        });

        mWebView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                super.onProgressChanged(view, newProgress);
//            }

//            @Override
//            public void onReceivedTitle(WebView view, String title) {
//                super.onReceivedTitle(view, title);
//                getSupportActionBar().setTitle(title);
//            }

//            @Override
//            public void onReceivedIcon(WebView view, Bitmap icon) {
//                super.onReceivedIcon(view, icon);
//                superImageView.setImageBitmap(icon);
//            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources;
                resources = request.getResources();
                for (int i = 0; i < resources.length; i++) {
                    Log.d("PermissionRequest", resources[i].toString());
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resources[i])) {
                        Log.d("Granted", resources[i].toString());
                        request.grant(resources);
                        return;
                    }
                }
                super.onPermissionRequest(request);
            }
        });

        injectJavascriptOnly();
    }


    private void startVideo(String url) {
        Log.d(TAG, "startVideo() = " + url);
        if (!url.isEmpty()) {
            Matcher matcher =  NETFLIX_WEB_URL_PATTERN.matcher(url);
            if (matcher.find()) {
                String videoID = matcher.group(1);
                loadNetflixManifest(videoID);
            }
        }
    }

    private void loadNetflixManifest(String videoID) {
        Log.d(TAG, "loadNetflixManifest() videoID = " + videoID);

        MslNativeSession.getInstance().getManifest(videoID, new MslNativeSession.MslCallback() {
            public void onSuccess(@Nullable NetflixManifest netflixManifest) {
                if (netflixManifest != null) {
                    if (netflixManifest.getError() != null) {
                        Log.e(TAG, "onSuccess: manifest error");
                    } else {
                        boolean hasWrittenManifest = NetflixManifestGenerator.writeJSONManifest(netflixManifest);
                        String dashManifestPath = NetflixManifestGenerator.getDashManifestPath();
                        Log.d(TAG, "Manifest path " + dashManifestPath);
                    }
                }
            }

            public void onSuccess(Object netflixManifest) {
                this.onSuccess((NetflixManifest)netflixManifest);
            }

            @Override
            public void onFailure() {

            }

            @Override
            public void onFailure(MslErrorResponse mslErrorResponse) {

            }

            @Override
            public void onFailure(NetflixError netflixError) {

            }

        });
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void injectJavascriptOnly() {
        runOnUiThread(() -> injectJs());
    }

    @Override
    public void onBackPressed() {
        if (!canGoBack()) super.onBackPressed();
    }

    private Boolean canGoBack() {
        if (!mWebView.canGoBack()) {
            return false;
        }
        mWebView.goBack();
        return true;
    }

    private String ReadFromfile(String fileName) { // , Context context
        StringBuilder returnString = new StringBuilder();
        InputStream fIn = null;
        InputStreamReader isr = null;
        BufferedReader input = null;
        try {
            fIn = getAssets().open(fileName); // , Context.MODE_WORLD_READABLE
            isr = new InputStreamReader(fIn);
            input = new BufferedReader(isr);
            String line = "";
            while ((line = input.readLine()) != null) {
                returnString.append(line);
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                if (isr != null)
                    isr.close();
                if (fIn != null)
                    fIn.close();
                if (input != null)
                    input.close();
            } catch (Exception e2) {
                e2.getMessage();
            }
        }
        return returnString.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void injectJs() {
        try {
            String script = ReadFromfile("netflix.js");
            StringBuilder sb = new StringBuilder();
            sb.append("javascript:(function() {");
//            sb.append("var parent = document.getElementsByTagName('head').item(0);");
            sb.append("var script = document.createElement('script');");
            sb.append("script.type = 'text/javascript';");
            sb.append("script.innerHTML = '");
            sb.append(script);
            sb.append("';");
            sb.append("document.head.appendChild(script);");
            sb.append("console.log('added netflix functions to javascript');");
            sb.append("})()");
            Log.d("InjectJs", sb.toString());
            mWebView.evaluateJavascript(sb.toString(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class JavaScriptInterface {
        @JavascriptInterface
        public void saveLogin(String username, String password) {
            Log.d(TAG, "Credentials - " + username + " : " + password);
        }
    }
}