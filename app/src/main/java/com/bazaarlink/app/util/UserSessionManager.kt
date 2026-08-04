package com.bazaarlink.app.util

import android.content.Context
import android.content.SharedPreferences
import com.bazaarlink.app.models.User
import com.bazaarlink.app.models.VendorProfile
import org.json.JSONArray
import org.json.JSONObject

object UserSessionManager {
    private const val PREFS_NAME = "bazaarlink_user_session_v6"
    private const val KEY_ACTIVE_IDENTIFIER = "active_user_identifier"
    private const val KEY_USERS_REGISTRY = "registered_users_registry"

    fun saveUserSession(context: Context, user: User) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val registryStr = prefs.getString(KEY_USERS_REGISTRY, "{}") ?: "{}"
        val registry = try { JSONObject(registryStr) } catch (e: Exception) { JSONObject() }

        val rolesArr = JSONArray()
        val effectiveRoles = (user.registeredRoles + (if (user.vendorProfile != null) listOf("VENDOR") else emptyList()) + listOf("BUYER")).distinct()
        effectiveRoles.forEach { rolesArr.put(it) }

        val userJson = JSONObject().apply {
            put("userId", user.userId)
            put("email", user.email)
            put("password", user.password)
            put("role", user.role)
            put("registeredRoles", rolesArr)
            put("displayName", user.displayName)
            put("phoneNumber", user.phoneNumber)
            put("cnic", user.cnic)

            user.vendorProfile?.let { vp ->
                val vpJson = JSONObject().apply {
                    put("shopName", vp.shopName)
                    put("marketZone", vp.marketZone)
                    put("connectsBalance", vp.connectsBalance)
                }
                put("vendorProfile", vpJson)
            }
        }

        val primaryKey = if (user.phoneNumber.isNotBlank()) user.phoneNumber.trim() else user.userId.lowercase()
        registry.put(primaryKey, userJson)
        if (user.userId.isNotBlank()) {
            registry.put(user.userId.lowercase(), userJson)
        }

        prefs.edit()
            .putString(KEY_ACTIVE_IDENTIFIER, primaryKey)
            .putString(KEY_USERS_REGISTRY, registry.toString())
            .apply()
    }

    fun getActiveUserSession(context: Context): User? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeKey = prefs.getString(KEY_ACTIVE_IDENTIFIER, null) ?: return null
        return findUserByEmailOrUid(context, activeKey)
    }

    fun getLastRegisteredUser(context: Context): User? {
        val active = getActiveUserSession(context)
        if (active != null) return active

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val registryStr = prefs.getString(KEY_USERS_REGISTRY, "{}") ?: "{}"
        val registry = try { JSONObject(registryStr) } catch (e: Exception) { return null }

        val keys = registry.keys()
        if (keys.hasNext()) {
            val firstKey = keys.next()
            val userObj = registry.optJSONObject(firstKey)
            if (userObj != null) return parseUserJson(userObj)
        }
        return null
    }

    fun findUserByPhone(context: Context, phoneNumber: String): User? {
        if (phoneNumber.isBlank()) return null
        val cleanPhone = phoneNumber.trim()
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val registryStr = prefs.getString(KEY_USERS_REGISTRY, "{}") ?: "{}"
        val registry = try { JSONObject(registryStr) } catch (e: Exception) { return null }

        val keys = registry.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val obj = registry.optJSONObject(k)
            if (obj != null) {
                val phone = obj.optString("phoneNumber", "").trim()
                if (phone == cleanPhone) {
                    return parseUserJson(obj)
                }
            }
        }
        return null
    }

    fun findUserByEmailOrUid(context: Context, identifier: String): User? {
        if (identifier.isBlank()) return null
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val registryStr = prefs.getString(KEY_USERS_REGISTRY, "{}") ?: "{}"
        val registry = try { JSONObject(registryStr) } catch (e: Exception) { return null }

        val key = identifier.lowercase()
        val userObj = if (registry.has(key)) registry.optJSONObject(key) else {
            var found: JSONObject? = null
            val keys = registry.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = registry.optJSONObject(k)
                if (obj != null) {
                    val em = obj.optString("email", "")
                    val id = obj.optString("userId", "")
                    val ph = obj.optString("phoneNumber", "")
                    if (em.equals(identifier, ignoreCase = true) || id.equals(identifier, ignoreCase = true) || ph == identifier) {
                        found = obj
                        break
                    }
                }
            }
            found
        } ?: return null

        return parseUserJson(userObj)
    }

    fun setActiveUser(context: Context, user: User) {
        saveUserSession(context, user)
    }

    fun clearActiveSession(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ACTIVE_IDENTIFIER).apply()
    }

    private fun parseUserJson(obj: JSONObject): User {
        val uid = obj.optString("userId", "")
        val email = obj.optString("email", "")
        val password = obj.optString("password", "")
        val role = obj.optString("role", "BUYER")
        val name = obj.optString("displayName", "")
        val phone = obj.optString("phoneNumber", "")
        val cnic = obj.optString("cnic", "")

        val vpObj = obj.optJSONObject("vendorProfile")
        val vp = if (vpObj != null || role == "VENDOR") {
            VendorProfile(
                shopName = vpObj?.optString("shopName", "$name's Shop") ?: "$name's Shop",
                marketZone = vpObj?.optString("marketZone", "Star City Mall, Saddar") ?: "Star City Mall, Saddar",
                categories = listOf("mobile parts"),
                connectsBalance = vpObj?.optInt("connectsBalance", 50) ?: 50
            )
        } else null

        val rolesList = mutableListOf<String>()
        val rolesArray = obj.optJSONArray("registeredRoles")
        if (rolesArray != null) {
            for (i in 0 until rolesArray.length()) {
                val r = rolesArray.optString(i, "")
                if (r.isNotBlank()) rolesList.add(r)
            }
        }
        if (!rolesList.contains(role)) rolesList.add(role)
        if (vp != null && !rolesList.contains("VENDOR")) rolesList.add("VENDOR")
        if (!rolesList.contains("BUYER")) rolesList.add("BUYER")

        return User(
            userId = uid,
            email = email,
            password = password,
            role = role,
            registeredRoles = rolesList.distinct(),
            displayName = name,
            phoneNumber = phone,
            cnic = cnic,
            vendorProfile = vp
        )
    }
}
