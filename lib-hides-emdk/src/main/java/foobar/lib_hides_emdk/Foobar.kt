package foobar.lib_hides_emdk

import android.content.Context
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.BarcodeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//use api without exposing it; returning value, so it cannot be elided
suspend fun getEmdkString(appCtx: Context): String {
    val emdk = obtainEmdkUnsafe(appCtx)
    val bcmgr = emdk.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
    val s = bcmgr.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
    return s.scannerInfo.friendlyName
}

internal suspend fun obtainEmdkUnsafe(appCtx: Context): EMDKManager = withContext(Dispatchers.IO) {

    suspendCoroutine<EMDKManager> { ct ->
        val results = EMDKManager.getEMDKManager(appCtx, object : EMDKManager.EMDKListener {
            override fun onOpened(m: EMDKManager) {
                ct.resume(m)
            }

            override fun onClosed() {
                ct.context.cancel(EmdkClosedException())
            }
        })

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            throw IllegalStateException("EMDKManager Request Failed; ${results.statusCode}:${results.extendedStatusCode}")
        }
    }
}

class EmdkClosedException : CancellationException("EmdkClosed")