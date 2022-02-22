package org.neshan.data.database

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Test

class DataConverterTest {

    private lateinit var dataConverter: DataConverter

    @Before
    fun initDataConverter() {
        dataConverter = DataConverter()
    }

    @Test
    fun fromStringList_emptyOrNullList_returnEmpty() {
        val result1 = dataConverter.fromStringList(null)
        val result2 = dataConverter.fromStringList(listOf())

        assertThat(result1, `is`(""))
        assertThat(result2, `is`(""))
    }

    @Test
    fun fromStringList_stringItems_returnJson() {
        val result1 = dataConverter.fromStringList(listOf("1", "2", "3"))
        val result2 = dataConverter.fromStringList(listOf("1,", "2", "3"))
        val result3 = dataConverter.fromStringList(listOf(null, "2", "3"))

        assertThat(result1, `is`("[\"1\",\"2\",\"3\"]"))
        assertThat(result2, `is`("[\"1,\",\"2\",\"3\"]"))
        assertThat(result3, `is`("[null,\"2\",\"3\"]"))
    }

    @Test
    fun toStringList_emptyJson_returnEmptyList() {
        val result1 = dataConverter.toStringList("")
        val result2 = dataConverter.toStringList(null)

        assertThat(isEqual(result1, listOf()) , `is`(true))
        assertThat(isEqual(result2, listOf()) , `is`(true))
    }

    @Test
    fun toStringList_jsonItems_returnStringList() {
        val result1 = dataConverter.toStringList("[\"1\",\"2\",\"3\"]")
        val result2 = dataConverter.toStringList("[\"1,\",\"2\",\"3\"]")
        val result3 = dataConverter.toStringList("[null,\"2\",\"3\"]")

        assertThat(isEqual(result1, listOf("1", "2", "3")) , `is`(true))
        assertThat(isEqual(result2, listOf("1,", "2", "3")) , `is`(true))
        assertThat(isEqual(result3, listOf(null, "2", "3")) , `is`(true))
    }

    private fun<T> isEqual(first: List<T>, second: List<T>): Boolean {

        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (x, y) -> x == y }
    }
}