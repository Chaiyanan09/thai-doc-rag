package com.chaiyanan09.thairag.search;

import com.chaiyanan09.thairag.chunk.SearchHit;

import java.util.List;

public record SearchResponse(String query, List<SearchHit> results) {
}
