package com.example.biblioscan.sem_matching_API

import kotlinx.serialization.Serializable

@Serializable
data class MatchResponse(val matches: List<Match>)

@Serializable
data class Match(val title: String, val score: Double)