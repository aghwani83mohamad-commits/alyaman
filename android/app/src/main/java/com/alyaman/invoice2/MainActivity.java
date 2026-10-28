package com.alyaman.invoice2;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // جسر الطباعة والحفظ الذي يعتمد عليه البرنامج (window.AlyPrint)
        getBridge().getWebView().addJavascriptInterface(new AlyPrint(this), "AlyPrint");
    }
}
