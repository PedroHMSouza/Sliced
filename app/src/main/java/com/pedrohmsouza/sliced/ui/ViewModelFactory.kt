package com.pedrohmsouza.sliced.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.pedrohmsouza.sliced.AppContainer
import com.pedrohmsouza.sliced.SlicedApp

/** Dentro de `viewModelFactory { initializer { ... } }`, pega o container do app. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as SlicedApp).container
