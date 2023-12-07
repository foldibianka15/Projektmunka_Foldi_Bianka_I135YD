package com.example.projektmunka.viewModel

import com.example.firstapp.repository.FireStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QRCodeViewModel @Inject constructor(val fireStoreRepository: FireStoreRepository): BaseViewModel() {

    val currentUserData = fireStoreRepository.currentUserData
}