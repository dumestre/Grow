package com.daime.grow.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.daime.grow.ui.screen.store.CartItem
import com.daime.grow.ui.screen.store.StoreProduct

class StoreViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> get() = _cartItems

    fun addToCart(product: StoreProduct, quantity: Int) {
        val index = _cartItems.indexOfFirst { it.product.name == product.name }
        if (index != -1) {
            _cartItems[index] = _cartItems[index].copy(quantity = _cartItems[index].quantity + quantity)
        } else {
            _cartItems.add(CartItem(product, quantity))
        }
    }

    fun removeFromCart(productName: String) {
        _cartItems.removeAll { it.product.name == productName }
    }

    fun updateQuantity(productName: String, newQuantity: Int) {
        val index = _cartItems.indexOfFirst { it.product.name == productName }
        if (index != -1) {
            if (newQuantity <= 0) {
                _cartItems.removeAt(index)
            } else {
                _cartItems[index] = _cartItems[index].copy(quantity = newQuantity)
            }
        }
    }

    fun clearCart() {
        _cartItems.clear()
    }

    val cartTotal: Double
        get() = _cartItems.sumOf { it.product.price * it.quantity }

    val cartCount: Int
        get() = _cartItems.sumOf { it.quantity }
}
