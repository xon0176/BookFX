package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testKvdbConnection() {
    val logFile = File("/app/applet/app/src/test/java/com/example/test_output.txt")
    logFile.writeText("TEST_STARTED\n")

    fun log(msg: String) {
        println(msg)
        logFile.appendText(msg + "\n")
    }

    try {
        log("1. Fetching available domains from mail.tm...")
        val domUrl = URL("https://api.mail.tm/domains")
        val domConn = domUrl.openConnection() as HttpURLConnection
        domConn.requestMethod = "GET"
        domConn.connectTimeout = 8000
        domConn.readTimeout = 8000
        domConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        val domCode = domConn.responseCode
        log("Domains endpoint response code: $domCode")
        if (domCode != 200) {
            log("Failed to fetch domains: ${domConn.errorStream?.bufferedReader()?.readText()}")
            return
        }
        val domText = domConn.inputStream.bufferedReader().use { it.readText() }
        val domMatcher = Pattern.compile("\"domain\"\\s*:\\s*\"([^\"]+)\"").matcher(domText)
        if (!domMatcher.find()) {
            log("No domains found in response: $domText")
            return
        }
        val domain = domMatcher.group(1)
        log("Selected Domain: $domain")

        // Generate account info
        val randChars = (1..8).map { "abcdefghijklmnopqrstuvwxyz"[(Math.random() * 26).toInt()] }.joinToString("")
        val email = "kvdbverify$randChars@$domain"
        val password = "StrongPassword123!"
        log("Generated email address: $email")

        // 2. Create account on mail.tm
        log("2. Creating account on mail.tm...")
        val accUrl = URL("https://api.mail.tm/accounts")
        val accConn = accUrl.openConnection() as HttpURLConnection
        accConn.requestMethod = "POST"
        accConn.doOutput = true
        accConn.connectTimeout = 10000
        accConn.readTimeout = 10000
        accConn.setRequestProperty("Content-Type", "application/json")
        accConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        val accBody = """{"address":"$email","password":"$password"}"""
        accConn.outputStream.use { it.write(accBody.toByteArray(Charsets.UTF_8)) }
        val accCode = accConn.responseCode
        log("Account creation response code: $accCode")
        if (accCode !in 200..201) {
            log("Failed to create account: ${accConn.errorStream?.bufferedReader()?.readText()}")
            return
        }

        // 3. Get JWT token from mail.tm
        log("3. Authenticating and getting JWT token...")
        val tokUrl = URL("https://api.mail.tm/token")
        val tokConn = tokUrl.openConnection() as HttpURLConnection
        tokConn.requestMethod = "POST"
        tokConn.doOutput = true
        tokConn.connectTimeout = 10000
        tokConn.readTimeout = 10000
        tokConn.setRequestProperty("Content-Type", "application/json")
        tokConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        tokConn.outputStream.use { it.write(accBody.toByteArray(Charsets.UTF_8)) }
        val tokCode = tokConn.responseCode
        log("Token response code: $tokCode")
        if (tokCode != 200) {
            log("Failed to get token: ${tokConn.errorStream?.bufferedReader()?.readText()}")
            return
        }
        val tokText = tokConn.inputStream.bufferedReader().use { it.readText() }
        val tokMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(tokText)
        if (!tokMatcher.find()) {
            log("Failed to find token in JSON response: $tokText")
            return
        }
        val token = tokMatcher.group(1)
        log("Successfully retrieved auth bearer token!")

        // 4. Request the bucket from KVdb
        log("4. Requesting new bucket from KVdb...")
        val createUrl = URL("https://kvdb.io/")
        val createConn = createUrl.openConnection() as HttpURLConnection
        createConn.requestMethod = "POST"
        createConn.doOutput = true
        createConn.connectTimeout = 10000
        createConn.readTimeout = 10000
        createConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        createConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        val postData = "email=" + URLEncoder.encode(email, "UTF-8")
        createConn.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }
        
        val createCode = createConn.responseCode
        log("Create bucket response code: $createCode")
        if (createCode !in 200..299) {
            log("Failed to create bucket: ${createConn.errorStream?.bufferedReader()?.readText()}")
            return
        }
        val newBucketId = createConn.inputStream.bufferedReader().use { it.readText().trim() }
        log("Assigned KVdb Bucket ID: $newBucketId")

        // 5. Poll mail.tm for message
        log("5. Waiting and polling mail.tm inbox for verification email...")
        var activationLink: String? = null
        for (i in 1..20) {
            Thread.sleep(1500) // Sleep 1.5s (total 30s)
            log("Poll attempt $i...")
            
            val msgUrl = URL("https://api.mail.tm/messages")
            val msgConn = msgUrl.openConnection() as HttpURLConnection
            msgConn.requestMethod = "GET"
            msgConn.connectTimeout = 8000
            msgConn.readTimeout = 8000
            msgConn.setRequestProperty("Authorization", "Bearer $token")
            msgConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            
            val msgCode = msgConn.responseCode
            if (msgCode == 200) {
                val msgText = msgConn.inputStream.bufferedReader().use { it.readText() }
                log("Inbox messages count / check: ${msgText.length} bytes")
                
                val idMatcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(msgText)
                if (idMatcher.find()) {
                    val messageId = idMatcher.group(1)
                    log("Found email! Reading message ID: $messageId")
                    
                    // Retrieve message details
                    val detailUrl = URL("https://api.mail.tm/messages/$messageId")
                    val detailConn = detailUrl.openConnection() as HttpURLConnection
                    detailConn.requestMethod = "GET"
                    detailConn.connectTimeout = 8000
                    detailConn.readTimeout = 8000
                    detailConn.setRequestProperty("Authorization", "Bearer $token")
                    detailConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    
                    if (detailConn.responseCode == 200) {
                        val detailBody = detailConn.inputStream.bufferedReader().use { it.readText() }
                        log("Email body length: ${detailBody.length}")
                        
                        // Let's replace any escaped/backslashed slashes to make parsing dead simple
                        val cleanBody = detailBody.replace("\\/", "/").replace("\\\\/", "/")
                        
                        // Find "https://kvdb.io/login?token="
                        val tokenKey = "https://kvdb.io/login?token="
                        val index = cleanBody.indexOf(tokenKey)
                        if (index != -1) {
                            // Extract until the first ending character (double quote, whitespace, backslash, newline, or HTML tag etc.)
                            val tokenPart = cleanBody.substring(index)
                            val endMatcher = Pattern.compile("https://kvdb\\.io/login\\?token=[a-zA-Z0-9_=\\-\\.]+").matcher(tokenPart)
                            
                            if (endMatcher.find()) {
                                activationLink = endMatcher.group(0)
                                log("Extracted activation link successfully: $activationLink")
                                break
                            }
                        }
                        log("Token phrase not found in this email message. Trying again...")
                    }
                }
            } else {
                log("Poll request error code: $msgCode")
            }
        }

        if (activationLink == null) {
            log("Timeout: Activation email did not arrive.")
            return
        }

        // 6. Request activation GET
        log("6. Activating bucket via GET request...")
        val actUrl = URL(activationLink)
        val actConn = actUrl.openConnection() as HttpURLConnection
        actConn.requestMethod = "GET"
        actConn.connectTimeout = 8000
        actConn.readTimeout = 8000
        actConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        val actCode = actConn.responseCode
        log("Activation endpoint response: $actCode")
        val actBody = if (actCode in 200..299) {
            actConn.inputStream.bufferedReader().use { it.readText() }
        } else {
            actConn.errorStream?.bufferedReader()?.use { it.readText() }
        }
        log("Response body: $actBody")

        // 7. Verify PUT writes
        log("7. Verifying writeability on activated bucket...")
        val testUrl = URL("https://kvdb.io/$newBucketId/test_status_active")
        val testConn = testUrl.openConnection() as HttpURLConnection
        testConn.requestMethod = "PUT"
        testConn.doOutput = true
        testConn.connectTimeout = 10000
        testConn.readTimeout = 10000
        testConn.setRequestProperty("Content-Type", "application/json")
        testConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        
        testConn.outputStream.use { it.write("""{"verified_active": true}""".toByteArray(Charsets.UTF_8)) }
        val testCode = testConn.responseCode
        log("Test PUT response: $testCode")
        if (testCode in 200..299) {
            log("SUCCESS_VERIFIED_BUCKET_ID=$newBucketId")
        } else {
            log("Failed test PUT verification: ${testConn.errorStream?.bufferedReader()?.readText()}")
        }
    } catch (e: Exception) {
        log("Exception inside test execution: ${e.message}")
        e.printStackTrace()
    }
  }
}
