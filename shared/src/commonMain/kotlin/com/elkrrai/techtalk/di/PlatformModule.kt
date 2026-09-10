package com.elkrrai.techtalk.di

import org.koin.core.module.Module

/** The only place platform types (AppDatabase, TipPackFileHandler, HttpClient) are bound. */
expect fun platformModule(): Module
