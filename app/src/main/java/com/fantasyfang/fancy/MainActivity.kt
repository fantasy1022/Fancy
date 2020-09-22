package com.fantasyfang.fancy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.fantasyfang.fancy.ui.song.SongListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()
        if (intent?.categories?.contains("android.shortcut.play.song") == true) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val songListFragment =
                navHostFragment.childFragmentManager.fragments[0] as? SongListFragment
            songListFragment?.playSongDirectly()
        }
    }
}