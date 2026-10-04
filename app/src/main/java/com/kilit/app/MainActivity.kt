package com.kilit.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.Collator
import java.util.Locale

class Uyg(val ad: String, val paket: String, val ikon: Drawable)

class MainActivity : Activity() {

    private lateinit var kok: FrameLayout
    private var oturumAcik = false
    private var gosterilen = ""
    private var durumYazi: TextView? = null
    private var durumDugme: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Renk.BG
        window.navigationBarColor = Renk.BG
        kok = FrameLayout(this)
        kok.setBackgroundColor(Renk.BG)
        setContentView(kok)
    }

    override fun onResume() {
        super.onResume()
        val g = Depo.gecikmeSn(this) * 1000L
        val gecen = System.currentTimeMillis() - Depo.sonAktif(this)
        if (oturumAcik) {
            if (g == 0L || gecen > g) oturumAcik = false
        } else if (Depo.pinVar(this) && g > 0 && gecen >= 0 && gecen < g) {
            oturumAcik = true
        }
        ekranGoster()
    }

    override fun onPause() {
        super.onPause()
        if (oturumAcik) Depo.sonAktifAyarla(this, System.currentTimeMillis())
    }

    // ---------- ekran seçimi ----------
    private fun ekranGoster() {
        val hedef = when {
            !Depo.pinVar(this) -> "kur"
            !oturumAcik -> "giris"
            else -> "panel"
        }
        if (hedef == "panel" && gosterilen == "panel") {
            durumGuncelle()
            return
        }
        gosterilen = hedef
        kok.removeAllViews()
        when (hedef) {
            "kur" -> pinKur(false)
            "giris" -> pinGiris()
            else -> panel()
        }
    }

    private fun pinGiris() {
        val v = PinView(this)
        v.beklemeKontrol = true
        v.baslikAyarla("PIN gir")
        v.onTamam = { girilen ->
            if (Depo.pinDogru(this, girilen)) {
                Depo.dogruGirildi()
                oturumAcik = true
                Depo.sonAktifAyarla(this, System.currentTimeMillis())
                ekranGoster()
            } else {
                val kalan = Depo.yanlisGirildi(this)
                v.hata(if (kalan > 0) "Yanlış PIN. Kalan hak: $kalan" else "30 sn kilitlendi")
            }
        }
        kok.addView(v, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    private fun pinKur(degistirme: Boolean) {
        val v = PinView(this)
        var ilk: String? = null
        v.baslikAyarla("Yeni PIN belirle")
        if (degistirme) {
            v.iptalGoster {
                gosterilen = ""
                ekranGoster()
            }
        }
        v.onTamam = { girilen ->
            val onceki = ilk
            if (onceki == null) {
                ilk = girilen
                v.baslikAyarla("PIN'i tekrar gir")
                v.sifirla()
            } else if (onceki == girilen) {
                Depo.pinKaydet(this, girilen)
                oturumAcik = true
                Depo.sonAktifAyarla(this, System.currentTimeMillis())
                gosterilen = ""
                ekranGoster()
            } else {
                ilk = null
                v.baslikAyarla("Yeni PIN belirle")
                v.hata("PIN'ler uyuşmuyor")
            }
        }
        kok.addView(v, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    // ---------- yardımcılar ----------
    private fun tv(t: String, boyut: Float, renk: Int, kalin: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = boyut
        v.setTextColor(renk)
        if (kalin) v.typeface = Typeface.DEFAULT_BOLD
        return v
    }

    private fun kutu(renk: Int = Renk.KART, cerceve: Int = 0): LinearLayout {
        val k = LinearLayout(this)
        k.orientation = LinearLayout.HORIZONTAL
        k.gravity = Gravity.CENTER_VERTICAL
        k.setPadding(dp(14), dp(12), dp(14), dp(12))
        k.background = yuvarlak(renk, 14f, this, cerceve)
        return k
    }

    private fun kartLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
        return lp
    }

    private fun bolum(t: String): TextView {
        val v = tv(t, 12f, Renk.SOLUK, true)
        v.setPadding(dp(4), dp(20), 0, dp(6))
        return v
    }

    private fun ipucu(t: String): TextView {
        val v = tv(t, 12f, Renk.SOLUK)
        v.setPadding(dp(4), dp(2), dp(4), dp(6))
        return v
    }

    private fun durumBari(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    // ---------- ana panel ----------
    private fun panel() {
        val gen = resources.displayMetrics.widthPixels

        val ana = LinearLayout(this)
        ana.orientation = LinearLayout.VERTICAL
        ana.setBackgroundColor(Renk.BG)

        val baslik = tv("Uygulama Kilidi", 22f, Renk.YAZI, true)
        baslik.setPadding(dp(20), durumBari() + dp(16), dp(20), dp(8))
        ana.addView(baslik)

        val sekmeler = LinearLayout(this)
        sekmeler.orientation = LinearLayout.HORIZONTAL
        val sekmeT = listOf("Uygulamalar", "Ayarlar").map { ad ->
            val t = tv(ad, 15f, Renk.SOLUK, true)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(12), 0, dp(12))
            t
        }
        for (t in sekmeT) sekmeler.addView(t, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        ana.addView(sekmeler, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val gosterge = View(this)
        gosterge.setBackgroundColor(Renk.ACIK)
        val cizgi = FrameLayout(this)
        cizgi.setBackgroundColor(Renk.KART)
        cizgi.addView(gosterge, FrameLayout.LayoutParams(gen / 2, dp(3)))
        ana.addView(cizgi, LinearLayout.LayoutParams(MATCH_PARENT, dp(3)))

        val sekmeRenk = { aktif: Int ->
            for ((i, t) in sekmeT.withIndex()) {
                t.setTextColor(if (i == aktif) Renk.YAZI else Renk.SOLUK)
            }
        }
        sekmeRenk(0)

        val hsv = object : HorizontalScrollView(this) {
            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                super.onScrollChanged(l, t, oldl, oldt)
                gosterge.translationX = l / 2f
                sekmeRenk(if (l > gen / 2) 1 else 0)
            }
        }
        hsv.isHorizontalScrollBarEnabled = false
        hsv.overScrollMode = View.OVER_SCROLL_NEVER
        hsv.isFillViewport = true

        val sayfalar = LinearLayout(this)
        sayfalar.orientation = LinearLayout.HORIZONTAL
        sayfalar.addView(uygulamaSayfasi(), LinearLayout.LayoutParams(gen, MATCH_PARENT))
        sayfalar.addView(ayarSayfasi(), LinearLayout.LayoutParams(gen, MATCH_PARENT))
        hsv.addView(sayfalar, FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT))

        for ((i, t) in sekmeT.withIndex()) {
            t.setOnClickListener { hsv.smoothScrollTo(i * gen, 0) }
        }

        var baslangic = 0
        hsv.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    baslangic = if (hsv.scrollX > gen / 2) 1 else 0
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = hsv.scrollX - baslangic * gen
                    val hedef = if (dx > gen * 0.15) 1 else if (dx < -gen * 0.15) 0 else baslangic
                    hsv.post { hsv.smoothScrollTo(hedef * gen, 0) }
                }
            }
            false
        }

        ana.addView(hsv, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        kok.addView(ana, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    // ---------- sayfa 1: uygulamalar ----------
    private fun uygulamaSayfasi(): View {
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        val ic = LinearLayout(this)
        ic.orientation = LinearLayout.VERTICAL
        ic.setPadding(dp(12), dp(16), dp(12), dp(32))
        sv.addView(ic, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val ust = LinearLayout(this)
        ust.orientation = LinearLayout.HORIZONTAL
        ust.setPadding(dp(4), 0, dp(4), dp(8))
        val ipucuT = tv("Kilitlemek istediklerini seç", 13f, Renk.SOLUK)
        val sayac = tv("${Depo.kilitli(this).size} seçili", 13f, Renk.ACIK, true)
        ust.addView(ipucuT, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        ust.addView(sayac)
        ic.addView(ust)

        val yukleniyor = tv("Uygulamalar yükleniyor…", 14f, Renk.SOLUK)
        yukleniyor.setPadding(dp(4), dp(24), 0, 0)
        ic.addView(yukleniyor)

        Thread {
            val pm = packageManager
            val niyet = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val coll = Collator.getInstance(Locale("tr", "TR"))
            val liste = pm.queryIntentActivities(niyet, 0)
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { Uyg(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
                .sortedWith { a, b -> coll.compare(a.ad, b.ad) }
            runOnUiThread {
                ic.removeView(yukleniyor)
                var i = 0
                while (i < liste.size) {
                    val satir = LinearLayout(this)
                    satir.orientation = LinearLayout.HORIZONTAL
                    for (j in 0..1) {
                        val lp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
                        if (i + j < liste.size) {
                            satir.addView(kart(liste[i + j], sayac), lp)
                        } else {
                            satir.addView(View(this), lp)
                        }
                    }
                    ic.addView(satir)
                    i += 2
                }
            }
        }.start()
        return sv
    }

    private fun kart(u: Uyg, sayac: TextView): View {
        val c = kutu()
        val ikon = ImageView(this)
        ikon.setImageDrawable(u.ikon)
        c.addView(ikon, LinearLayout.LayoutParams(dp(36), dp(36)))

        val ad = tv(u.ad, 14f, Renk.YAZI)
        ad.maxLines = 1
        ad.ellipsize = TextUtils.TruncateAt.END
        val alp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        alp.setMargins(dp(10), 0, dp(6), 0)
        c.addView(ad, alp)

        val tik = tv("", 13f, Renk.YAZI, true)
        tik.gravity = Gravity.CENTER
        c.addView(tik, LinearLayout.LayoutParams(dp(24), dp(24)))

        fun goster() {
            val sec = Depo.kilitli(this).contains(u.paket)
            c.background = yuvarlak(
                if (sec) Renk.SECILI else Renk.KART, 14f, this, if (sec) Renk.ACIK else 0
            )
            tik.text = if (sec) "✓" else ""
            tik.background = if (sec) daire(Renk.ACIK) else halka(dp(2), Renk.SOLUK)
            sayac.text = "${Depo.kilitli(this).size} seçili"
        }
        goster()
        c.setOnClickListener {
            Depo.kilitliDegistir(this, u.paket)
            goster()
        }
        return c
    }

    // ---------- sayfa 2: ayarlar ----------
    private fun servisAcikMi(): Boolean {
        if (AppLockService.ornek != null) return true
        val ayar = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, AppLockService::class.java)
        return ayar.contains(cn.flattenToString()) || ayar.contains(cn.flattenToShortString())
    }

    private fun durumGuncelle() {
        val acik = servisAcikMi()
        durumYazi?.text = if (acik) "Çalışıyor ✓" else "Kapalı, kilit çalışmaz"
        durumYazi?.setTextColor(if (acik) Renk.YESIL else Renk.HATA)
        durumDugme?.visibility = if (acik) View.GONE else View.VISIBLE
    }

    private fun ayarSayfasi(): View {
        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        val ic = LinearLayout(this)
        ic.orientation = LinearLayout.VERTICAL
        ic.setPadding(dp(12), dp(4), dp(12), dp(32))
        sv.addView(ic, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // servis durumu
        ic.addView(bolum("KİLİT SERVİSİ"))
        val durumKart = kutu()
        val kolon = LinearLayout(this)
        kolon.orientation = LinearLayout.VERTICAL
        kolon.addView(tv("Erişilebilirlik servisi", 15f, Renk.YAZI))
        val k2 = tv("", 13f, Renk.SOLUK)
        kolon.addView(k2)
        durumKart.addView(kolon, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val dg = tv("Aç", 14f, Renk.YAZI, true)
        dg.gravity = Gravity.CENTER
        dg.setPadding(dp(18), dp(8), dp(18), dp(8))
        dg.background = yuvarlak(Renk.ACIK, 16f, this)
        dg.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        durumKart.addView(dg)
        ic.addView(durumKart, kartLp())
        durumYazi = k2
        durumDugme = dg
        durumGuncelle()
        ic.addView(
            ipucu(
                "Kapalıysa: Aç → Yüklü uygulamalar → Kilit → aç. " +
                    "Düğme gri ise önce telefon Ayarlar → Uygulamalar → Kilit → ⋮ → " +
                    "\"Kısıtlı ayarlara izin ver\" yap."
            )
        )

        // süre
        ic.addView(bolum("YENİDEN KİLİTLEME SÜRESİ"))
        ic.addView(ipucu("Çıkıştan sonra bu süre dolana kadar PIN sorma"))
        val sureler = listOf(
            "Hemen" to 0, "10 sn" to 10, "20 sn" to 20,
            "30 sn" to 30, "1 dk" to 60, "5 dk" to 300
        )
        val cipler = ArrayList<Pair<TextView, Int>>()

        fun cipRenk() {
            val s = Depo.gecikmeSn(this)
            for ((t, sn) in cipler) {
                val sec = sn == s
                t.typeface = if (sec) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                t.background = yuvarlak(if (sec) Renk.ACIK else Renk.KART, 12f, this)
            }
        }

        for (r in 0..1) {
            val satir = LinearLayout(this)
            satir.orientation = LinearLayout.HORIZONTAL
            for (k in 0..2) {
                val (ad, sn) = sureler[r * 3 + k]
                val t = tv(ad, 14f, Renk.YAZI)
                t.gravity = Gravity.CENTER
                t.setPadding(0, dp(14), 0, dp(14))
                t.setOnClickListener {
                    Depo.gecikmeAyarla(this, sn)
                    cipRenk()
                }
                cipler.add(Pair(t, sn))
                val lp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                lp.setMargins(dp(4), dp(4), dp(4), dp(4))
                satir.addView(t, lp)
            }
            ic.addView(satir)
        }
        cipRenk()

        // güvenlik
        ic.addView(bolum("GÜVENLİK"))
        ic.addView(
            anahtarSatiri(
                "Karışık tuş sırası",
                "Tuşların yeri her girişte değişir",
                Depo.karisik(this)
            ) { Depo.karisikAyarla(this, it) },
            kartLp()
        )
        ic.addView(
            dugme("PIN değiştir", Renk.KART) {
                gosterilen = "kur"
                kok.removeAllViews()
                pinKur(true)
            },
            kartLp()
        )
        ic.addView(
            dugme("Şimdi kilitle", Renk.ACIK) {
                AppLockService.ornek?.hepsiniKilitle()
                Depo.sonAktifAyarla(this, 0L)
                oturumAcik = false
                ekranGoster()
            },
            kartLp()
        )
        ic.addView(ipucu("İpucu: Telefonun \"Ayarlar\" uygulamasını da listeden seçersen, servisi PIN'siz kapatamazlar."))
        return sv
    }

    private fun anahtarSatiri(
        baslik: String,
        aciklama: String,
        ilk: Boolean,
        degisti: (Boolean) -> Unit
    ): View {
        val satir = kutu()
        val metin = LinearLayout(this)
        metin.orientation = LinearLayout.VERTICAL
        metin.addView(tv(baslik, 15f, Renk.YAZI))
        metin.addView(tv(aciklama, 12f, Renk.SOLUK))
        satir.addView(metin, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        var acik = ilk
        val pil = tv("", 13f, Renk.YAZI, true)
        pil.gravity = Gravity.CENTER
        pil.setPadding(dp(14), dp(6), dp(14), dp(6))

        fun goster() {
            pil.text = if (acik) "Açık" else "Kapalı"
            pil.background = yuvarlak(if (acik) Renk.ACIK else Renk.BASILI, 16f, this)
        }
        goster()
        satir.setOnClickListener {
            acik = !acik
            goster()
            degisti(acik)
        }
        satir.addView(pil)
        return satir
    }

    private fun dugme(metin: String, renk: Int, tikla: () -> Unit): View {
        val v = kutu(renk)
        v.setPadding(dp(14), dp(16), dp(14), dp(16))
        v.addView(tv(metin, 15f, Renk.YAZI, true), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        v.addView(tv("›", 20f, Renk.YAZI))
        v.setOnClickListener { tikla() }
        return v
    }
}
