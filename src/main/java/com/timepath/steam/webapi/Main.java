package com.timepath.steam.webapi;

import com.timepath.web.api.base.Connection;
import com.timepath.web.api.base.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * http://code.google.com/p/pidgin-opensteamworks/source/browse/trunk/steam-mobile
 *
 * @author TimePath
 */
public class Main {

    private static final Logger LOG            = Logger.getLogger(Main.class.getName());
    private static final Logger LOG_CONNECTION = Logger.getLogger(Connection.class.getName());
    private final LoginDialog dlg;
    private String emailsteamid = "";
    private long   gid          = -1L;

    private Main() {
        dlg = new LoginDialog(null, true) {
            String prevattempt;
            private Object[] enc;

            @Override
            public void login() {
                LOG.info("Logging in");
                try {
                    String u = getUserInput().getText();
                    byte[] p = new String(getPassInput().getPassword()).getBytes();
                    if(( prevattempt == null ) || !prevattempt.toLowerCase().equals(u.toLowerCase())) {
                        prevattempt = u;
                        enc = encrypt(u, p);
                    }
                    boolean loggedin = tryLogin(u, gid, enc);
                    if(loggedin) {
                        dispose();
                        byte nul = 0;
                        Arrays.fill(p, 0, Math.min(0, p.length - 1), nul);
                        return;
                    }
                } catch(Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                LOG.info("Login failed");
            }
        };
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * @return [0] = (byte[]) token, [1] = (String) timestamp
     *
     * @throws Exception
     * @throws JSONException
     */
    private static Object[] encrypt(String username, byte... password) throws
                                                                       MalformedURLException,
                                                                       IllegalBlockSizeException,
                                                                       InvalidKeyException,
                                                                       NoSuchAlgorithmException,
                                                                       InvalidKeySpecException,
                                                                       NoSuchPaddingException,
                                                                       BadPaddingException
    {
        Connection login = new SteamConnection();
        RequestBuilder rb = RequestBuilder.fromArray(new String[][] { { "username", username } });
        JSONObject ret = new JSONObject(login.postget("login/getrsakey", rb.toString()));
        BigInteger mod = new BigInteger(ret.getString("publickey_mod"), 16);
        BigInteger exp = new BigInteger(ret.getString("publickey_exp"), 16);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec pubKeySpec = new RSAPublicKeySpec(mod, exp);
        Key key = keyFactory.generatePublic(pubKeySpec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return new Object[] { cipher.doFinal(password), ret.getString("timestamp") };
    }

    /**
     * @param u
     * @param g
     * @param enc
     *
     * @return
     *
     * @throws MalformedURLException
     * @throws IOException
     * @throws SecurityException
     * @throws InterruptedException
     * @throws JSONException
     */
    private boolean tryLogin(String u, long g, Object... enc) throws MalformedURLException, IOException, InterruptedException {
        String captcha = dlg.getCaptchaInput().getText();
        String eauth = dlg.getSteamguardInput().getText();
        byte[] cipherData = (byte[]) enc[0];
        Connection login = new SteamConnection();
        String mobile = "mobile";
        String req1 = RequestBuilder.fromArray(new Object[][] {
                                                       // Credentials
                                                       {
                                                               "username", u
                                                       }, {
                                                               "password", DatatypeConverter.printBase64Binary(cipherData)
                                                       }, {
                                                               "rsatimestamp", enc[1]
                                                       },
                                                       // Steamguard
                                                       {
                                                               "emailauth", eauth
                                                       }, {
                                                               "emailsteamid", emailsteamid
                                                       },
                                                       // Captcha
                                                       {
                                                               "captchagid", g
                                                       }, {
                                                               "captcha_text", captcha
                                                       },
                                                       // User
                                                       // preference
                                                       {
                                                               "loginfriendlyname", "TimePath java test"
                                                       }, {
                                                               "remember_login", true
                                                       },
                                                       // oauth
                                                       {
                                                               "oauth_client_id", "DE45CD61"
                                                       },
                                                       // 3638BFB1,
                                                       // DE45CD61
                                                       {
                                                               "oauth_scope", "read_profile " +
                                                                              "write_profile " +
                                                                              "read_client " +
                                                                              "write_client"
                                                       },
                                               }
                                              ).toString();
        JSONObject ret = new JSONObject(login.postget(mobile + "login/dologin", req1));
        if(ret.getBoolean("success")) {
            if(ret.has("oauth")) {
                long umqid = (long) ( Math.random() * Long.MAX_VALUE );
                JSONObject dict = new JSONObject(ret.getString("oauth"));
                LOG.info(String.valueOf(dict));
                String req2 = RequestBuilder.fromArray(new Object[][] {
                                                               {
                                                                       "access_token", dict.getString("oauth_token")
                                                               }, {
                                                                       "umqid", umqid
                                                               }
                                                       }
                                                      ).toString();
                String ret2 = login.postget("ISteamWebUserPresenceOAuth/Logon/v0001", req2);
                while(true) {
                    LOG_CONNECTION.setLevel(Level.WARNING);
                    LOG.info(login.postget("ISteamWebUserPresenceOAuth/Poll/v0001", RequestBuilder.fromArray(new Object[][] {
                                                                                                                     {
                                                                                                                             "access_token",
                                                                                                                             dict.getString(
                                                                                                                                     "oauth_token")
                                                                                                                     }, {
                                                                                                                             "umqid",
                                                                                                                             umqid
                                                                                                                     }
                                                                                                             }
                                                                                                            ).toString()
                                          ));
                    Thread.sleep(1000);
                }
            }
            if(ret.has("transfer_url")) {
                RequestBuilder rb = new RequestBuilder();
                String trans = ret.getString("transfer_url");
                JSONObject arr = ret.getJSONObject("transfer_parameters");
                Object[] keys = arr.keySet().toArray();
                for(Object key : keys) {
                    String keyStr = key.toString();
                    rb.append(keyStr, arr.get(keyStr).toString());
                }
                LOG.info(trans + '?' + rb);
            }
            return true;
        }
        dlg.getMessageLabel().setText(ret.getString("message"));
        if(ret.optBoolean("emailauth_needed")) {
            emailsteamid = ret.getString("emailsteamid");
        } else if(ret.optBoolean("captcha_needed")) {
            gid = ret.getLong("captcha_gid");
            String address = "https://steamcommunity.com/public/captcha.php?gid=" + gid;
            dlg.getCaptchaLabel().setIcon(new ImageIcon(ImageIO.read(URI.create(address).toURL())));
        }
        return false;
    }

    @SuppressWarnings("MethodNamesDifferingOnlyByCase")
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @SuppressWarnings("ResultOfObjectAllocationIgnored")
            @Override
            public void run() {
                new Main();
            }
        });
    }
}