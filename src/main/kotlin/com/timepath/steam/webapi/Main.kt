package com.timepath.steam.webapi

import com.timepath.Logger
import com.timepath.web.api.base.Connection
import com.timepath.web.api.base.RequestBuilder
import org.json.JSONObject
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.logging.Level
import javax.crypto.Cipher
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.platform.platformStatic
import kotlin.properties.Delegates
import java.util.logging.Logger as JLogger

/**
 * http://code.google.com/p/pidgin-opensteamworks/source/browse/trunk/steam-mobile
 */
public class Main {

    val dlg = object : LoginDialog(null, true) {
        var prevUser: String? = null
        var cipherData: ByteArray by Delegates.notNull()
        var rsatimestamp: String by Delegates.notNull()
        var emailsteamid = ""
        var gid = -1L


        override fun login() {
            LOG.info { "Logging in" }
            val user = userInput.getText()
            val pass = String(passInput.getPassword()).toByteArray()
            val retry = prevUser?.let { it.equals(user, ignoreCase = true) } ?: false
            if (!retry) {
                prevUser = user
                val enc = encrypt(user, pass)
                cipherData = enc.first
                rsatimestamp = enc.second
                pass.fill(0)
            }
            JLogger.getLogger(javaClass<Connection>().getName()).setLevel(Level.WARNING)
            dologin(user, cipherData, rsatimestamp,
                    emailsteamid, steamguardInput.getText(),
                    gid, captchaInput.getText()).let {
                LOG.info { it.toString() }

                if (!it.getBoolean("success") || !it.has("oauth")) {
                    messageLabel.setText(it.getString("message"))
                    if (it.optBoolean("emailauth_needed")) {
                        emailsteamid = it.getString("emailsteamid")
                    } else if (it.optBoolean("captcha_needed")) {
                        gid = it.getLong("captcha_gid")
                        val address = "https://steamcommunity.com/public/captcha.php?gid=$gid"
                        captchaLabel.setIcon(ImageIcon(ImageIO.read(URI.create(address).toURL())))
                    }
                    LOG.info { "Login failed" }
                    return
                }

                dispose()
                run {
                    val token = it.getString("oauth").let { JSONObject(it) }.getString("oauth_token")
                    logon(token).let {
                        if (it.error != "OK") {
                            println(it.error)
                        }
                        LOG.info { it.toString() }
                        var lmid = it.message
                        while (true) {
                            Thread.sleep(1000)
                            poll(token, lmid).let {
                                if (it.error != "OK") {
                                    println(it.error)
                                }
                                lmid = it.messagelast
                                it.messages.forEach {
                                    println(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val LOG = Logger()

        fun encrypt(username: String, password: ByteArray): Pair<ByteArray, String> {
            val ret = RequestBuilder.fromArray(arrayOf(arrayOf("username", username))).let {
                Endpoint.COMMUNITY.postget("login/getrsakey", it.toString())
            }.let {
                JSONObject(it)
            }
            val keySpec = RSAPublicKeySpec(
                    BigInteger(ret.getString("publickey_mod"), 16),
                    BigInteger(ret.getString("publickey_exp"), 16)
            )
            val key = KeyFactory.getInstance("RSA").generatePublic(keySpec)
            val cipher = Cipher.getInstance(key.getAlgorithm())
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher.doFinal(password) to ret.getString("timestamp")
        }

        public platformStatic fun main(args: Array<String>): Unit = SwingUtilities.invokeLater {
            Main().dlg.let {
                it.pack()
                it.setLocationRelativeTo(null)
                it.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                it.setVisible(true)
                it
            }
        }
    }
}
