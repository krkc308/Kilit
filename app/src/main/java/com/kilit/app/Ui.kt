package com.kilit.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object Renk {
    val BG = Color.parseColor("#0E1014")
    val KART = Color.parseColor("#171B22")
    val SECILI = Color.parseColor("#17233D")
    val TUS = Color.parseColor("#1B1F27")
    val BASILI = Color.parseColor("#2C3340")
    val ACIK = Color.parseColor("#4F8CFF")
    val HATA = Color.parseColor("#FF5C6C")
    val YESIL = Color.parseColor("#3DDC97")
    val YAZI = Color.WHITE
    val SOLUK = Color.parseColor("#7D8594")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun yuvarlak(renk: Int, yaricapDp: Float, ctx: Context, cerceve: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(renk)
    g.cornerRadius = yaricapDp * ctx.resources.displayMetrics.density
    if (cerceve != 0) g.setStroke(ctx.dp(2), cerceve)
    return g
}

fun daire(renk: Int): GradientDrawable {
    val g = GradientDrawable()
    g.shape = GradientDrawable.OVAL
    g.setColor(renk)
    return g
}

fun halka(kalinlikPx: Int, renk: Int): GradientDrawable {
    val g = GradientDrawable()
    g.shape = GradientDrawable.OVAL
    g.setColor(Color.TRANSPARENT)
    g.setStroke(kalinlikPx, renk)
    return g
}
