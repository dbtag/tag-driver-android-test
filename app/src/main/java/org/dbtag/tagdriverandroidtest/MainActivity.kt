package org.dbtag.tagdriverandroidtest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.dbtag.data.ServerDatabases
import org.dbtag.driver.TAndMs
import org.dbtag.driver.asyncDatabases0
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = org.dbtag.driver.TagUnsignedInClient("logicart.flowbook.com")
//        launch(UI) {
        val databases = client.asyncDatabases0(object: Continuation<TAndMs<ServerDatabases>> {
            override val context = EmptyCoroutineContext
            override fun resume(value: TAndMs<ServerDatabases>) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun resumeWithException(exception: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
        //      }

    }
}
