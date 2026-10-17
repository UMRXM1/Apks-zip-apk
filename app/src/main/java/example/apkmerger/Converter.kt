package com.example.apkmerger

import android.content.Context
import com.android.apksig.ApkSigner
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object Converter {
    private const val PASS = "android"

    /** 合并 bundle -> 单个 apk，并签名。返回签名后的文件。 */
    fun convert(ctx: Context, bundle: File): File {
        val merged = File(ctx.cacheDir, "merged.apk").also { it.delete() }
        merge(bundle, merged)
        val signed = File(ctx.cacheDir, "signed.apk").also { it.delete() }
        sign(ctx, merged, signed)
        return signed
    }

    // 调用 APKEditor 的入口：等同命令行 `m -i <input> -o <output>`
    private fun merge(input: File, output: File) {
        val args = arrayOf("m", "-i", input.absolutePath, "-o", output.absolutePath)
        try {
            Class.forName("com.reandroid.apkeditor.Main")
                .getMethod("main", Array<String>::class.java)
                .invoke(null, args)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("合并失败: " + (e.targetException?.message ?: e.toString()))
        }
        if (!output.exists() || output.length() == 0L) {
            throw RuntimeException("合并没有产出文件，可能是 bundle 格式不支持")
        }
    }

    private fun sign(ctx: Context, input: File, output: File) {
        val ks = KeyStore.getInstance("PKCS12")
        ctx.assets.open("sign.p12").use { ks.load(it, PASS.toCharArray()) }
        val alias = ks.aliases().nextElement()
        val key = ks.getKey(alias, PASS.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(alias) as X509Certificate
        val cfg = ApkSigner.SignerConfig.Builder("sign", key, listOf(cert)).build()
        ApkSigner.Builder(listOf(cfg))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }
}
