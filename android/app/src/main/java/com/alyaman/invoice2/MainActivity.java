package com.alyaman.invoice2;

import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // نفس إعدادات النسخة الأصلية: تجاهل حجم خط النظام وعرض الصفحة بمقياس متوافق
        WebSettings ws = getBridge().getWebView().getSettings();
        ws.setTextZoom(100);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        // جسر الطباعة والحفظ الذي يعتمد عليه البرنامج (window.AlyPrint)
        getBridge().getWebView().addJavascriptInterface(new AlyPrint(this), "AlyPrint");
    }
}
