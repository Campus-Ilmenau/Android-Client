package de.kaigothe.campusilmenau.core.device

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Was dieses Gerät kann. Heute nur NFC (Pendant zu `NFCVerfuegbarkeit` in der iOS-App). */
@Singleton
class DeviceCapabilities @Inject constructor(@ApplicationContext context: Context) {

    /** Das Gerät hat NFC-Hardware. Ohne sie entfällt der Thoska-Tab. */
    val hasNfc: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
}
