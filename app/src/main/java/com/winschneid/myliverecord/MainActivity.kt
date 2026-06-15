package com.winschneid.myliverecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.winschneid.myliverecord.ui.navigation.NavGraph
import com.winschneid.myliverecord.ui.theme.MyLiveRecordTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyLiveRecordTheme {
                NavGraph()
            }
        }
    }
}
