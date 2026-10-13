package com.example

import com.example.model.ExecutorAction
import com.example.model.ExecutorButton
import com.example.model.FaderItem
import com.example.model.LayoutConfiguration
import com.example.osc.OscDecoder
import com.example.osc.OscEncoder
import com.example.osc.OscMessage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OscEncodingTest {

    @Test
    fun testOscStringMessageEncodingAndDecoding() {
        val original = OscMessage("/gma2/cmd", "Go Exec 1.1")
        val bytes = OscEncoder.encode(original)

        // Must be multiple of 4 bytes
        assertEquals(0, bytes.size % 4)

        val decoded = OscDecoder.decode(bytes)
        assertNotNull(decoded)
        assertEquals("/gma2/cmd", decoded!!.address)
        assertEquals(1, decoded.arguments.size)
        assertEquals("Go Exec 1.1", decoded.arguments[0])
    }

    @Test
    fun testOscFloatAndIntArguments() {
        val original = OscMessage("/gma2/fader/1/1", 0.75f, 42)
        val bytes = OscEncoder.encode(original)

        assertEquals(0, bytes.size % 4)

        val decoded = OscDecoder.decode(bytes)
        assertNotNull(decoded)
        assertEquals("/gma2/fader/1/1", decoded!!.address)
        assertEquals(2, decoded.arguments.size)
        assertEquals(0.75f, (decoded.arguments[0] as Float), 0.001f)
        assertEquals(42, decoded.arguments[1])
    }

    @Test
    fun testLayoutJsonSerialization() {
        val layout = LayoutConfiguration(
            name = "Test Busking",
            description = "Test layout description",
            executorButtons = listOf(
                ExecutorButton("b1", 1, 201, "Strobe", ExecutorAction.FLASH, "#FF1744")
            ),
            faders = listOf(
                FaderItem("f1", 1, 1, "Main Wash", 0.8f, false, "#00E5FF")
            )
        )

        val root = JSONObject()
        root.put("name", layout.name)
        root.put("description", layout.description)

        val btnArray = JSONArray()
        for (btn in layout.executorButtons) {
            val bObj = JSONObject()
            bObj.put("id", btn.id)
            bObj.put("page", btn.page)
            bObj.put("execNumber", btn.execNumber)
            bObj.put("label", btn.label)
            bObj.put("action", btn.action.name)
            bObj.put("colorHex", btn.colorHex)
            btnArray.put(bObj)
        }
        root.put("executorButtons", btnArray)

        val jsonStr = root.toString()
        val parsed = JSONObject(jsonStr)

        assertEquals("Test Busking", parsed.getString("name"))
        val loadedBtns = parsed.getJSONArray("executorButtons")
        assertEquals(1, loadedBtns.length())
        assertEquals("Strobe", loadedBtns.getJSONObject(0).getString("label"))
        assertEquals("FLASH", loadedBtns.getJSONObject(0).getString("action"))
    }
}
