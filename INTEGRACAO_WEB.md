# 📱➕🖥️ Guia de Integração: App Android + Painel Web

## Visão Geral da Arquitetura

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   App Android   │         │    Supabase      │         │   Painel Web    │
│   (Grow App)    │◄───────►│  (Backend/Banco) │◄───────►│  (Admin/Loja)   │
│                 │         │                  │         │                 │
│ - Plantas       │         │ - PostgreSQL     │         │ - Vendas        │
│ - Cultivo       │         │ - Auth           │         │ - Produtos      │
│ - Mural         │         │ - Storage        │         │ - Banners       │
│ - Loja (novo)   │         │ - Realtime       │         │ - Relatórios    │
└─────────────────┘         └──────────────────┘         └─────────────────┘
```

---

## 📋 Índice

1. [Configuração do Supabase](#1-configuração-do-supabase)
2. [Estrutura do Banco de Dados](#2-estrutura-do-banco-de-dados)
3. [APIs e Endpoints](#3-apis-e-endpoints)
4. [Integração no App Android](#4-integração-no-app-android)
5. [Painel Web (React/Next.js)](#5-painel-web-reactnextjs)
6. [Segurança e RLS](#6-segurança-e-rls)
7. [Deploy e Publicação](#7-deploy-e-publicação)

---

## 1. Configuração do Supabase

### 1.1 Criar Projeto

1. Acesse https://supabase.com
2. Clique em **"New Project"**
3. Preencha:
   - **Name:** `grow-admin`
   - **Database Password:** (guarde em local seguro)
   - **Region:** Escolha a mais próxima (US East para Brasil)

### 1.2 Obter Credenciais

Após criar o projeto:

1. Vá em **Settings > API**
2. Copie:
   - **Project URL:** `https://xxxxx.supabase.co`
   - **Anon/Public Key:** `eyJhbG...`
   - **Service Role Key:** (use apenas no backend/painel)

### 1.3 Atualizar App Android

Edite `local.properties` na raiz do projeto:

```properties
SUPABASE_URL=https://seu-project.supabase.co
SUPABASE_ANON_KEY=sua-chave-anon
```

---

## 2. Estrutura do Banco de Dados

### 2.1 Tabelas Principais

Execute no **SQL Editor** do Supabase:

```sql
-- ========================================
-- 1. TABELA DE PRODUTOS (LOJA)
-- ========================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    stock INTEGER DEFAULT 0,
    image_url TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- 2. TABELA DE VENDAS/PEDIDOS
-- ========================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(20),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending', -- pending, paid, shipped, delivered, cancelled
    payment_method VARCHAR(50),
    payment_status VARCHAR(50) DEFAULT 'unpaid',
    shipping_address TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- 3. ITENS DO PEDIDO
-- ========================================
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

-- ========================================
-- 4. BANNERS PROMOCIONAIS
-- ========================================
CREATE TABLE banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    image_url TEXT NOT NULL,
    link_url TEXT,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- 5. CUPONS DE DESCONTO
-- ========================================
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) DEFAULT 'percentage', -- percentage, fixed
    discount_value DECIMAL(10,2) NOT NULL,
    min_purchase DECIMAL(10,2) DEFAULT 0,
    max_uses INTEGER,
    used_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- 6. USUÁRIOS DO PAINEL (ADMINS)
-- ========================================
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT auth.uid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) DEFAULT 'admin', -- admin, manager, viewer
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- 7. CONFIGURAÇÕES DA LOJA
-- ========================================
CREATE TABLE store_settings (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(100) UNIQUE NOT NULL,
    value TEXT,
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================
-- ÍNDICES PARA PERFORMANCE
-- ========================================
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_banners_active ON banners(is_active);
CREATE INDEX idx_coupons_code ON coupons(code);
```

### 2.2 Views para Relatórios

```sql
-- View: Resumo de Vendas por Dia
CREATE VIEW daily_sales_summary AS
SELECT 
    DATE(created_at) as sale_date,
    COUNT(*) as total_orders,
    SUM(total_amount) as total_revenue,
    AVG(total_amount) as avg_ticket
FROM orders
WHERE status != 'cancelled'
GROUP BY DATE(created_at)
ORDER BY sale_date DESC;

-- View: Produtos Mais Vendidos
CREATE VIEW top_products AS
SELECT 
    p.id,
    p.name,
    p.category,
    SUM(oi.quantity) as total_sold,
    SUM(oi.subtotal) as total_revenue
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status != 'cancelled'
GROUP BY p.id, p.name, p.category
ORDER BY total_sold DESC
LIMIT 100;

-- View: Status de Pedidos
CREATE VIEW orders_status_summary AS
SELECT 
    status,
    COUNT(*) as count,
    SUM(total_amount) as total_value
FROM orders
GROUP BY status;
```

---

## 3. APIs e Endpoints

### 3.1 Estrutura de API com Supabase

O Supabase já fornece API REST automática. Exemplos:

```typescript
// URL base
const API_URL = 'https://seu-project.supabase.co/rest/v1'
const HEADERS = {
    'apikey': 'SUA_ANON_KEY',
    'Authorization': 'Bearer SUA_ANON_KEY',
    'Content-Type': 'application/json'
}
```

### 3.2 Endpoints Principais

#### Produtos

```typescript
// Listar produtos ativos
GET /rest/v1/products?is_active=eq.true&order=created_at.desc

// Criar produto
POST /rest/v1/products
{
    "name": "Nutriente Grow A+B",
    "description": "Conjunto completo...",
    "price": 89.90,
    "category": "nutrientes",
    "stock": 50,
    "image_url": "https://...",
    "is_active": true
}

// Atualizar produto
PATCH /rest/v1/products?id=eq.1
{
    "price": 79.90,
    "stock": 45
}

// Deletar produto (soft delete)
PATCH /rest/v1/products?id=eq.1
{
    "is_active": false
}
```

#### Pedidos

```typescript
// Listar pedidos
GET /rest/v1/orders?order=created_at.desc
GET /rest/v1/orders?status=eq.pending

// Criar pedido
POST /rest/v1/orders
{
    "customer_name": "João Silva",
    "customer_email": "joao@email.com",
    "customer_phone": "11999999999",
    "total_amount": 199.80,
    "status": "pending",
    "payment_method": "pix",
    "shipping_address": "Rua X, 123..."
}

// Atualizar status
PATCH /rest/v1/orders?id=eq.1
{
    "status": "shipped",
    "payment_status": "paid"
}
```

#### Banners

```typescript
// Listar banners ativos
GET /rest/v1/banners?is_active=eq.true&order=display_order.asc

// Criar banner
POST /rest/v1/banners
{
    "title": "Promoção Verão",
    "image_url": "https://storage.../banner-summer.jpg",
    "link_url": "/promo/summer",
    "is_active": true,
    "display_order": 1,
    "start_date": "2026-12-01T00:00:00Z",
    "end_date": "2026-02-28T23:59:59Z"
}
```

#### Cupons

```typescript
// Validar cupom
GET /rest/v1/coupons?code=eq.PROMO10&is_active=eq.true

// Usar cupom
POST /rpc/increment_coupon_use
{
    "coupon_code": "PROMO10"
}
```

---

## 4. Integração no App Android

### 4.1 Atualizar Dependências

No `app/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing deps ...
    
    // Supabase (já deve estar)
    implementation(libs.supabase.kt.android)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
}
```

### 4.2 Criar Modelos de Dados

Crie em `app/src/main/java/com/daime/grow/data/remote/model/`:

```kotlin
// ProductDto.kt
package com.daime.grow.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("price") val price: Double,
    @SerialName("category") val category: String? = null,
    @SerialName("stock") val stock: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

// OrderDto.kt
@Serializable
data class OrderDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("status") val status: String = "pending",
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_status") val paymentStatus: String = "unpaid",
    @SerialName("shipping_address") val shippingAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// BannerDto.kt
@Serializable
data class BannerDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("title") val title: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("link_url") val linkUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0
)
```

### 4.3 Criar Repositório da Loja

```kotlin
// StoreRepository.kt
package com.daime.grow.data.repository

import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.BannerDto
import com.daime.grow.data.remote.model.OrderDto
import com.daime.grow.data.remote.model.ProductDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StoreRepository {
    private val supabase = SupabaseClient.clientOrNull
    private val productsTable = "products"
    private val ordersTable = "orders"
    private val bannersTable = "banners"

    // Produtos
    suspend fun getProducts(category: String? = null): List<ProductDto> {
        val supabase = supabase ?: return emptyList()
        return try {
            var query = supabase.from(productsTable).select()
            if (category != null) {
                query = query.eq("category", category)
            }
            query.eq("is_active", true)
                .order("created_at", ascending = false)
                .decodeList<ProductDto>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductById(id: Long): ProductDto? {
        val supabase = supabase ?: return null
        return try {
            supabase.from(productsTable)
                .select { filter { eq("id", id.toString()) } }
                .decodeSingle<ProductDto>()
        } catch (e: Exception) {
            null
        }
    }

    // Pedidos
    suspend fun createOrder(order: OrderDto): Long? {
        val supabase = supabase ?: return null
        return try {
            val response = supabase.from(ordersTable).insert(order)
            // Extrair ID do response
            response.data?.get("id")?.toString()?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getOrderById(id: Long): OrderDto? {
        val supabase = supabase ?: return null
        return try {
            supabase.from(ordersTable)
                .select { filter { eq("id", id.toString()) } }
                .decodeSingle<OrderDto>()
        } catch (e: Exception) {
            null
        }
    }

    // Banners
    suspend fun getActiveBanners(): List<BannerDto> {
        val supabase = supabase ?: return emptyList()
        return try {
            supabase.from(bannersTable)
                .select()
                .eq("is_active", true)
                .order("display_order", ascending = true)
                .decodeList<BannerDto>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

### 4.4 Atualizar ViewModel da Loja

```kotlin
// StoreViewModel.kt
package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.remote.model.OrderDto
import com.daime.grow.data.remote.model.ProductDto
import com.daime.grow.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val products: List<ProductDto> = emptyList(),
    val banners: List<BannerDto> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CartItem(
    val product: ProductDto,
    val quantity: Int
)

class StoreViewModel : ViewModel() {
    private val repository = StoreRepository()
    
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadBanners()
    }

    fun loadProducts(category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val products = repository.getProducts(category)
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao carregar produtos",
                    isLoading = false
                )
            }
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            val banners = repository.getActiveBanners()
            _uiState.value = _uiState.value.copy(banners = banners)
        }
    }

    fun addToCart(product: ProductDto) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        
        if (existingIndex >= 0) {
            currentCart[existingIndex] = currentCart[existingIndex].copy(
                quantity = currentCart[existingIndex].quantity + 1
            )
        } else {
            currentCart.add(CartItem(product, 1))
        }
        
        _uiState.value = _uiState.value.copy(cart = currentCart)
    }

    fun checkout(customerName: String, customerPhone: String, address: String) {
        viewModelScope.launch {
            val cart = _uiState.value.cart
            if (cart.isEmpty()) return@launch

            val total = cart.sumOf { it.product.price * it.quantity }
            
            val order = OrderDto(
                customerName = customerName,
                customerPhone = customerPhone,
                shippingAddress = address,
                totalAmount = total,
                status = "pending",
                paymentMethod = "pix"
            )

            val orderId = repository.createOrder(order)
            
            if (orderId != null) {
                _uiState.value = _uiState.value.copy(cart = emptyList())
                // Navegar para tela de sucesso
            }
        }
    }
}
```

### 4.5 Atualizar Tela da Loja

```kotlin
// StoreScreen.kt (atualizado)
@Composable
fun StoreScreen(
    innerPadding: PaddingValues,
    viewModel: StoreViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banners Promocionais
            if (state.banners.isNotEmpty()) {
                BannerCarousel(
                    banners = state.banners,
                    onBannerClick = { /* abrir link */ }
                )
            }
            
            // Lista de Produtos
            ProductsGrid(
                products = state.products,
                onAddToCart = viewModel::addToCart
            )
        }
        
        // Floating Cart Button
        if (state.cart.isNotEmpty()) {
            FloatingActionButton(
                onClick = { /* abrir carrinho */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                BadgeBox(count = state.cart.sumOf { it.quantity }) {
                    Icon(Icons.Default.ShoppingCart, "Carrinho")
                }
            }
        }
    }
}
```

---

## 5. Painel Web (React/Next.js)

### 5.1 Criar Projeto

```bash
npx create-next-app@latest grow-admin --typescript --tailwind --app
cd grow-admin
npm install @supabase/supabase-js recharts lucide-react
```

### 5.2 Configurar Supabase Client

```typescript
// lib/supabase.ts
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!

export const supabase = createClient(supabaseUrl, supabaseAnonKey)

// Para operações admin (use no backend/server-side)
export const supabaseAdmin = createClient(
    supabaseUrl, 
    process.env.SUPABASE_SERVICE_ROLE_KEY!
)
```

### 5.3 Variáveis de Ambiente

```env
# .env.local
NEXT_PUBLIC_SUPABASE_URL=https://seu-project.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbG...
SUPABASE_SERVICE_ROLE_KEY=eyJhbG... (NUNCA exponha no frontend!)
```

### 5.4 Estrutura de Pastas

```
grow-admin/
├── app/
│   ├── (auth)/
│   │   ├── login/
│   │   └── layout.tsx
│   ├── (dashboard)/
│   │   ├── dashboard/
│   │   ├── products/
│   │   ├── orders/
│   │   ├── banners/
│   │   ├── coupons/
│   │   └── layout.tsx
│   ├── api/
│   │   ├── products/
│   │   ├── orders/
│   │   └── upload/
│   └── layout.tsx
├── components/
│   ├── ui/
│   ├── products/
│   ├── orders/
│   └── dashboard/
├── lib/
│   ├── supabase.ts
│   └── utils.ts
└── .env.local
```

### 5.5 Página de Login

```typescript
// app/(auth)/login/page.tsx
'use client'

import { useState } from 'react'
import { supabase } from '@/lib/supabase'
import { useRouter } from 'next/navigation'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const router = useRouter()

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault()
        
        const { data, error } = await supabase.auth.signInWithPassword({
            email,
            password
        })

        if (error) {
            alert('Erro: ' + error.message)
            return
        }

        router.push('/dashboard')
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <form onSubmit={handleLogin} className="bg-white p-8 rounded-lg shadow-md w-96">
                <h1 className="text-2xl font-bold mb-6">Admin Grow</h1>
                
                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full p-3 border rounded mb-4"
                />
                
                <input
                    type="password"
                    placeholder="Senha"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full p-3 border rounded mb-4"
                />
                
                <button
                    type="submit"
                    className="w-full bg-green-600 text-white p-3 rounded hover:bg-green-700"
                >
                    Entrar
                </button>
            </form>
        </div>
    )
}
```

### 5.6 Dashboard Principal

```typescript
// app/(dashboard)/dashboard/page.tsx
'use client'

import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { DollarSign, ShoppingCart, Package, TrendingUp } from 'lucide-react'

export default function Dashboard() {
    const [stats, setStats] = useState({
        totalOrders: 0,
        totalRevenue: 0,
        pendingOrders: 0,
        totalProducts: 0
    })

    useEffect(() => {
        loadStats()
    }, [])

    const loadStats = async () => {
        // Pedidos totais
        const { count: ordersCount } = await supabase
            .from('orders')
            .select('*', { count: 'exact', head: true })
        
        // Receita total
        const { data: revenue } = await supabase
            .from('orders')
            .select('total_amount')
            .neq('status', 'cancelled')
        
        // Pedidos pendentes
        const { count: pendingCount } = await supabase
            .from('orders')
            .select('*', { count: 'exact', head: true })
            .eq('status', 'pending')
        
        // Produtos ativos
        const { count: productsCount } = await supabase
            .from('products')
            .select('*', { count: 'exact', head: true })
            .eq('is_active', true)

        setStats({
            totalOrders: ordersCount || 0,
            totalRevenue: revenue?.reduce((sum, o) => sum + o.total_amount, 0) || 0,
            pendingOrders: pendingCount || 0,
            totalProducts: productsCount || 0
        })
    }

    return (
        <div className="p-8">
            <h1 className="text-3xl font-bold mb-8">Dashboard</h1>
            
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle className="text-sm font-medium">Receita Total</CardTitle>
                        <DollarSign className="h-4 w-4 text-gray-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">
                            R$ {stats.totalRevenue.toFixed(2)}
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle className="text-sm font-medium">Pedidos</CardTitle>
                        <ShoppingCart className="h-4 w-4 text-gray-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{stats.totalOrders}</div>
                        <p className="text-xs text-gray-500">
                            {stats.pendingOrders} pendentes
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle className="text-sm font-medium">Produtos</CardTitle>
                        <Package className="h-4 w-4 text-gray-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{stats.totalProducts}</div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle className="text-sm font-medium">Crescimento</CardTitle>
                        <TrendingUp className="h-4 w-4 text-gray-500" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">+20.1%</div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}
```

### 5.7 Gerenciamento de Produtos

```typescript
// app/(dashboard)/products/page.tsx
'use client'

import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Plus, Edit, Trash2 } from 'lucide-react'

interface Product {
    id: number
    name: string
    price: number
    stock: number
    category: string
    is_active: boolean
}

export default function ProductsPage() {
    const [products, setProducts] = useState<Product[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [showForm, setShowForm] = useState(false)

    useEffect(() => {
        loadProducts()
    }, [])

    const loadProducts = async () => {
        const { data } = await supabase
            .from('products')
            .select('*')
            .order('created_at', { ascending: false })
        
        if (data) setProducts(data)
        setIsLoading(false)
    }

    const handleDelete = async (id: number) => {
        if (!confirm('Tem certeza?')) return
        
        await supabase.from('products').update({ is_active: false }).eq('id', id)
        loadProducts()
    }

    return (
        <div className="p-8">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-3xl font-bold">Produtos</h1>
                <Button onClick={() => setShowForm(!showForm)}>
                    <Plus className="mr-2 h-4 w-4" />
                    Novo Produto
                </Button>
            </div>

            {showForm && <ProductForm onClose={() => setShowForm(false)} />}

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nome</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Categoria</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Preço</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estoque</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ações</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {products.map((product) => (
                            <tr key={product.id}>
                                <td className="px-6 py-4">{product.name}</td>
                                <td className="px-6 py-4">{product.category}</td>
                                <td className="px-6 py-4">R$ {product.price.toFixed(2)}</td>
                                <td className="px-6 py-4">{product.stock}</td>
                                <td className="px-6 py-4">
                                    <span className={`px-2 py-1 rounded text-xs ${product.is_active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                        {product.is_active ? 'Ativo' : 'Inativo'}
                                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <Button variant="ghost" size="sm">
                                        <Edit className="h-4 w-4" />
                                    </Button>
                                    <Button 
                                        variant="ghost" 
                                        size="sm"
                                        onClick={() => handleDelete(product.id)}
                                    >
                                        <Trash2 className="h-4 w-4 text-red-600" />
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}
```

### 5.8 Gerenciamento de Banners

```typescript
// app/(dashboard)/banners/page.tsx
'use client'

import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Upload } from 'lucide-react'

interface Banner {
    id: number
    title: string
    image_url: string
    link_url: string
    is_active: boolean
    display_order: number
}

export default function BannersPage() {
    const [banners, setBanners] = useState<Banner[]>([])
    const [uploading, setUploading] = useState(false)

    useEffect(() => {
        loadBanners()
    }, [])

    const loadBanners = async () => {
        const { data } = await supabase
            .from('banners')
            .select('*')
            .order('display_order')
        
        if (data) setBanners(data)
    }

    const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        setUploading(true)

        try {
            const fileExt = file.name.split('.').pop()
            const fileName = `${Date.now()}.${fileExt}`
            
            const { data, error } = await supabase.storage
                .from('banners')
                .upload(fileName, file)

            if (error) throw error

            const { data: { publicUrl } } = supabase.storage
                .from('banners')
                .getPublicUrl(fileName)

            // Criar banner
            await supabase.from('banners').insert({
                title: 'Novo Banner',
                image_url: publicUrl,
                is_active: true,
                display_order: banners.length
            })

            loadBanners()
        } catch (error) {
            alert('Erro ao upload: ' + error)
        } finally {
            setUploading(false)
        }
    }

    return (
        <div className="p-8">
            <h1 className="text-3xl font-bold mb-8">Banners Promocionais</h1>

            <div className="mb-8">
                <label className="block mb-2">
                    <Button asChild>
                        <span>
                            <Upload className="mr-2 h-4 w-4" />
                            {uploading ? 'Enviando...' : 'Upload de Imagem'}
                        </span>
                    </Button>
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleImageUpload}
                        className="hidden"
                    />
                </label>
            </div>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {banners.map((banner) => (
                    <div key={banner.id} className="border rounded-lg overflow-hidden">
                        <img 
                            src={banner.image_url} 
                            alt={banner.title}
                            className="w-full h-48 object-cover"
                        />
                        <div className="p-4">
                            <Input 
                                value={banner.title}
                                onChange={async (e) => {
                                    await supabase
                                        .from('banners')
                                        .update({ title: e.target.value })
                                        .eq('id', banner.id)
                                    loadBanners()
                                }}
                                placeholder="Título"
                            />
                            <div className="flex gap-2 mt-4">
                                <Button
                                    variant={banner.is_active ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={async () => {
                                        await supabase
                                            .from('banners')
                                            .update({ is_active: !banner.is_active })
                                            .eq('id', banner.id)
                                        loadBanners()
                                    }}
                                >
                                    {banner.is_active ? 'Ativo' : 'Inativo'}
                                </Button>
                                <Button
                                    variant="destructive"
                                    size="sm"
                                    onClick={async () => {
                                        await supabase
                                            .from('banners')
                                            .delete()
                                            .eq('id', banner.id)
                                        loadBanners()
                                    }}
                                >
                                    Excluir
                                </Button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}
```

---

## 6. Segurança e RLS

### 6.1 Row Level Security (RLS)

```sql
-- Habilitar RLS
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE banners ENABLE ROW LEVEL SECURITY;

-- Produtos: Qualquer um pode ler ativos, apenas admins podem escrever
CREATE POLICY "Public can view active products"
ON products FOR SELECT
USING (is_active = true);

CREATE POLICY "Admins can manage products"
ON products FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM admin_users
        WHERE admin_users.id = auth.uid()
    )
);

-- Pedidos: Cliente vê apenas seus pedidos
CREATE POLICY "Users can view own orders"
ON orders FOR SELECT
USING (
    customer_email = auth.email()
    OR 
    EXISTS (
        SELECT 1 FROM admin_users
        WHERE admin_users.id = auth.uid()
    )
);

CREATE POLICY "Users can create own orders"
ON orders FOR INSERT
WITH CHECK (true); -- Qualquer um pode criar pedido

-- Banners: Apenas leitura pública
CREATE POLICY "Public can view active banners"
ON banners FOR SELECT
USING (is_active = true);

CREATE POLICY "Admins can manage banners"
ON banners FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM admin_users
        WHERE admin_users.id = auth.uid()
    )
);
```

### 6.2 Criar Admin Inicial

```sql
-- Inserir primeiro admin manualmente
INSERT INTO admin_users (id, email, role)
VALUES 
    ('00000000-0000-0000-0000-000000000001', 'admin@seuemail.com', 'admin');

-- Ou via API após criar auth user
```

---

## 7. Deploy e Publicação

### 7.1 Deploy do Painel Web

#### Vercel (Recomendado)

```bash
# Instalar Vercel CLI
npm install -g vercel

# Deploy
cd grow-admin
vercel --prod
```

#### Variáveis no Vercel

Em **Settings > Environment Variables**:
```
NEXT_PUBLIC_SUPABASE_URL=https://...
NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJ...
SUPABASE_SERVICE_ROLE_KEY=eyJ... (apenas server-side)
```

### 7.2 Build do App Android

```bash
cd D:\Dev\Porjetos\Grow

# Build debug
.\gradlew.bat assembleDebug

# Build release (AAB)
.\gradlew.bat bundleRelease

# APK estará em:
# app/build/outputs/bundle/release/app-release.aab
```

### 7.3 Checklist de Publicação

- [ ] Configurar keystore de assinatura
- [ ] Gerar AAB assinado
- [ ] Criar conta Google Play (US$ 25)
- [ ] Preparar screenshots e descrições
- [ ] Política de privacidade hospedada
- [ ] Submeter para revisão

---

## 📞 Suporte

### Links Úteis

- [Supabase Docs](https://supabase.com/docs)
- [Next.js Docs](https://nextjs.org/docs)
- [Android Developers](https://developer.android.com)
- [Material Design 3](https://m3.material.io)

### Próximos Passos

1. ✅ Configurar Supabase
2. ✅ Criar tabelas no banco
3. ✅ Integrar no App Android
4. ✅ Criar Painel Web
5. ✅ Configurar RLS e segurança
6. 🚀 Deploy e publicação

---

**Tempo estimado de implementação:** 3-5 dias

**Dúvidas?** Consulte a documentação oficial ou abra uma issue no GitHub.
