package com.zomdroid.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zomdroid.ui.theme.ZomdroidTheme

@Composable
fun SettingsSection(
    title: String,
    headerContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            headerContent()
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Preview
@Composable
fun SettingsSectionPreview() {
    ZomdroidTheme {
        SettingsSection(title = "Example setting") {
            Text("Example Content")
        }
    }
}