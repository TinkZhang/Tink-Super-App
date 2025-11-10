package app.tinks.tink.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://rywzzcluqsdmtnilhkkf.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ5d3p6Y2x1cXNkbXRuaWxoa2tmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzEyOTQ1MDYsImV4cCI6MjA0Njg3MDUwNn0.W1wkpIRlK8cJA1CWtsxrhSUeLcDaMkAPbikxbrINiR8"
        ) {
            install(Postgrest)
        }
    }
}