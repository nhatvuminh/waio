package com.waiosoft.tvapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

public class FullscreenActivity extends AppCompatActivity {
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    public String discoveryPage = "https://tv.waio.app/";
    public WebView myWebView;
    public TextView txtConnect;
    public String screenID = "";
    public String companyID = "";
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String APPMODE = "WAIO";
    public boolean retrying = false;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_FULLSCREEN|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen);
        String MyUA = "Waio Screen";
        loadSettings();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        //pref = getApplicationContext().getSharedPreferences("WaioScreen", MODE_PRIVATE);
        editor = pref.edit();
//        screenID = pref.getString("ScreenID", "0");
//        if (screenID == "" || screenID == "0") {
//            Toast.makeText(getApplicationContext(), "No Screen ID...", Toast.LENGTH_LONG).show();
//            showDeviceNamePrompt();
//        }

        String loadedScreenURL = pref.getString("screenURL",discoveryPage);
        if (loadedScreenURL == "" || !loadedScreenURL.toLowerCase().startsWith("http")){
            loadedScreenURL = discoveryPage;
            editor.putString("screenURL", loadedScreenURL);
            editor.apply();
        }
        discoveryPage = loadedScreenURL;
        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        myWebView = (WebView) findViewById(R.id.fullscreen_content);
        txtConnect = findViewById(R.id.txtReconnect);
        txtConnect.setText("Connecting...");
        // myWebView.setWebViewClient(new WebViewClient());
/*
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            //    Toast.makeText(getApplicationContext(), "Retrying...", Toast.LENGTH_LONG).show();
                retryLoad();
            }
        });
*/
        CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true);
        CookieManager.getInstance().setAcceptCookie(true);
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (error.getErrorCode() == -2) {
                      //No internet, do nothing. Chrome will automatically attempt a reload once the connection is detected
                } else {
                    //its another error, reload
                    retryLoad();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Toast.makeText(getApplicationContext(), "HTTP ERROR:" + errorResponse.getStatusCode(), Toast.LENGTH_LONG).show();
                retryLoad();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        myWebView.getSettings().setUserAgentString(MyUA);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setSupportZoom(false);
        myWebView.addJavascriptInterface(new JSInterface(this), "Android");
        loadPage();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        APPMODE = prefs.getString("appMode", "CannStat");
    }

    public void showDeviceNamePrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Company ID");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        input.setText(screenID);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                companyID = input.getText().toString();
                editor.putString("companyID", companyID);  // Saving string
                editor.apply();
//                Toast.makeText(getApplicationContext(), "Screen ID Set As: " + screenID, Toast.LENGTH_LONG).show();
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        loadPage();
//                        //myWebView.evaluateJavascript("javascript: nameUpdated()", null);
//                    }
//                });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    public void retryLoad() {
        try {
            txtConnect.setText("Error connecting to the internet. Retrying...");
            retrying = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadPage();
                }
            }, 10000);
        } catch (Exception e) {
        }
    }

    public class JSInterface {
        Context mContext;

        JSInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void showToast(String message) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void rebootDevice() {
            Toast.makeText(mContext, "Rebooting...", Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void changeScreenID() {
            showDeviceNamePrompt();
        }
    }

    public void loadPage() {
        String page = discoveryPage;
//        if (screenID != "") {
//            page = page + "/" + screenID;
//        } else {
//            page = page + "/0";
//        }
        //  Toast.makeText(getApplicationContext(), "Loading:" + page, Toast.LENGTH_LONG).show();
        Log.v("WAIO", "Loading Page...");
        myWebView.loadUrl(page);
        Log.v("WAIO", "Loaded.");
        retrying = false;
        if (!isConnected(this.getApplicationContext())) {
            Log.v("WAIO", "Not connected....");
            // Toast.makeText(this.getApplicationContext(), "Reconnecting...", Toast.LENGTH_SHORT).show();
            retryLoad();
        } else {
            Log.v("WAIO", "IS CONNECTED, WAITING ON PAGE....");
            txtConnect.setVisibility(View.GONE);
            myWebView.setVisibility(View.VISIBLE);
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null != cm) {
            NetworkInfo info = cm.getActiveNetworkInfo();

            return (info != null && info.isConnected());
        }
        return false;
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
