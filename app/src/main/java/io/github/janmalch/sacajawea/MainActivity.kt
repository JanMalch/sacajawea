package io.github.janmalch.sacajawea

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.github.janmalch.sacajawea.listening.ListenActivity
import io.github.janmalch.sacajawea.translating.TranslateActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        translate.setOnClickListener {
            startActivity(Intent(this, TranslateActivity::class.java))
        }

        listen.setOnClickListener {
            startActivity(Intent(this, ListenActivity::class.java))
        }
    }

}
