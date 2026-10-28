package com.alyaman.invoice2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * جسر JavaScript باسم AlyPrint يستخدمه البرنامج:
 *   AlyPrint.printHtml(html, title)            -> طباعة / حفظ PDF
 *   AlyPrint.saveFile(name, mime, base64)      -> حفظ ملف في مجلد التنزيلات، يعيد "ok" أو نص الخطأ
 */
public class AlyPrint {
    private final Activity activity;
    private WebView printView; // يبقى مرجعًا حتى تنتهي الطباعة

    public AlyPrint(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void printHtml(final String html, final String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String jobName = (title == null || title.trim().isEmpty()) ? "Alyaman" : title;
                    final WebView wv = new WebView(activity);
                    printView = wv;
                    wv.getSettings().setJavaScriptEnabled(false);
                    wv.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            try {
                                PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                                pm.print(jobName, adapter, new PrintAttributes.Builder().build());
                            } catch (Exception ignored) {
                            }
                        }
                    });
                    wv.loadDataWithBaseURL("https://alyaman.local/", html == null ? "" : html,
                            "text/html", "UTF-8", null);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @JavascriptInterface
    public String saveFile(String name, String mime, String base64) {
        try {
            if (name == null || name.trim().isEmpty()) name = "file";
            if (mime == null || mime.isEmpty()) mime = "application/octet-stream";
            byte[] data = Base64.decode(base64, Base64.DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver cr = activity.getContentResolver();
                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                Uri target = null;
                // إعادة استخدام الملف نفسه (مثل الحفظ التلقائي) بدل تكرار الأسماء
                Cursor c = cr.query(collection, new String[]{MediaStore.Downloads._ID},
                        MediaStore.Downloads.DISPLAY_NAME + "=?", new String[]{name}, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            target = Uri.withAppendedPath(collection, String.valueOf(c.getLong(0)));
                        }
                    } finally {
                        c.close();
                    }
                }
                OutputStream os = null;
                if (target != null) {
                    try {
                        os = cr.openOutputStream(target, "wt");
                    } catch (Exception e) {
                        os = null; // ملف يملكه تطبيق آخر: ننشئ نسخة جديدة
                        target = null;
                    }
                }
                if (os == null) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    target = cr.insert(collection, v);
                    if (target == null) return "تعذر إنشاء الملف";
                    os = cr.openOutputStream(target, "wt");
                }
                if (os == null) return "تعذر فتح الملف للكتابة";
                try {
                    os.write(data);
                    os.flush();
                } finally {
                    os.close();
                }
                return "ok";
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    fos.write(data);
                    fos.flush();
                } finally {
                    fos.close();
                }
                return "ok";
            }
        } catch (Throwable t) {
            return String.valueOf(t.getMessage() == null ? t.toString() : t.getMessage());
        }
    }
}
