package de.smartsquare.kickprotocol.sample

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import de.smartsquare.kickprotocol.nearby.NearbyManager

/**k
 * @author Ruben Gees
 */
class MainActivity : AppCompatActivity() {

    private val nearbyManager by lazy { NearbyManager(this) }

    override fun onStart() {
        super.onStart()

        RxPermissions(this)
            .requestEach(Manifest.permission.ACCESS_COARSE_LOCATION)
            .autoDisposable(this.scope())
            .subscribe {
                if (it.granted) {
                    nearbyManager
                        .advertise("sample")
                        .subscribe()
                } else {
                    finish()
                }
            }
    }

    override fun onStop() {
        nearbyManager.destroy()

        super.onStop()
    }
}
