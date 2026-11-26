package de.kaigothe.campusilmenau.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.data.remote.mensa.MensaApi
import de.kaigothe.campusilmenau.data.nfc.NfcAvailability
import de.kaigothe.campusilmenau.data.nfc.NfcThoskaReader
import de.kaigothe.campusilmenau.data.nfc.SystemNfcAvailability
import de.kaigothe.campusilmenau.data.nfc.ThoskaReader
import de.kaigothe.campusilmenau.data.remote.events.EventsApi
import de.kaigothe.campusilmenau.data.remote.library.LibraryApi
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.MerklisteRepository
import de.kaigothe.campusilmenau.data.repository.NetworkLibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.RoomMerklisteRepository
import de.kaigothe.campusilmenau.data.repository.RoomThoskaRepository
import de.kaigothe.campusilmenau.data.security.CredentialStore
import de.kaigothe.campusilmenau.data.security.KeystoreCredentialStore
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.data.repository.EventsRepository
import de.kaigothe.campusilmenau.data.repository.NetworkEventsRepository
import de.kaigothe.campusilmenau.data.repository.RepositoryHeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.EmptyNoticesSource
import de.kaigothe.campusilmenau.data.repository.HeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.data.repository.NoticesSource
import de.kaigothe.campusilmenau.data.repository.NetworkMensaRepository
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMensaRepository(impl: NetworkMensaRepository): MensaRepository

    @Binds
    abstract fun bindHeuteEventsSource(impl: RepositoryHeuteEventsSource): HeuteEventsSource

    @Binds
    abstract fun bindNoticesSource(impl: EmptyNoticesSource): NoticesSource

    @Binds
    abstract fun bindEventsRepository(impl: NetworkEventsRepository): EventsRepository

    @Binds
    abstract fun bindLibraryCatalogRepository(impl: NetworkLibraryCatalogRepository): LibraryCatalogRepository

    @Binds
    abstract fun bindCredentialStore(impl: KeystoreCredentialStore): CredentialStore

    @Binds
    abstract fun bindThoskaRepository(impl: RoomThoskaRepository): ThoskaRepository

    @Binds
    abstract fun bindMerklisteRepository(impl: RoomMerklisteRepository): MerklisteRepository

    @Binds
    abstract fun bindThoskaReader(impl: NfcThoskaReader): ThoskaReader

    @Binds
    abstract fun bindNfcAvailability(impl: SystemNfcAvailability): NfcAvailability

    companion object {
        @Provides
        @Singleton
        fun provideMensaApi(retrofit: Retrofit): MensaApi = retrofit.create()

        @Provides
        @Singleton
        fun provideEventsApi(retrofit: Retrofit): EventsApi = retrofit.create()

        @Provides
        @Singleton
        fun provideLibraryApi(retrofit: Retrofit): LibraryApi = retrofit.create()
    }
}
