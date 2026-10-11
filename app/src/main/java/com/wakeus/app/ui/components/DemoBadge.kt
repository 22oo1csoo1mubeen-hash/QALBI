package com.wakeus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wakeus.app.ui.theme.SubtleGold
import com.wakeus.app.ui.theme.MidnightBlue

/**
 * A small, unobtrusive chip shown on screens that use demo/mock data.
 * Ensures the user always knows they are in a Phase 1 demonstration.
 */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Row(
        modifier          = modifier
            .clip(RoundedCornerShape(50))
            .background(SubtleGold.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Filled.Science,
            contentDescription = null,
            modifier           = Modifier.size(14.dp),
            tint               = SubtleGold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "Demo Mode — Phase 1",
            style = MaterialTheme.typography.labelSmall,
            color = SubtleGold,
        )
    }
}
