package com.freshstart.app.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.freshstart.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sign-in (Google or email) and backup of the user's data to Firebase.
 * Each user's data lives under users/{uid}, readable only by them (see firestore.rules).
 * If the build has no Firebase config, [configured] is false and the app works offline only.
 */
class Account(context: Context, private val store: AppStore) {
    private val appContext = context.applicationContext
    val configured: Boolean = FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null

    private val auth: FirebaseAuth? = if (configured) FirebaseAuth.getInstance() else null
    private val db: FirebaseFirestore? = if (configured) FirebaseFirestore.getInstance() else null
    private val scope = MainScope()
    private var uploadJob: Job? = null
    private val uploadedMonths = mutableMapOf<String, Int>()

    var user by mutableStateOf(auth?.currentUser)
        private set

    /** False until the first download/upload after signing in has finished. */
    var ready by mutableStateOf(false)
        private set

    var lastSyncMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Lets Firebase check requests really come from the Play Store app. It has no effect until
        // App Check enforcement is switched on in the Firebase console.
        if (configured && BuildConfig.STORE_BUILD) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
        auth?.addAuthStateListener { user = it.currentUser }
    }

    class AccountException(message: String) : Exception(message)

    // --- Signing in ---

    suspend fun signInWithGoogle(activity: Activity) {
        val auth = auth ?: throw AccountException("Accounts aren't set up in this version.")
        val idRes = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (idRes == 0) throw AccountException("Google sign-in isn't set up in this version.")
        val option = GetSignInWithGoogleOption.Builder(appContext.getString(idRes)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AccountException("Google sign-in didn't return an account.")
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }

    suspend fun signInWithEmail(email: String, password: String) {
        val auth = auth ?: throw AccountException("Accounts aren't set up in this version.")
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun createAccount(email: String, password: String) {
        val auth = auth ?: throw AccountException("Accounts aren't set up in this version.")
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun sendPasswordReset(email: String) {
        val auth = auth ?: throw AccountException("Accounts aren't set up in this version.")
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Uploads any pending changes, signs out and clears this phone's copy. */
    suspend fun signOut() {
        val u = user
        if (u != null) runCatching { upload(u, all = true) }
        stopSync()
        auth?.signOut()
        runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) }
        store.clearAll()
    }

    /** Permanently deletes the account and all its data, in the cloud and on this phone. */
    suspend fun deleteAccount() {
        val u = user ?: return
        val db = db ?: return
        val doc = db.collection("users").document(u.uid)
        val months = doc.collection("foodMonths").get().await()
        val batch = db.batch()
        months.documents.forEach { batch.delete(it.reference) }
        batch.delete(doc)
        batch.commit().await()
        try {
            u.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw AccountException("Your data was deleted. To finish removing the account, sign out, sign back in, and tap Delete again.")
        }
        stopSync()
        runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) }
        store.clearAll()
    }

    // --- Backup ---

    /** After sign-in: restore the account's data if it's newer, otherwise back up this phone's data. */
    suspend fun startSync(u: FirebaseUser) {
        val db = db ?: return
        ready = false
        try {
            val snap = db.collection("users").document(u.uid).get().await()
            val cloudUpdated = snap.getLong("updatedAt") ?: 0L
            if (snap.exists() && (store.profile == null || cloudUpdated > store.updatedAt)) {
                download(u, snap.getString("data") ?: "{}")
                lastSyncMessage = "Your data was restored from your account."
            } else {
                upload(u, all = true)
                lastSyncMessage = "Your data is backed up to your account."
            }
        } catch (e: Exception) {
            lastSyncMessage = "Couldn't reach your account. Changes are saved on this phone and will back up later."
        }
        store.onChanged = { scheduleUpload(u) }
        ready = true
    }

    fun stopSync() {
        store.onChanged = null
        uploadJob?.cancel()
        uploadedMonths.clear()
        ready = false
    }

    private fun scheduleUpload(u: FirebaseUser) {
        uploadJob?.cancel()
        uploadJob = scope.launch {
            delay(3_000)
            runCatching { upload(u, all = false) }
                .onSuccess { lastSyncMessage = "Backed up just now." }
                .onFailure { lastSyncMessage = "Not backed up yet. It will try again on your next change." }
        }
    }

    private suspend fun download(u: FirebaseUser, data: String) {
        val db = db ?: return
        val root = JSONObject(data)
        val foods = JSONArray()
        val months = db.collection("users").document(u.uid).collection("foodMonths").get().await()
        uploadedMonths.clear()
        for (m in months.documents) {
            val text = m.getString("foods") ?: continue
            uploadedMonths[m.id] = text.hashCode()
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) foods.put(arr.getJSONObject(i))
        }
        root.put("foods", foods)
        store.replaceFromCloud(root)
    }

    /**
     * Saves the account copy. Food logs are stored one document per month, and only
     * months that changed since the last upload are written, to keep the free quota.
     */
    private suspend fun upload(u: FirebaseUser, all: Boolean) {
        val db = db ?: return
        val root = store.toCloudJson()
        val foods = root.remove("foods") as? JSONArray ?: JSONArray()
        val byMonth = mutableMapOf<String, JSONArray>()
        for (i in 0 until foods.length()) {
            val o = foods.getJSONObject(i)
            byMonth.getOrPut(o.getString("date").take(7)) { JSONArray() }.put(o)
        }
        val doc = db.collection("users").document(u.uid)
        val batch = db.batch()
        batch.set(
            doc,
            mapOf("data" to root.toString(), "updatedAt" to store.updatedAt, "email" to (u.email ?: "")),
            SetOptions.merge()
        )
        val written = mutableMapOf<String, Int>()
        for ((month, arr) in byMonth) {
            val text = arr.toString()
            if (all || uploadedMonths[month] != text.hashCode()) {
                batch.set(doc.collection("foodMonths").document(month), mapOf("foods" to text))
            }
            written[month] = text.hashCode()
        }
        // Months whose foods were all deleted.
        for (month in uploadedMonths.keys - byMonth.keys) {
            batch.delete(doc.collection("foodMonths").document(month))
        }
        batch.commit().await()
        uploadedMonths.clear()
        uploadedMonths.putAll(written)
    }
}
