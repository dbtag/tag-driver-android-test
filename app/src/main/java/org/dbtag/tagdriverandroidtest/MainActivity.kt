package org.dbtag.tagdriverandroidtest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.coroutines.experimental.launch
import org.dbtag.driver.TagUnsignedInClient
import org.dbtag.driver.count
import org.dbtag.driver.databases

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = TagUnsignedInClient("logicart.flowbook.com")
        launch() {

            val databases = client.databases()
            Log.i("databases", databases.toString())
        }

        Log.i("first", "!")

    }
}
