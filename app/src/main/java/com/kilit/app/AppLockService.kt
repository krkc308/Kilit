package com.kilit.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class AppLockService : AccessibilityService() {

    companion object {
        @Volatile
        var ornek: AppLockService? = null
    }

    private var wm: WindowManager? = null
    private var kaplama: PinView? = null
    private var kaplamaPaket: String? = null
    private var acik: String? = null
    private var onPlan: String? = null
    private var bekleyen: String? = null
    private val sonCikis = HashMap<String, Long>()
    private val yoksay = HashSet<String>()
    private var dinleyici: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ornek = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        yoksay.add("com.android.systemui")
        yoksay.add("android")
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            for (m in imm.enabledInputMethodList) yoksay.add(m.packageName)
        } catch (e: Exception) {
        }

        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                when (i?.action) {
                    Intent.ACTION_SCREEN_OFF -> ekranKapandi()
                    Intent.ACTION_USER_PRESENT -> ekranAcildi()
                }
            }
        }
        dinleyici = r
        val f = IntentFilter()
        f.addAction(Intent.ACTION_SCREEN_OFF)
        f.addAction(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(r, f)
        }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paket = e.packageName?.toString() ?: return
        val sinif = e.className?.toString() ?: ""
        if (paket == packageName) {
            if (sinif != MainActivity::class.java.name) return
        } else if (paket in yoksay) {
            return
        }
        onPlan = paket
        kontrol(paket)
    }

    private fun kontrol(paket: String) {
        val simdi = System.currentTimeMillis()

        val onceki = acik
        if (onceki != null && onceki != paket) {
            sonCikis[onceki] = simdi
            acik = null
        }

        if (paket !in Depo.kilitli(this)) {
            kaplamayiKaldir()
            return
        }
        if (paket == acik) return
        if (paket == kaplamaPaket && kaplama != null) return

        val gecikmeMs = Depo.gecikmeSn(this) * 1000L
        val son = sonCikis[paket]
        if (son != null && gecikmeMs > 0 && simdi - son < gecikmeMs) {
            sonCikis.remove(paket)
            acik = paket
            kaplamayiKaldir()
            return
        }
        kaplamaGoster(paket)
    }

    private fun ekranKapandi() {
        val a = acik
        if (a != null) {
            sonCikis[a] = System.currentTimeMillis()
            acik = null
        }
        val p = onPlan
        bekleyen = if (p != null && p in Depo.kilitli(this)) p else null
        kaplamayiKaldir()
    }

    private fun ekranAcildi() {
        val b = bekleyen ?: return
        bekleyen = null
        if (onPlan == b) kontrol(b)
    }

    fun hepsiniKilitle() {
        acik = null
        sonCikis.clear()
    }

    private fun etiket(p: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)).toString()
        } catch (e: Exception) {
            p
        }
    }

    private fun kaplamaGoster(paket: String) {
        kaplamayiKaldir()
        val v = PinView(this)
        v.beklemeKontrol = true
        v.baslikAyarla(etiket(paket))
        try {
            v.ikonAyarla(packageManager.getApplicationIcon(paket))
        } catch (e: Exception) {
        }
        v.onTamam = { girilen ->
            if (Depo.pinDogru(this, girilen)) {
                Depo.dogruGirildi()
                acik = paket
                sonCikis.remove(paket)
                kaplamayiKaldir()
            } else {
                val kalan = Depo.yanlisGirildi(this)
                v.hata(if (kalan > 0) "Yanlış PIN. Kalan hak: $kalan" else "30 sn kilitlendi")
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        try {
            wm?.addView(v, lp)
            kaplama = v
            kaplamaPaket = paket
        } catch (e: Exception) {
        }
    }

    private fun kaplamayiKaldir() {
        val v = kaplama ?: return
        try {
            wm?.removeView(v)
        } catch (e: Exception) {
        }
        kaplama = null
        kaplamaPaket = null
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        kaplamayiKaldir()
        if (ornek === this) ornek = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        kaplamayiKaldir()
        val d = dinleyici
        if (d != null) {
            try {
                unregisterReceiver(d)
            } catch (e: Exception) {
            }
        }
        dinleyici = null
        if (ornek === this) ornek = null
        super.onDestroy()
    }
}
