package com.example

import com.example.ai.BanglaNlpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBanglaDigitConversion() {
        val converted = BanglaNlpParser.convertBanglaDigits("১২৩৪৫৬৭৮৯০")
        assertEquals("1234567890", converted)
    }

    @Test
    fun testMultiItemBanglaExpenseExtraction() {
        val input = "আজকে মিনা বাজার থেকে ১ কেজি চাল ১৪০ টাকা, ১ লিটার তেল ১৮০ টাকা, ৫০০ টাকার মাছ কিনেছি"
        val result = BanglaNlpParser.parseMessage(input)

        assertEquals("create_expense", result.intent)
        assertNotNull(result.transaction)

        val tx = result.transaction!!
        assertEquals("EXPENSE", tx.type)
        assertEquals("মিনা বাজার", tx.shopName)
        assertEquals(3, tx.items.size)

        // 140 + 180 + 500 = 820
        assertEquals(820.0, tx.totalAmount, 0.01)

        val itemRice = tx.items.find { it.itemName.contains("চাল") }
        assertNotNull(itemRice)
        assertEquals(140.0, itemRice!!.totalPrice, 0.01)

        val itemOil = tx.items.find { it.itemName.contains("তেল") }
        assertNotNull(itemOil)
        assertEquals(180.0, itemOil!!.totalPrice, 0.01)

        val itemFish = tx.items.find { it.itemName.contains("মাছ") }
        assertNotNull(itemFish)
        assertEquals(500.0, itemFish!!.totalPrice, 0.01)
    }

    @Test
    fun testEnglishTransportExpense() {
        val input = "I spent 350 taka on transport today"
        val result = BanglaNlpParser.parseMessage(input)

        assertNotNull(result.transaction)
        val tx = result.transaction!!
        assertEquals("EXPENSE", tx.type)
        assertEquals(350.0, tx.totalAmount, 0.01)
        assertEquals("Transport", tx.categoryName)
    }

    @Test
    fun testBanglaIncomeExtraction() {
        val input = "গতকাল বেতন পেয়েছি ২৫,০০০ টাকা"
        val result = BanglaNlpParser.parseMessage(input)

        assertEquals("create_income", result.intent)
        assertNotNull(result.transaction)
        val tx = result.transaction!!
        assertEquals("INCOME", tx.type)
        assertEquals(25000.0, tx.totalAmount, 0.01)
        assertEquals("Salary", tx.categoryName)
    }

    @Test
    fun testContextualFollowUpShopUpdate() {
        val initialInput = "আজকে ৫০০ টাকার মাছ কিনেছি"
        val initialResult = BanglaNlpParser.parseMessage(initialInput)
        assertNotNull(initialResult.transaction)

        val followUpInput = "এটা রহমান স্টোর থেকে"
        val updatedResult = BanglaNlpParser.parseMessage(followUpInput, initialResult.transaction)

        assertEquals("follow_up_update", updatedResult.intent)
        assertNotNull(updatedResult.transaction)
        assertEquals("রহমান স্টোর", updatedResult.transaction!!.shopName)
        assertEquals(500.0, updatedResult.transaction!!.totalAmount, 0.01)
    }
}
