package sau.odev.usagemonitor.appusagemanager.challenges

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ChallengeFinishBroadcast: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "On Receiver = ${intent?.getLongExtra("ID", -1L)}", Toast.LENGTH_SHORT).show()
    }
}