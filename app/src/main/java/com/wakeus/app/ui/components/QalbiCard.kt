package com.wakeus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeus.app.ui.theme.DividerColor

/**
 * Standard surface card used for sections and list items in QALBI.
 */
@Composable
fun QalbiCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    innerPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border    = BorderStroke(1.dp, DividerColor),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(innerPadding),
            content  = content,
        )
    }
}
