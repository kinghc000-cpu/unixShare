package com.example.data

data class UserProfile(
    val displayName: String = DEFAULT_NAME,
    val bio: String = DEFAULT_BIO,
    val avatarColorIndex: Int = 0
) {
    companion object {
        const val DEFAULT_NAME = "Unex Device"
        const val DEFAULT_BIO = "Ready to connect and share files"
    }
}
