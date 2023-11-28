package com.example.projektmunka.utils

fun isFieldNotEmpty(text: String) : Boolean = text.trim{ it <= ' ' }.isNotEmpty()

fun isConfirmPassWordMatch(password: String, confirmPassword: String) : Boolean {
    return password.trim {it <= ' '} == confirmPassword.trim {it <= ' '}
}