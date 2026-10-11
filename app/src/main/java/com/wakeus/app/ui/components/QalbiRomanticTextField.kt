package com.wakeus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeus.app.R

private val RomanticFieldBg          = Color(0xFFFFF7F4)
private val RomanticFieldBorder      = Color(0xFFF3C2C9)
private val RomanticFieldBorderFocus = Color(0xFF80243C)
private val RomanticFieldBorderError = Color(0xFFC73650)
private val RomanticFieldDivider     = Color(0xFFF0CCD2)
private val RomanticFieldIconTint    = Color(0xFF80243C)
private val RomanticFieldText        = Color(0xFF382329)
private val RomanticFieldHint        = Color(0xFF94727A)
private val RomanticFieldError       = Color(0xFFB7233E)

/**
 * Romantic-themed text field matching QALBI's Login and Registration reference design.
 * Features rounded corners, subtle rose border, tinted leading icon, vertical separator,
 * and clean single-line text entry with error and password visibility support.
 */
@Composable
fun QalbiRomanticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingVector: ImageVector? = null,
    leadingPainter: Painter? = null,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        error != null -> RomanticFieldBorderError
        isFocused     -> RomanticFieldBorderFocus
        else          -> RomanticFieldBorder
    }

    val borderWidth = if (isFocused || error != null) 1.5.dp else 1.2.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(RomanticFieldBg, RoundedCornerShape(16.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading Icon
            if (leadingVector != null) {
                Icon(
                    imageVector = leadingVector,
                    contentDescription = null,
                    tint = RomanticFieldIconTint,
                    modifier = Modifier.size(22.dp),
                )
            } else if (leadingPainter != null) {
                Icon(
                    painter = leadingPainter,
                    contentDescription = null,
                    tint = RomanticFieldIconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Subtle vertical separator line between leading icon and input
            if (leadingVector != null || leadingPainter != null) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(RomanticFieldDivider)
                )
                Spacer(Modifier.width(12.dp))
            }

            // Input field
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = RomanticFieldHint,
                        ),
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = RomanticFieldText,
                    ),
                    cursorBrush = SolidColor(RomanticFieldBorderFocus),
                    visualTransformation = if (isPassword && !showPassword)
                        PasswordVisualTransformation()
                    else
                        VisualTransformation.None,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                )
            }

            // Trailing Icon (Password Visibility Toggle)
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                                      else Icons.Outlined.Visibility,
                        contentDescription = stringResource(R.string.cd_password_toggle),
                        tint = RomanticFieldIconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Error message below field
        if (error != null) {
            Text(
                text = error,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = RomanticFieldError,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
