package com.appliedrec.credentials.app

import android.content.Context
import com.appliedrec.verid.core2.Classifier
import com.microblink.blinkid.entities.recognizers.blinkid.generic.BlinkIdMultiSideRecognizer
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Region
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Type
import java.io.File

object AuthenticityScoreSupport {

    private val supportedDocuments: Map<Region,Array<Type>> = mapOf(
        Region.ALBERTA to arrayOf(Type.DL, Type.ID),
        Region.BRITISH_COLUMBIA to arrayOf(Type.DL, Type.ID, Type.DL_PUBLIC_SERVICES_CARD, Type.PUBLIC_SERVICES_CARD),
        Region.MANITOBA to arrayOf(Type.DL, Type.ID),
        Region.NEW_BRUNSWICK to arrayOf(Type.DL),
        Region.NEWFOUNDLAND_AND_LABRADOR to arrayOf(Type.DL),
        Region.NOVA_SCOTIA to arrayOf(Type.DL, Type.ID),
        Region.ONTARIO to arrayOf(Type.DL, Type.ID),
        Region.QUEBEC to arrayOf(Type.DL),
        Region.SASKATCHEWAN to arrayOf(Type.DL, Type.ID),
        Region.YUKON to arrayOf(Type.DL)
    )

    fun isDocumentSupported(result: BlinkIdMultiSideRecognizer.Result): Boolean {
        val region = result.classInfo.region
        val type = result.classInfo.type
        val supportedTypes = supportedDocuments[region]
        return supportedTypes?.contains(type) ?: false
    }

    fun getClassifiers(context: Context): Array<Classifier> {
        var cachedModelFiles = getCachedModelFiles(context)
        if (cachedModelFiles.isEmpty()) {
            val modelAssets = context.assets.list("")
                ?.filter { name -> (name.startsWith("licence", true) || name.startsWith("license", true)) && name.endsWith("nv", true) }
                ?.toTypedArray() ?: emptyArray()
            for (modelAsset in modelAssets) {
                context.assets.open(modelAsset).use { input ->
                    File(context.cacheDir, modelAsset).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cachedModelFiles = getCachedModelFiles(context)
        }
        var index = 1
        return cachedModelFiles.map { file -> Classifier("licence${index++.toString().padStart(2, '0')}", file.path) }.toTypedArray()
    }

    private fun getCachedModelFiles(context: Context): Array<File> {
        return context.cacheDir.list { dir, name -> (name.startsWith("licence", true) || name.startsWith("license", true)) && name.endsWith("nv", true) }
            ?.map { fileName -> File(context.cacheDir, fileName) }
            ?.toTypedArray()
            ?: emptyArray()
    }
}