package com.kilit.app

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class PinView(ctx: Context) : LinearLayout(ctx) {

    var onTamam: (String) -> Unit = {}
    var beklemeKontrol = false

    private val ikon = ImageView(ctx)
    private val baslik = TextView(ctx)
    private val noktalar = LinearLayout(ctx)
    private val mesaj = TextView(ctx)
    private val tuslar = LinearLayout(ctx)
    private val iptal = TextView(ctx)
    private val nokta = ArrayList<View>()
    private val h = Handler(Looper.getMainLooper())
    private var pin = ""
    private var engel = false
    private val tusBoy = minOf(
        (ctx.resources.displayMetrics.widthPixels * 0.2f).toInt(),
        (ctx.resources.displayMetrics.heightPixels * 0.105f).toInt()
    )

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Renk.BG)
        setPadding(0, ctx.dp(56), 0, ctx.dp(16))

        ikon.visibility = View.GONE
        val ilp = LinearLayout.LayoutParams(ctx.dp(56), ctx.dp(56))
        ilp.bottomMargin = ctx.dp(12)
        addView(ikon, ilp)

        baslik.textSize = 22f
        baslik.setTextColor(Renk.YAZI)
        baslik.typeface = Typeface.DEFAULT_BOLD
        baslik.gravity = Gravity.CENTER
        addView(baslik, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        noktalar.orientation = LinearLayout.HORIZONTAL
        noktalar.gravity = Gravity.CENTER
        repeat(Depo.PIN_UZUNLUK) {
            val d = View(ctx)
            val lp = LinearLayout.LayoutParams(ctx.dp(14), ctx.dp(14))
            lp.setMargins(ctx.dp(10), 0, ctx.dp(10), 0)
            noktalar.addView(d, lp)
            nokta.add(d)
        }
        val nlp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        nlp.topMargin = ctx.dp(28)
        addView(noktalar, nlp)

        mesaj.textSize = 14f
        mesaj.setTextColor(Renk.HATA)
        mesaj.gravity = Gravity.CENTER
        val mlp = LinearLayout.LayoutParams(MATCH_PARENT, ctx.dp(28))
        mlp.topMargin = ctx.dp(12)
        addView(mesaj, mlp)

        tuslar.orientation = LinearLayout.VERTICAL
        val tlp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        tlp.topMargin = ctx.dp(8)
        addView(tuslar, tlp)

        iptal.text = "İptal"
        iptal.textSize = 15f
        iptal.setTextColor(Renk.SOLUK)
        iptal.gravity = Gravity.CENTER
        iptal.setPadding(ctx.dp(24), ctx.dp(12), ctx.dp(24), ctx.dp(12))
        iptal.visibility = View.GONE
        val plp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        plp.topMargin = ctx.dp(8)
        addView(iptal, plp)

        tusYenile()
        guncelle()
    }

    fun baslikAyarla(t: String) {
        baslik.text = t
    }

    fun ikonAyarla(d: Drawable?) {
        if (d != null) {
            ikon.setImageDrawable(d)
            ikon.visibility = View.VISIBLE
        }
    }

    fun iptalGoster(f: () -> Unit) {
        iptal.visibility = View.VISIBLE
        iptal.setOnClickListener { f() }
    }

    fun sifirla() {
        pin = ""
        engel = false
        mesaj.text = ""
        guncelle()
        if (Depo.karisik(context)) tusYenile()
    }

    fun hata(metin: String) {
        mesaj.text = metin
        for (d in nokta) d.background = daire(Renk.HATA)
        noktalar.animate().translationX(24f).setDuration(40).withEndAction {
            noktalar.animate().translationX(-24f).setDuration(80).withEndAction {
                noktalar.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
        h.postDelayed({
            sifirla()
            mesaj.text = metin
        }, 350)
    }

    private fun guncelle() {
        for ((i, d) in nokta.withIndex()) {
            d.background = if (i < pin.length) daire(Renk.ACIK) else halka(context.dp(2), Renk.SOLUK)
        }
    }

    private fun tusYenile() {
        tuslar.removeAllViews()
        val c = context
        val dizi = if (Depo.karisik(c)) {
            (0..9).map { it.toString() }.shuffled()
        } else {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        }
        val m = c.dp(8)

        fun satir(vararg hucre: View): LinearLayout {
            val s = LinearLayout(c)
            s.orientation = LinearLayout.HORIZONTAL
            for (v in hucre) {
                val lp = LinearLayout.LayoutParams(tusBoy, tusBoy)
                lp.setMargins(m, m, m, m)
                s.addView(v, lp)
            }
            return s
        }

        for (r in 0..2) {
            tuslar.addView(satir(tus(dizi[r * 3]), tus(dizi[r * 3 + 1]), tus(dizi[r * 3 + 2])))
        }
        tuslar.addView(satir(View(c), tus(dizi[9]), tus("⌫")))
    }

    private fun tus(ad: String): View {
        val t = TextView(context)
        t.text = ad
        t.textSize = if (ad == "⌫") 24f else 28f
        t.setTextColor(Renk.YAZI)
        t.gravity = Gravity.CENTER
        t.background = daire(Renk.TUS)
        t.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.background = daire(Renk.BASILI)
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    bas(ad)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = daire(Renk.TUS)
                }
            }
            true
        }
        return t
    }

    private fun bas(ad: String) {
        if (engel) return
        if (beklemeKontrol) {
            val kalan = Depo.beklemeKalanMs(context)
            if (kalan > 0) {
                mesaj.text = "${(kalan + 999) / 1000} sn bekle"
                return
            }
        }
        mesaj.text = ""
        if (ad == "⌫") {
            pin = pin.dropLast(1)
        } else if (pin.length < Depo.PIN_UZUNLUK) {
            pin += ad
        }
        guncelle()
        if (pin.length == Depo.PIN_UZUNLUK) {
            engel = true
            val girilen = pin
            h.postDelayed({ onTamam(girilen) }, 10)
        }
    }
}
