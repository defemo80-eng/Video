package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DeiPlayApp
import com.example.ui.DeiPlayViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DeiPlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeiPlayApp(viewModel = viewModel)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val playerState = viewModel.playerState.value
        if (playerState.activeVideo != null && playerState.isPlaying) {
            viewModel.playerManager.enterSystemPip(this)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.playerManager.setSystemPipActive(isInPictureInPictureMode)
    }
}
