package de.kaigothe.campusilmenau.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.data.local.CampusDatabase
import de.kaigothe.campusilmenau.data.local.MerklisteDao
import de.kaigothe.campusilmenau.data.local.ThoskaDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CampusDatabase =
        Room.databaseBuilder(context, CampusDatabase::class.java, CampusDatabase.NAME)
            .addMigrations(CampusDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideThoskaDao(database: CampusDatabase): ThoskaDao = database.thoskaDao()

    @Provides
    fun provideMerklisteDao(database: CampusDatabase): MerklisteDao = database.merklisteDao()
}
