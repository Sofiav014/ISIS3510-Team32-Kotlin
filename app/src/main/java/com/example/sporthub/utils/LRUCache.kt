package com.example.sporthub.utils

class LRUCache<K, V>(private val maxSize: Int) {

    private val cache: LinkedHashMap<K, V> = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > maxSize
        }
    }

    operator fun get(key: K): V? = cache[key]

    operator fun set(key: K, value: V) {
        cache[key] = value
    }

    fun remove(key: K): V? = cache.remove(key)

    fun clear() = cache.clear()

    val keys: Set<K> get() = cache.keys
    val values: Collection<V> get() = cache.values
    val entries: Set<Map.Entry<K, V>> get() = cache.entries
    val size: Int get() = cache.size
}
