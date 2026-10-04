package com.kilit.app

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Depo {
    const val PIN_UZUNLUK = 4
    const val MAX_DENEME = 5
    const val BEKLEME_MS = 30_000L
    private const val ITER = 10_000
    private var deneme = 0

    private fun p(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("kilit", Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITER, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexten(s: String): ByteArray =
        ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun pinVar(c: Context): Boolean = (p(c).getString("hash", "") ?: "").isNotEmpty()

    fun pinKaydet(c: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        p(c).edit()
            .putString("salt", salt.joinToString("") { "%02x".format(it) })
            .putString("hash", hash(pin, salt))
            .putLong("bekleme_bitis", 0L)
            .apply()
    }

    fun pinDogru(c: Context, pin: String): Boolean {
        val s = p(c)
        val tuz = s.getString("salt", "") ?: ""
        val h = s.getString("hash", "") ?: ""
        if (tuz.isEmpty() || h.isEmpty()) return false
        return hash(pin, hexten(tuz)) == h
    }

    fun kilitli(c: Context): Set<String> =
        (p(c).getStringSet("kilitli", emptySet()) ?: emptySet()).toSet()

    fun kilitliDegistir(c: Context, paket: String) {
        val yeni = kilitli(c).toMutableSet()
        if (!yeni.add(paket)) yeni.remove(paket)
        p(c).edit().putStringSet("kilitli", yeni).apply()
    }

    fun gecikmeSn(c: Context): Int = p(c).getInt("gecikme", 10)
    fun gecikmeAyarla(c: Context, sn: Int) = p(c).edit().putInt("gecikme", sn).apply()

    fun karisik(c: Context): Boolean = p(c).getBoolean("karisik", false)
    fun karisikAyarla(c: Context, v: Boolean) = p(c).edit().putBoolean("karisik", v).apply()

    fun sonAktif(c: Context): Long = p(c).getLong("son_aktif", 0L)
    fun sonAktifAyarla(c: Context, t: Long) = p(c).edit().putLong("son_aktif", t).apply()

    fun beklemeKalanMs(c: Context): Long =
        maxOf(0L, p(c).getLong("bekleme_bitis", 0L) - System.currentTimeMillis())

    fun yanlisGirildi(c: Context): Int {
        deneme++
        if (deneme >= MAX_DENEME) {
            deneme = 0
            p(c).edit()
                .putLong("bekleme_bitis", System.currentTimeMillis() + BEKLEME_MS)
                .apply()
            return 0
        }
        return MAX_DENEME - deneme
    }

    fun dogruGirildi() {
        deneme = 0
    }
}
