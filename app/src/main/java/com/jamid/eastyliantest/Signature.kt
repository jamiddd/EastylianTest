package com.jamid.eastyliantest

import okhttp3.internal.and
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signature {

	@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
	fun hmac(algorithm: String?, key: ByteArray?, message: ByteArray?): ByteArray {
		val mac = Mac.getInstance(algorithm)
		mac.init(SecretKeySpec(key, algorithm))
		return mac.doFinal(message)
	}

	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 * * @param data
	 * The data to be signed.
	 * @param key
	 * The signing key.
	 * @return
	 * The Base64-encoded RFC 2104-compliant HMAC signature.
	 * @throws
	 * java.security.SignatureException when signature generation fails
	 */
	/*@Throws(SignatureException::class)
	fun calculateRFC2104HMAC(data: String, secret: String): String {
		val result = try {
			// get an hmac_sha256 key from the raw secret bytes
			val signingKey = SecretKeySpec(secret.toByteArray(), HMAC_SHA256_ALGORITHM)

			// get an hmac_sha256 Mac instance and initialize with the signing key
			val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
			mac.init(signingKey)

			// compute the hmac on input data bytes
			val rawHmac: ByteArray = mac.doFinal(data.toByteArray())

			// base64-encode the hmac
			DatatypeConverter.printHexBinary(rawHmac).lowercase(Locale.getDefault())
		} catch (e: Exception) {
			throw SignatureException("Failed to generate HMAC : " + e.message)
		}
		return result
	}*/

	fun generateHashWithHmac256(message: String, key: String): String{
		return try {
			val hashingAlgorithm = "HmacSHA256" //or "HmacSHA1", "HmacSHA512"
			val bytes = hmac(hashingAlgorithm, key.toByteArray(), message.toByteArray())
			val messageDigest = bytesToHex(bytes)
			messageDigest
		} catch (e: java.lang.Exception) {
			e.printStackTrace()
			throw e
		}
	}

	private fun bytesToHex(bytes: ByteArray): String {
		val hexArray = "0123456789abcdef".toCharArray()
		val hexChars = CharArray(bytes.size * 2)
		var j = 0
		var v: Int
		while (j < bytes.size) {
			v = bytes[j] and 0xFF
			hexChars[j * 2] = hexArray[v ushr 4]
			hexChars[j * 2 + 1] = hexArray[v and 0x0F]
			j++
		}
		return String(hexChars)
	}
}