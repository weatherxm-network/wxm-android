package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AuthDataSource
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.datasource.UserDataSource
import org.koin.core.component.KoinComponent

interface AuthRepository {
    suspend fun login(username: String, password: String): Either<Error, String>
    suspend fun logout()
    suspend fun isLoggedIn(): Either<Error, String>
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Error, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
}

class AuthRepositoryImpl(
    private val authTokenDatasource: AuthTokenDataSource,
    private val credentialsDatasource: CredentialsDataSource,
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource
) : AuthRepository, KoinComponent {

    override suspend fun login(username: String, password: String): Either<Error, String> {
        return authDataSource.login(username, password)
    }

    override suspend fun logout() {
        authTokenDatasource.clear()
        credentialsDatasource.clear()
        userDataSource.clear()
    }

    override suspend fun isLoggedIn(): Either<Error, String> {
        return credentialsDatasource.getCredentials().map { it.username }
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Error, String> {
        return authDataSource.signup(username, firstName, lastName)
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authDataSource.resetPassword(email)
    }
}
