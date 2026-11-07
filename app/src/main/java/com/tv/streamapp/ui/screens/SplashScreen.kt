package com.tv.streamapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tv.streamapp.R
import com.tv.streamapp.ui.components.parseColorOrDefault

@Composable
fun SplashScreen(
    appName: String,
    logoUrl: String?,
    primaryColorHex: String,
    modifier: Modifier = Modifier
) {
    val brand = parseColorOrDefault(primaryColorHex)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFF080A1C), Color(0xFF3C126E)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (logoUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(R.drawable.hocine_logo_white),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(200.dp)
                )
            } else {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    error = painterResource(R.drawable.hocine_logo_white),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(200.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = appName,
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.loading_playlist),
                color = brand,
                fontSize = 18.sp
            )
        }
    }
}
