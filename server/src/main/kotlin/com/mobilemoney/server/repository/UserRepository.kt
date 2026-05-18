package com.mobilemoney.server.repository

data class UserCredentials(
    val login: String,
    val passwordHash: String,
    val salt: String
)

class UserRepository {

    fun findByLogin(login: String): UserCredentials? {
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT login, passwordHash, salt FROM users WHERE login = ?").use { stmt ->
                stmt.setString(1, login)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return UserCredentials(
                            login = rs.getString("login"),
                            passwordHash = rs.getString("passwordHash"),
                            salt = rs.getString("salt")
                        )
                    }
                }
            }
        }
        return null
    }

}