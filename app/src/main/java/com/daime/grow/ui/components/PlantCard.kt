package com.daime.grow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.daime.grow.R
import com.daime.grow.domain.model.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlantCard(
    plant: Plant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                if (plant.photoUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    AsyncImage(
                        model = plant.photoUri,
                        contentDescription = plant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(8.dp)
                ) {
                    BadgeText(plant.stage)
                }

                if (onDeleteClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f))
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.plant_delete_desc, plant.name),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = plant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${plant.days} dias de cultivo",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            MetaLine("Strain", plant.strain)
            MetaLine("Substrato", plant.medium)
            Text(
                text = stringResource(
                    R.string.plant_next_watering,
                    plant.nextWateringDate?.toDateLabel() ?: stringResource(R.string.plant_not_defined)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BadgeText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Long.toDateLabel(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))
}
