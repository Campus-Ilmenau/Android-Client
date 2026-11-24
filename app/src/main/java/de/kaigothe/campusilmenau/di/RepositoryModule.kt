package de.kaigothe.campusilmenau.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.data.remote.mensa.MensaApi
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.data.repository.NetworkMensaRepository
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMensaRepository(impl: NetworkMensaRepository): MensaRepository

    companion object {
        @Provides
        @Singleton
        fun provideMensaApi(retrofit: Retrofit): MensaApi = retrofit.create()
    }
}
