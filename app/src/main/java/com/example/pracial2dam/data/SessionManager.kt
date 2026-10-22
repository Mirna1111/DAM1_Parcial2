package com.example.pracial2dam.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )

    private val auth = FirebaseAuth.getInstance()

    fun saveCredentials(email: String, remember: Boolean) {
        prefs.edit()
            .putBoolean("remember_me", remember)
            .putString("saved_email", if (remember) email else "")
            .apply()
    }

    fun isRememberEnabled(): Boolean {
        return prefs.getBoolean("remember_me", false)
    }

    fun getSavedEmail(): String {
        return prefs.getString("saved_email", "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return isRememberEnabled() && auth.currentUser != null
    }

    fun getCurrentUser(): String? {
        return auth.currentUser?.email
    }

    fun setCurrentUser(email: String) {
        // Compatibilidad con llamadas anteriores
        // Firebase Authentication gestiona la sesion
    }

    fun logout() {
        auth.signOut()

        prefs.edit()
            .remove("remember_me")
            .remove("saved_email")
            .remove("current_user")
            .apply()
    }
}

