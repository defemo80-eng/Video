package com.example.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun DeiPlayLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    Image(
        painter = painterResource(id = R.drawable.deiplay_brand_logo),
        contentDescription = "Logotipo oficial de DeiPlay",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DeiRed.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
    )
}

@Composable
fun DeiPlayCoverBanner(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 200.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 10.dp,
            color = DeiBlack,
            modifier = Modifier
                .size(maxWidth)
                .aspectRatio(1f)
                .border(1.5.dp, DeiRed.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.deiplay_brand_logo),
                contentDescription = "Portada y logotipo oficial de DeiPlay",
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = "El mundo entero del video en un solo lugar",
            style = MaterialTheme.typography.labelMedium,
            color = DeiWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun DeiPlayWordmark(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    showSlogan: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeiPlayLogoMark(size = iconSize)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append("Dei")
                    }
                    withStyle(SpanStyle(color = DeiRed)) {
                        append("Play")
                    }
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (showSlogan) {
                Text(
                    text = "El mundo entero del video en un solo lugar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
