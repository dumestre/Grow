package com.daime.grow.ui.screen.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class StoreCategory(val name: String, val icon: ImageVector)
data class StoreProduct(val name: String, val price: Double, val category: String, val rating: String)
data class CartItem(val product: StoreProduct, val quantity: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    innerPadding: PaddingValues,
    useAlternativeIcons: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isTablet = screenWidthDp >= 600.dp
    
    val cartItems = remember { mutableStateListOf<CartItem>() }
    val cartCount = cartItems.sumOf { it.quantity }

    // DEFINIÇÃO DAS CATEGORIAS (REAL vs ALTERNATIVA)
    val categories = if (useAlternativeIcons) {
        listOf(
            StoreCategory("Sementes", Icons.Default.Spa),
            StoreCategory("Substratos", Icons.Default.Build),
            StoreCategory("Nutrientes", Icons.Default.WaterDrop),
            StoreCategory("Acessórios", Icons.Default.Checklist),
            StoreCategory("Manuais", Icons.Default.ContentPaste)
        )
    } else {
        listOf(
            StoreCategory("Sedas", Icons.Default.Description),
            StoreCategory("Isqueiros", Icons.Default.LocalFireDepartment),
            StoreCategory("Piteiras", Icons.Default.FilterAlt),
            StoreCategory("Dichavadores", Icons.Default.Settings),
            StoreCategory("Boladores", Icons.Default.Build)
        )
    }

    // DEFINIÇÃO DOS PRODUTOS (REAL vs ALTERNATIVA)
    val products = if (useAlternativeIcons) {
        listOf(
            StoreProduct("Kit Sementes Orgânicas", 25.50, "Sementes", "4.9"),
            StoreProduct("Substrato Premium 5L", 38.00, "Substratos", "4.7"),
            StoreProduct("Fertilizante NPK 10-10-10", 14.00, "Nutrientes", "4.5"),
            StoreProduct("Tesoura de Poda Curva", 45.00, "Acessórios", "4.8"),
            StoreProduct("Etiquetas de Identificação", 12.00, "Acessórios", "4.6"),
            StoreProduct("Guia Prático de Cultivo", 15.00, "Manuais", "4.9")
        )
    } else {
        listOf(
            StoreProduct("Seda King Size Slim", 5.50, "Sedas", "4.9"),
            StoreProduct("Isqueiro Recarregável", 8.00, "Isqueiros", "4.7"),
            StoreProduct("Piteira de Papel", 4.00, "Piteiras", "4.5"),
            StoreProduct("Dichavador Metal 3 Fases", 45.00, "Dichavadores", "4.8"),
            StoreProduct("Bolador Automático 110mm", 25.00, "Boladores", "4.6"),
            StoreProduct("Seda Brown Orgânica", 6.00, "Sedas", "4.9")
        )
    }

    Scaffold(
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { /* Implementar navegação para carrinho */ },
                containerColor = Color(0xFF121212),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = if (isTablet) 16.dp else 84.dp)
            ) {
                BadgedBox(
                    badge = { 
                        if (cartCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = Color(0xFF1B5E20),
                                modifier = Modifier.size(16.dp)
                            ) { 
                                Text(
                                    cartCount.toString(), 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.ExtraBold 
                                ) 
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ShoppingCart, 
                        contentDescription = "Carrinho",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet) 3 else 2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        "Loja", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("O que você procura hoje?", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        Column {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "OFERTA",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tudo o que você precisa\ncom os melhores preços",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 28.sp
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        CategoryItem(category)
                    }
                }
            }

            items(products) { product ->
                ProductCard(
                    product = product,
                    onAddToCart = { qty ->
                        val index = cartItems.indexOfFirst { it.product.name == product.name }
                        if (index != -1) {
                            cartItems[index] = cartItems[index].copy(quantity = cartItems[index].quantity + qty)
                        } else {
                            cartItems.add(CartItem(product, qty))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryItem(category: StoreCategory) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProductCard(
    product: StoreProduct, 
    modifier: Modifier = Modifier,
    onAddToCart: (Int) -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                
                Surface(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB300))
                        Spacer(Modifier.width(2.dp))
                        Text(product.rating, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "R$ ${String.format(Locale.getDefault(), "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Quantidade", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Text(
                            quantity.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onAddToCart(quantity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF121212).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "ADD AO CARRINHO", 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF121212)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { onAddToCart(quantity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF121212),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "COMPRAR", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
