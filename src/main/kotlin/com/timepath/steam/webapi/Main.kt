package com.timepath.steam.webapi

import com.timepath.web.api.base.Connection
import com.timepath.web.api.base.RequestBuilder
import org.json.JSONObject

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.imageio.ImageIO
import javax.swing.*
import javax.xml.bind.DatatypeConverter
import java.io.IOException
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URI
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates
import kotlin.platform.platformStatic

/**
 * http://code.google.com/p/pidgin-opensteamworks/source/browse/trunk/steam-mobile
 *
 * @author TimePath
 */
public class Main private() {
    private val dlg: LoginDialog = object : com.timepath.steam.webapi.LoginDialog(null, true) {
        var prevattempt: String? = null
        private var enc: Pair<ByteArray, String> by Delegates.notNull()

        override fun login() {
            LOG.info("Logging in")
            try {
                val u = userInput.getText()
                val p = String(passInput.getPassword()).toByteArray("UTF-8")
                if ((prevattempt == null) || prevattempt!!.toLowerCase() != u.toLowerCase()) {
                    prevattempt = u
                    enc = encrypt(u, *p)
                }
                val loggedin = tryLogin(u, gid, enc)
                if (loggedin) {
                    dispose()
                    p.fill(0)
                    return
                }
            } catch (ex: Exception) {
                Logger.getLogger(javaClass<Main>().getName()).log(Level.SEVERE, null, ex)
            }

            LOG.info("Login failed")
        }
    }.let {
        it.pack()
        it.setLocationRelativeTo(null)
        it.setVisible(true)
        it.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        it
    }

    private var emailsteamid = ""
    private var gid = -1L

    /**
     * @param u
     * @param g
     * @param enc
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws SecurityException
     * @throws InterruptedException
     * @throws JSONException
     */
    throws(javaClass<MalformedURLException>(), javaClass<IOException>(), javaClass<InterruptedException>())
    private fun tryLogin(u: String, g: Long, enc: Pair<ByteArray, String>): Boolean {
        val captcha = dlg.captchaInput.getText()
        val eauth = dlg.steamguardInput.getText()
        val cipherData = enc.first
        val login = com.timepath.steam.webapi.SteamConnection()
        val mobile = "mobile"
        val req1 = RequestBuilder.fromArray(array(
                // Credentials
                array<Any>("username", u),
                array<Any>("password", DatatypeConverter.printBase64Binary(cipherData)),
                array("rsatimestamp", enc.second),
                // Steamguard
                array<Any>("emailauth", eauth),
                array<Any>("emailsteamid", emailsteamid),
                // Captcha
                array<Any>("captchagid", g),
                array<Any>("captcha_text", captcha),
                // User
                // preference
                array<Any>("loginfriendlyname", "TimePath java test"),
                array<Any>("remember_login", true),
                // oauth: 3638BFB1, DE45CD61
                array<Any>("oauth_client_id", "DE45CD61"),
                array<Any>("oauth_scope", array("read_profile", "write_profile", "read_client",  "write_client").join(" "))
        )).toString()
        val ret = JSONObject(login.postget("${mobile}login/dologin", req1))
        if (ret.getBoolean("success")) {
            if (ret.has("oauth")) {
                val umqid = (Math.random() * java.lang.Long.MAX_VALUE.toDouble()).toLong()
                val dict = JSONObject(ret.getString("oauth"))
                LOG.info(dict.toString())
                val req2 = RequestBuilder.fromArray(array(array<Any>("access_token", dict.getString("oauth_token")), array<Any>("umqid", umqid))).toString()
                val ret2 = login.postget("ISteamWebUserPresenceOAuth/Logon/v0001", req2)
                while (true) {
                    LOG_CONNECTION.setLevel(Level.WARNING)
                    LOG.info(login.postget("ISteamWebUserPresenceOAuth/Poll/v0001", RequestBuilder.fromArray(array(array<Any>("access_token", dict.getString("oauth_token")), array<Any>("umqid", umqid))).toString()))
                    Thread.sleep(1000)
                }
            }
            if (ret.has("transfer_url")) {
                val rb = RequestBuilder()
                val trans = ret.getString("transfer_url")
                val arr = ret.getJSONObject("transfer_parameters")
                val keys = arr.keySet()
                for (key in keys) {
                    val keyStr = key.toString()
                    rb.append(keyStr, arr[keyStr].toString())
                }
                LOG.info("$trans?$rb")
            }
            return true
        }
        dlg.messageLabel.setText(ret.getString("message"))
        if (ret.optBoolean("emailauth_needed")) {
            emailsteamid = ret.getString("emailsteamid")
        } else if (ret.optBoolean("captcha_needed")) {
            gid = ret.getLong("captcha_gid")
            val address = "https://steamcommunity.com/public/captcha.php?gid=$gid"
            dlg.captchaLabel.setIcon(ImageIcon(ImageIO.read(URI.create(address).toURL())))
        }
        return false
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<Main>().getName())
        private val LOG_CONNECTION = Logger.getLogger(javaClass<Connection>().getName())

        /**
         * @throws Exception
         * @throws JSONException
         */
        throws(javaClass<MalformedURLException>(), javaClass<IllegalBlockSizeException>(), javaClass<InvalidKeyException>(), javaClass<NoSuchAlgorithmException>(), javaClass<InvalidKeySpecException>(), javaClass<NoSuchPaddingException>(), javaClass<BadPaddingException>())
        private fun encrypt(username: String, vararg password: Byte): Pair<ByteArray, String> {
            val login = com.timepath.steam.webapi.SteamConnection()
            val rb = RequestBuilder.fromArray(array(array("username", username)))
            val ret = JSONObject(login.postget("login/getrsakey", rb.toString()))
            val mod = BigInteger(ret.getString("publickey_mod"), 16)
            val exp = BigInteger(ret.getString("publickey_exp"), 16)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKeySpec = RSAPublicKeySpec(mod, exp)
            val key = keyFactory.generatePublic(pubKeySpec)
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher.doFinal(password) to ret.getString("timestamp")
        }

        public platformStatic fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                Main()
            }
        }
    }
}
