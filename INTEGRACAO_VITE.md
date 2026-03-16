# 🚀 Guia Rápido: Painel Web com Vite + React + Supabase

## Visão Geral

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   App Android   │         │    Supabase      │         │  Painel Vite    │
│   (Grow App)    │◄───────►│  (Backend/Banco) │◄───────►│  (React + TS)   │
└─────────────────┘         └──────────────────┘         └─────────────────┘
```

---

## 1. Criar Projeto Vite

### 1.1 Instalar e Criar

```bash
# Criar pasta do projeto
mkdir grow-admin
cd grow-admin

# Criar projeto Vite com React + TypeScript
npm create vite@latest . -- --template react-ts

# Instalar dependências
npm install

# Instalar Supabase e outras libs
npm install @supabase/supabase-js react-router-dom lucide-react recharts
npm install -D tailwindcss postcss autoprefixer

# Configurar Tailwind
npx tailwindcss init -p
```

### 1.2 Configurar Tailwind

**`tailwind.config.js`:**
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**`src/index.css`:**
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### 1.3 Variáveis de Ambiente

Crie **`.env`** na raiz:

```env
VITE_SUPABASE_URL=https://dvyidxhutjmgtvkjkkkm.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR2eWlkeGh1dGptZ3R2a2pra2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMwMDYxNzYsImV4cCI6MjA4ODU4MjE3Nn0.nEM7LBIYMHWe0q549hmiuVDfXKreagXIX9MUE3Wprzc
```

---

## 2. Estrutura de Pastas

```
grow-admin/
├── public/
├── src/
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   └── Card.tsx
│   │   ├── Layout.tsx
│   │   └── ProtectedRoute.tsx
│   ├── pages/
│   │   ├── Login.tsx
│   │   ├── Dashboard.tsx
│   │   ├── Products.tsx
│   │   ├── Orders.tsx
│   │   ├── Banners.tsx
│   │   └── Settings.tsx
│   ├── lib/
│   │   └── supabase.ts
│   ├── types/
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── .env
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 3. Configuração Supabase

### 3.1 Client Supabase

**`src/lib/supabase.ts`:**
```typescript
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

export const supabase = createClient(supabaseUrl, supabaseAnonKey)

// Tipos
export type Product = {
    id: number
    name: string
    description: string | null
    price: number
    category: string | null
    stock: number
    image_url: string | null
    is_active: boolean
    created_at: string
}

export type Order = {
    id: number
    customer_name: string
    customer_email: string | null
    customer_phone: string | null
    total_amount: number
    status: 'pending' | 'paid' | 'shipped' | 'delivered' | 'cancelled'
    payment_status: 'unpaid' | 'paid' | 'refunded'
    created_at: string
}

export type Banner = {
    id: number
    title: string | null
    image_url: string
    link_url: string | null
    is_active: boolean
    display_order: number
}
```

---

## 4. Componentes UI Básicos

### 4.1 Button

**`src/components/ui/Button.tsx`:**
```typescript
import { ButtonHTMLAttributes, ReactNode } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
    size?: 'sm' | 'md' | 'lg'
    children: ReactNode
}

export function Button({ 
    variant = 'primary', 
    size = 'md', 
    children, 
    className = '', 
    ...props 
}: ButtonProps) {
    const baseStyles = 'font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2'
    
    const variants = {
        primary: 'bg-green-600 text-white hover:bg-green-700 focus:ring-green-500',
        secondary: 'bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-500',
        danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
        ghost: 'bg-transparent hover:bg-gray-100 focus:ring-gray-500'
    }
    
    const sizes = {
        sm: 'px-3 py-1.5 text-sm',
        md: 'px-4 py-2 text-base',
        lg: 'px-6 py-3 text-lg'
    }
    
    return (
        <button 
            className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className}`}
            {...props}
        >
            {children}
        </button>
    )
}
```

### 4.2 Input

**`src/components/ui/Input.tsx`:**
```typescript
import { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    label?: string
    error?: string
}

export function Input({ label, error, className = '', ...props }: InputProps) {
    return (
        <div className="w-full">
            {label && (
                <label className="block text-sm font-medium text-gray-700 mb-1">
                    {label}
                </label>
            )}
            <input 
                className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 ${
                    error ? 'border-red-500' : 'border-gray-300'
                } ${className}`}
                {...props}
            />
            {error && (
                <p className="mt-1 text-sm text-red-600">{error}</p>
            )}
        </div>
    )
}
```

### 4.3 Card

**`src/components/ui/Card.tsx`:**
```typescript
import { ReactNode } from 'react'

interface CardProps {
    title?: string
    children: ReactNode
    className?: string
}

export function Card({ title, children, className = '' }: CardProps) {
    return (
        <div className={`bg-white rounded-lg shadow-md p-6 ${className}`}>
            {title && (
                <h3 className="text-lg font-semibold mb-4">{title}</h3>
            )}
            {children}
        </div>
    )
}
```

---

## 5. Layout e Rotas

### 5.1 Layout Principal

**`src/components/Layout.tsx`:**
```typescript
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { Home, ShoppingCart, Package, Image, Settings, LogOut, Menu, X } from 'lucide-react'
import { useState } from 'react'
import { supabase } from '../lib/supabase'

export function Layout() {
    const [sidebarOpen, setSidebarOpen] = useState(true)
    const navigate = useNavigate()

    const handleLogout = async () => {
        await supabase.auth.signOut()
        navigate('/login')
    }

    const menuItems = [
        { icon: Home, label: 'Dashboard', path: '/dashboard' },
        { icon: Package, label: 'Produtos', path: '/products' },
        { icon: ShoppingCart, label: 'Pedidos', path: '/orders' },
        { icon: Image, label: 'Banners', path: '/banners' },
        { icon: Settings, label: 'Configurações', path: '/settings' },
    ]

    return (
        <div className="min-h-screen bg-gray-100">
            {/* Sidebar */}
            <aside className={`fixed top-0 left-0 h-full bg-white shadow-lg transition-all ${
                sidebarOpen ? 'w-64' : 'w-20'
            }`}>
                <div className="p-4 border-b">
                    <div className="flex items-center justify-between">
                        {sidebarOpen && <h1 className="text-xl font-bold text-green-600">Grow Admin</h1>}
                        <button 
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            className="p-2 hover:bg-gray-100 rounded-lg"
                        >
                            {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
                        </button>
                    </div>
                </div>

                <nav className="p-4 space-y-2">
                    {menuItems.map((item) => (
                        <Link
                            key={item.path}
                            to={item.path}
                            className="flex items-center gap-3 p-3 hover:bg-gray-100 rounded-lg transition-colors"
                        >
                            <item.icon size={20} className="text-gray-600" />
                            {sidebarOpen && <span>{item.label}</span>}
                        </Link>
                    ))}
                </nav>

                <div className="absolute bottom-0 w-full p-4 border-t">
                    <button 
                        onClick={handleLogout}
                        className="flex items-center gap-3 p-3 w-full hover:bg-red-50 text-red-600 rounded-lg"
                    >
                        <LogOut size={20} />
                        {sidebarOpen && <span>Sair</span>}
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main className={`transition-all ${sidebarOpen ? 'ml-64' : 'ml-20'}`}>
                <Outlet />
            </main>
        </div>
    )
}
```

### 5.2 Protected Route

**`src/components/ProtectedRoute.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { supabase } from '../lib/supabase'

interface ProtectedRouteProps {
    children: React.ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
    const [loading, setLoading] = useState(true)
    const [authenticated, setAuthenticated] = useState(false)

    useEffect(() => {
        checkAuth()
        
        const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
            setAuthenticated(!!session)
            setLoading(false)
        })

        return () => subscription.unsubscribe()
    }, [])

    const checkAuth = async () => {
        const { data: { session } } = await supabase.auth.getSession()
        setAuthenticated(!!session)
        setLoading(false)
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
            </div>
        )
    }

    if (!authenticated) {
        return <Navigate to="/login" replace />
    }

    return <>{children}</>
}
```

---

## 6. Páginas

### 6.1 Login

**`src/pages/Login.tsx`:**
```typescript
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../lib/supabase'
import { Input } from '../components/ui/Input'
import { Button } from '../components/ui/Button'

export function Login() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setError('')

        const { error } = await supabase.auth.signInWithPassword({
            email,
            password
        })

        if (error) {
            setError(error.message)
        } else {
            navigate('/dashboard')
        }

        setLoading(false)
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-400 to-blue-500">
            <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-md">
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold text-green-600">Grow Admin</h1>
                    <p className="text-gray-600 mt-2">Painel de Controle</p>
                </div>

                <form onSubmit={handleLogin} className="space-y-6">
                    <Input
                        type="email"
                        label="Email"
                        placeholder="admin@grow.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />

                    <Input
                        type="password"
                        label="Senha"
                        placeholder="••••••••"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />

                    {error && (
                        <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm">
                            {error}
                        </div>
                    )}

                    <Button 
                        type="submit" 
                        className="w-full" 
                        disabled={loading}
                    >
                        {loading ? 'Entrando...' : 'Entrar'}
                    </Button>
                </form>
            </div>
        </div>
    )
}
```

### 6.2 Dashboard

**`src/pages/Dashboard.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabase'
import { Card } from '../components/ui/Card'
import { DollarSign, ShoppingCart, Package, TrendingUp } from 'lucide-react'

export function Dashboard() {
    const [stats, setStats] = useState({
        totalOrders: 0,
        totalRevenue: 0,
        pendingOrders: 0,
        totalProducts: 0
    })
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        loadStats()
    }, [])

    const loadStats = async () => {
        try {
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
        } catch (error) {
            console.error('Erro ao carregar stats:', error)
        } finally {
            setLoading(false)
        }
    }

    const statCards = [
        {
            title: 'Receita Total',
            value: `R$ ${stats.totalRevenue.toFixed(2)}`,
            icon: DollarSign,
            color: 'text-green-600',
            bgColor: 'bg-green-100'
        },
        {
            title: 'Pedidos',
            value: stats.totalOrders.toString(),
            icon: ShoppingCart,
            color: 'text-blue-600',
            bgColor: 'bg-blue-100',
            subtitle: `${stats.pendingOrders} pendentes`
        },
        {
            title: 'Produtos',
            value: stats.totalProducts.toString(),
            icon: Package,
            color: 'text-purple-600',
            bgColor: 'bg-purple-100'
        },
        {
            title: 'Crescimento',
            value: '+20.1%',
            icon: TrendingUp,
            color: 'text-orange-600',
            bgColor: 'bg-orange-100'
        }
    ]

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
            </div>
        )
    }

    return (
        <div className="p-8">
            <h1 className="text-3xl font-bold text-gray-800 mb-8">Dashboard</h1>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
                {statCards.map((stat) => (
                    <Card key={stat.title}>
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-gray-600 mb-1">{stat.title}</p>
                                <p className="text-2xl font-bold">{stat.value}</p>
                                {stat.subtitle && (
                                    <p className="text-xs text-gray-500 mt-1">{stat.subtitle}</p>
                                )}
                            </div>
                            <div className={`${stat.bgColor} p-3 rounded-full`}>
                                <stat.icon className={`h-6 w-6 ${stat.color}`} />
                            </div>
                        </div>
                    </Card>
                ))}
            </div>

            {/* Gráfico de Vendas (opcional) */}
            <div className="mt-8">
                <Card title="Vendas dos Últimos 7 Dias">
                    <div className="h-64 flex items-center justify-center text-gray-400">
                        Instale recharts e implemente o gráfico aqui
                    </div>
                </Card>
            </div>
        </div>
    )
}
```

### 6.3 Produtos

**`src/pages/Products.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { supabase, type Product } from '../lib/supabase'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Plus, Edit, Trash2, Search } from 'lucide-react'

export function Products() {
    const [products, setProducts] = useState<Product[]>([])
    const [loading, setLoading] = useState(true)
    const [showForm, setShowForm] = useState(false)
    const [searchTerm, setSearchTerm] = useState('')
    const [editingProduct, setEditingProduct] = useState<Product | null>(null)

    useEffect(() => {
        loadProducts()
    }, [])

    const loadProducts = async () => {
        const { data } = await supabase
            .from('products')
            .select('*')
            .order('created_at', { ascending: false })
        
        if (data) setProducts(data)
        setLoading(false)
    }

    const handleDelete = async (id: number) => {
        if (!confirm('Tem certeza que deseja excluir?')) return
        
        await supabase.from('products').update({ is_active: false }).eq('id', id)
        loadProducts()
    }

    const filteredProducts = products.filter(p => 
        p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.category?.toLowerCase().includes(searchTerm.toLowerCase())
    )

    return (
        <div className="p-8">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Produtos</h1>
                    <p className="text-gray-600 mt-1">Gerencie seu catálogo de produtos</p>
                </div>
                <Button onClick={() => {
                    setEditingProduct(null)
                    setShowForm(!showForm)
                }}>
                    <Plus className="mr-2 h-5 w-5" />
                    Novo Produto
                </Button>
            </div>

            {showForm && (
                <ProductForm 
                    product={editingProduct}
                    onClose={() => {
                        setShowForm(false)
                        setEditingProduct(null)
                        loadProducts()
                    }}
                />
            )}

            {/* Search */}
            <div className="mb-6">
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
                    <input
                        type="text"
                        placeholder="Buscar produtos..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                    />
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Produto</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Categoria</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Preço</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estoque</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ações</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {filteredProducts.map((product) => (
                            <tr key={product.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4">
                                    <div className="flex items-center">
                                        {product.image_url && (
                                            <img 
                                                src={product.image_url} 
                                                alt={product.name}
                                                className="h-10 w-10 rounded-lg object-cover mr-3"
                                            />
                                        )}
                                        <div>
                                            <p className="font-medium">{product.name}</p>
                                            <p className="text-sm text-gray-500 truncate max-w-xs">
                                                {product.description}
                                            </p>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <span className="px-2 py-1 bg-gray-100 rounded-full text-sm">
                                        {product.category || 'Geral'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 font-medium">
                                    R$ {product.price.toFixed(2)}
                                </td>
                                <td className="px-6 py-4">
                                    <span className={product.stock > 10 ? 'text-green-600' : 'text-orange-600'}>
                                        {product.stock} un
                                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <span className={`px-2 py-1 rounded-full text-xs ${
                                        product.is_active 
                                            ? 'bg-green-100 text-green-800' 
                                            : 'bg-red-100 text-red-800'
                                    }`}>
                                        {product.is_active ? 'Ativo' : 'Inativo'}
                                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex gap-2">
                                        <Button 
                                            variant="ghost" 
                                            size="sm"
                                            onClick={() => {
                                                setEditingProduct(product)
                                                setShowForm(true)
                                            }}
                                        >
                                            <Edit className="h-4 w-4" />
                                        </Button>
                                        <Button 
                                            variant="ghost" 
                                            size="sm"
                                            onClick={() => handleDelete(product.id)}
                                        >
                                            <Trash2 className="h-4 w-4 text-red-600" />
                                        </Button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

// Formulário de Produto
function ProductForm({ product, onClose }: { product: Product | null, onClose: () => void }) {
    const [loading, setLoading] = useState(false)
    const [formData, setFormData] = useState({
        name: product?.name || '',
        description: product?.description || '',
        price: product?.price?.toString() || '',
        category: product?.category || '',
        stock: product?.stock?.toString() || '0',
        is_active: product?.is_active ?? true
    })

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)

        const data = {
            ...formData,
            price: parseFloat(formData.price),
            stock: parseInt(formData.stock)
        }

        try {
            if (product) {
                await supabase.from('products').update(data).eq('id', product.id)
            } else {
                await supabase.from('products').insert(data)
            }
            onClose()
        } catch (error) {
            alert('Erro ao salvar produto')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Card className="mb-8">
            <h3 className="text-lg font-semibold mb-4">
                {product ? 'Editar Produto' : 'Novo Produto'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                    <Input
                        label="Nome"
                        value={formData.name}
                        onChange={(e) => setFormData({...formData, name: e.target.value})}
                        required
                    />
                    <Input
                        label="Categoria"
                        value={formData.category}
                        onChange={(e) => setFormData({...formData, category: e.target.value})}
                    />
                </div>
                <Input
                    label="Descrição"
                    value={formData.description}
                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                />
                <div className="grid grid-cols-2 gap-4">
                    <Input
                        label="Preço"
                        type="number"
                        step="0.01"
                        value={formData.price}
                        onChange={(e) => setFormData({...formData, price: e.target.value})}
                        required
                    />
                    <Input
                        label="Estoque"
                        type="number"
                        value={formData.stock}
                        onChange={(e) => setFormData({...formData, stock: e.target.value})}
                    />
                </div>
                <div className="flex gap-4">
                    <Button type="submit" disabled={loading}>
                        {loading ? 'Salvando...' : 'Salvar'}
                    </Button>
                    <Button type="button" variant="secondary" onClick={onClose}>
                        Cancelar
                    </Button>
                </div>
            </form>
        </Card>
    )
}
```

### 6.4 Pedidos

**`src/pages/Orders.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { supabase, type Order } from '../lib/supabase'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { Eye, RefreshCw } from 'lucide-react'

export function Orders() {
    const [orders, setOrders] = useState<Order[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        loadOrders()
    }, [])

    const loadOrders = async () => {
        const { data } = await supabase
            .from('orders')
            .select('*')
            .order('created_at', { ascending: false })
        
        if (data) setOrders(data)
        setLoading(false)
    }

    const updateStatus = async (id: number, status: Order['status']) => {
        await supabase.from('orders').update({ status }).eq('id', id)
        loadOrders()
    }

    const getStatusColor = (status: string) => {
        const colors: Record<string, string> = {
            pending: 'bg-yellow-100 text-yellow-800',
            paid: 'bg-blue-100 text-blue-800',
            shipped: 'bg-purple-100 text-purple-800',
            delivered: 'bg-green-100 text-green-800',
            cancelled: 'bg-red-100 text-red-800'
        }
        return colors[status] || 'bg-gray-100 text-gray-800'
    }

    return (
        <div className="p-8">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Pedidos</h1>
                    <p className="text-gray-600 mt-1">Gerencie todos os pedidos</p>
                </div>
                <Button onClick={loadOrders} variant="secondary">
                    <RefreshCw className="mr-2 h-4 w-4" />
                    Atualizar
                </Button>
            </div>

            {loading ? (
                <div className="flex justify-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
                </div>
            ) : (
                <div className="grid gap-6">
                    {orders.map((order) => (
                        <Card key={order.id}>
                            <div className="flex justify-between items-start">
                                <div className="flex-1">
                                    <div className="flex items-center gap-4 mb-4">
                                        <h3 className="text-lg font-semibold">
                                            Pedido #{order.id}
                                        </h3>
                                        <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                                            getStatusColor(order.status)
                                        }`}>
                                            {order.status === 'pending' && 'Pendente'}
                                            {order.status === 'paid' && 'Pago'}
                                            {order.status === 'shipped' && 'Enviado'}
                                            {order.status === 'delivered' && 'Entregue'}
                                            {order.status === 'cancelled' && 'Cancelado'}
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 text-sm">
                                        <div>
                                            <p className="text-gray-600">Cliente</p>
                                            <p className="font-medium">{order.customer_name}</p>
                                        </div>
                                        <div>
                                            <p className="text-gray-600">Email</p>
                                            <p className="font-medium">{order.customer_email || 'N/A'}</p>
                                        </div>
                                        <div>
                                            <p className="text-gray-600">Telefone</p>
                                            <p className="font-medium">{order.customer_phone || 'N/A'}</p>
                                        </div>
                                        <div>
                                            <p className="text-gray-600">Total</p>
                                            <p className="font-medium text-green-600">
                                                R$ {order.total_amount.toFixed(2)}
                                            </p>
                                        </div>
                                    </div>

                                    {order.shipping_address && (
                                        <div className="mt-4 p-3 bg-gray-50 rounded-lg">
                                            <p className="text-sm text-gray-600 mb-1">Endereço de Entrega</p>
                                            <p className="font-medium">{order.shipping_address}</p>
                                        </div>
                                    )}
                                </div>

                                <div className="flex flex-col gap-2">
                                    {order.status === 'pending' && (
                                        <>
                                            <Button 
                                                size="sm" 
                                                onClick={() => updateStatus(order.id, 'paid')}
                                            >
                                                Marcar como Pago
                                            </Button>
                                            <Button 
                                                size="sm" 
                                                variant="danger"
                                                onClick={() => updateStatus(order.id, 'cancelled')}
                                            >
                                                Cancelar
                                            </Button>
                                        </>
                                    )}
                                    {order.status === 'paid' && (
                                        <Button 
                                            size="sm" 
                                            onClick={() => updateStatus(order.id, 'shipped')}
                                        >
                                            Marcar como Enviado
                                        </Button>
                                    )}
                                    {order.status === 'shipped' && (
                                        <Button 
                                            size="sm" 
                                            onClick={() => updateStatus(order.id, 'delivered')}
                                        >
                                            Marcar como Entregue
                                        </Button>
                                    )}
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    )
}
```

### 6.5 Banners

**`src/pages/Banners.tsx`:**
```typescript
import { useEffect, useState } from 'react'
import { supabase, type Banner } from '../lib/supabase'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Upload, Trash2, MoveUp, MoveDown } from 'lucide-react'

export function Banners() {
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
            
            const { error: uploadError } = await supabase.storage
                .from('banners')
                .upload(fileName, file)

            if (uploadError) throw uploadError

            const { data: { publicUrl } } = supabase.storage
                .from('banners')
                .getPublicUrl(fileName)

            await supabase.from('banners').insert({
                title: 'Novo Banner',
                image_url: publicUrl,
                is_active: true,
                display_order: banners.length
            })

            loadBanners()
        } catch (error: any) {
            alert('Erro ao upload: ' + error.message)
        } finally {
            setUploading(false)
        }
    }

    const updateOrder = async (id: number, newOrder: number) => {
        await supabase.from('banners').update({ display_order: newOrder }).eq('id', id)
        loadBanners()
    }

    const toggleActive = async (id: number, isActive: boolean) => {
        await supabase.from('banners').update({ is_active: !isActive }).eq('id', id)
        loadBanners()
    }

    const deleteBanner = async (id: number) => {
        if (!confirm('Excluir este banner?')) return
        await supabase.from('banners').delete().eq('id', id)
        loadBanners()
    }

    return (
        <div className="p-8">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Banners</h1>
                    <p className="text-gray-600 mt-1">Gerencie banners promocionais</p>
                </div>
                <label>
                    <Button asChild disabled={uploading}>
                        <span>
                            <Upload className="mr-2 h-4 w-4" />
                            {uploading ? 'Enviando...' : 'Novo Banner'}
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
                {banners.map((banner, index) => (
                    <Card key={banner.id} className="overflow-hidden">
                        <img 
                            src={banner.image_url} 
                            alt={banner.title || 'Banner'}
                            className="w-full h-48 object-cover"
                        />
                        <div className="p-4">
                            <input
                                type="text"
                                value={banner.title || ''}
                                onChange={async (e) => {
                                    await supabase
                                        .from('banners')
                                        .update({ title: e.target.value })
                                        .eq('id', banner.id)
                                    loadBanners()
                                }}
                                placeholder="Título do banner"
                                className="w-full mb-4 px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                            />
                            
                            <div className="flex items-center justify-between">
                                <div className="flex gap-2">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => updateOrder(banner.id, index - 1)}
                                        disabled={index === 0}
                                    >
                                        <MoveUp className="h-4 w-4" />
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => updateOrder(banner.id, index + 1)}
                                        disabled={index === banners.length - 1}
                                    >
                                        <MoveDown className="h-4 w-4" />
                                    </Button>
                                </div>
                                
                                <div className="flex gap-2">
                                    <Button
                                        variant={banner.is_active ? 'primary' : 'secondary'}
                                        size="sm"
                                        onClick={() => toggleActive(banner.id, banner.is_active)}
                                    >
                                        {banner.is_active ? 'Ativo' : 'Inativo'}
                                    </Button>
                                    <Button
                                        variant="danger"
                                        size="sm"
                                        onClick={() => deleteBanner(banner.id)}
                                    >
                                        <Trash2 className="h-4 w-4" />
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </Card>
                ))}
            </div>
        </div>
    )
}
```

---

## 7. Rotas do App

**`src/App.tsx`:**
```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Products } from './pages/Products'
import { Orders } from './pages/Orders'
import { Banners } from './pages/Banners'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />
                
                <Route path="/" element={
                    <ProtectedRoute>
                        <Layout />
                    </ProtectedRoute>
                }>
                    <Route index element={<Navigate to="/dashboard" replace />} />
                    <Route path="dashboard" element={<Dashboard />} />
                    <Route path="products" element={<Products />} />
                    <Route path="orders" element={<Orders />} />
                    <Route path="banners" element={<Banners />} />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

export default App
```

**`src/main.tsx`:**
```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
)
```

---

## 8. Scripts e Comandos

### 8.1 package.json

```json
{
    "name": "grow-admin",
    "private": true,
    "version": "1.0.0",
    "type": "module",
    "scripts": {
        "dev": "vite",
        "build": "tsc && vite build",
        "preview": "vite preview"
    }
}
```

### 8.2 Comandos

```bash
# Desenvolvimento (localhost:5173)
npm run dev

# Build para produção
npm run build

# Preview do build
npm run preview
```

---

## 9. Deploy (Grátis)

### 9.1 Vercel

```bash
# Instalar Vercel CLI
npm install -g vercel

# Deploy
cd grow-admin
vercel --prod
```

### 9.2 Netlify

```bash
# Build
npm run build

# Arraste a pasta 'dist' para netlify.com/drop
```

### 9.3 GitHub Pages

```bash
# Instalar gh-pages
npm install -D gh-pages

# Adicionar ao package.json
"scripts": {
    "predeploy": "npm run build",
    "deploy": "gh-pages -d dist"
}

# Deploy
npm run deploy
```

---

## 10. Checklist Final

- [ ] Criar projeto Vite
- [ ] Configurar Tailwind CSS
- [ ] Configurar Supabase (.env)
- [ ] Criar tabelas no Supabase
- [ ] Implementar Login
- [ ] Implementar Dashboard
- [ ] Implementar Produtos
- [ ] Implementar Pedidos
- [ ] Implementar Banners
- [ ] Testar localmente
- [ ] Deploy (Vercel/Netlify)

---

## 🎉 Pronto!

Seu painel web está funcional! Acesse:

- **Local:** http://localhost:5173
- **Produção:** https://seu-app.vercel.app

**Tempo estimado:** 2-3 horas
