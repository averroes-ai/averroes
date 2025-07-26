package com.rizilab.fiqhadvisor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rizilab.fiqhadvisor.ui.theme.FiqhColors
import com.rizilab.fiqhadvisor.ui.theme.FiqhTypography

@Composable
fun FiqhCard(
        modifier: Modifier = Modifier,
        backgroundColor: Color = FiqhColors.Surface,
        elevation: Dp = 4.dp,
        content: @Composable () -> Unit
) {
    Surface(
            modifier = modifier,
            color = backgroundColor,
            shadowElevation = elevation,
            shape = RoundedCornerShape(12.dp)
    ) { content() }
}

@Composable
fun FiqhButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        backgroundColor: Color = FiqhColors.Primary,
        contentColor: Color = FiqhColors.OnPrimary,
        content: @Composable RowScope.() -> Unit
) {
    Surface(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            enabled = enabled,
            color = if (enabled) backgroundColor else FiqhColors.NeutralGray,
            contentColor = contentColor,
            shape = RoundedCornerShape(8.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
        )
    }
}

@Composable
fun ComplianceIndicator(isHalal: Boolean, confidence: Double, modifier: Modifier = Modifier) {
    FiqhCard(
            modifier = modifier,
            backgroundColor = if (isHalal) FiqhColors.HalalGreen else FiqhColors.HaramRed
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = if (isHalal) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                        text = if (isHalal) "HALAL" else "HARAM",
                        style = FiqhTypography.Heading2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "Confidence: ${(confidence * 100).toInt()}%",
                        style = FiqhTypography.Body2,
                        color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AnalysisResultCard(
        analysis: com.rizilab.fiqhadvisor.core.QueryResponse,
        onViewDetails: () -> Unit,
        modifier: Modifier = Modifier
) {
    FiqhCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Extract halal/haram status from response
            val isHalal = analysis.response.contains("HALAL", ignoreCase = true)

            ComplianceIndicator(
                    isHalal = isHalal,
                    confidence = analysis.confidence,
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "Analysis Result",
                    style = FiqhTypography.Heading2,
                    color = FiqhColors.OnSurface,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = analysis.response,
                    style = FiqhTypography.Body1,
                    color = FiqhColors.OnSurface
            )

            if (analysis.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "Islamic References",
                        style = FiqhTypography.Body2,
                        color = FiqhColors.OnSurface,
                        fontWeight = FontWeight.Bold
                )

                analysis.sources.forEach { source ->
                    Text(
                            text = "• $source",
                            style = FiqhTypography.Caption,
                            color = FiqhColors.OnSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FiqhButton(onClick = onViewDetails, modifier = Modifier.fillMaxWidth()) {
                Text("View Details")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
