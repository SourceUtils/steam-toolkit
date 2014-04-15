package com.timepath.steam.webapi;

import com.timepath.web.api.base.Connection;
import com.timepath.web.api.base.RequestBuilder;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONObject;

/**
 *
 * http://code.google.com/p/pidgin-opensteamworks/source/browse/trunk/steam-mobile
 * <p>
 * @author TimePath
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String... args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }

    private final LoginDialog dlg;

    private String emailsteamid = "";

    private long gid = -1L;

    public Main() {
        dlg = new LoginDialog(null, true) {
            private Object[] enc;

            String prevattempt = null;

            @Override
            public void login() {
                LOG.info("Logging in");
                try {
                    String u = this.getUserInput().getText();
                    byte[] p = new String(this.getPassInput().getPassword()).getBytes();
                    if(prevattempt == null ? u.toLowerCase() != null : !prevattempt.toLowerCase().equals(u
                        .toLowerCase())) {
                        prevattempt = u;
                        enc = encrypt(u, p);
                    }
                    boolean loggedin = Main.this.login(u, gid, enc);
                    if(loggedin) {
                        this.dispose();
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

        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     *
     * @return [0] = (byte[]) token, [1] = (String) timestamp
     * <p/>
     * @throws Exception
     */
    private Object[] encrypt(String username, byte[] password) throws Exception {
        Connection login = new SteamConnection();
        RequestBuilder rb = RequestBuilder.fromArray(new String[][] {{"username", username}});

        JSONObject ret = new JSONObject(login.postget("login/getrsakey", rb.toString()));
        BigInteger mod = new BigInteger(ret.getString("publickey_mod"), 16);
        BigInteger exp = new BigInteger(ret.getString("publickey_exp"), 16);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(mod, exp);
        RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return new Object[] {cipher.doFinal(password), ret.getString("timestamp")};
    }

    private boolean login(final String u, final long g, final Object[] enc) throws Exception {
        final String captcha = dlg.getCaptchaInput().getText();
        final String eauth = dlg.getSteamguardInput().getText();
        final byte[] cipherData = (byte[]) enc[0];

        Connection login = new SteamConnection();
        String mobile = "";
        if(true) {
            mobile = "mobile";
        }
        final JSONObject ret = new JSONObject(login.postget(mobile + "login/dologin", RequestBuilder
                                                            .fromArray(
                                                                new Object[][] {
                                                                    // Credentials
                                                                    {"username", u},
                                                                    {"password", DatatypeConverter
                                                                     .printBase64Binary(cipherData)},
                                                                    {"rsatimestamp", enc[1]},
                                                                    // Steamguard
                                                                    {"emailauth", eauth},
                                                                    {"emailsteamid", emailsteamid},
                                                                    // Captcha
                                                                    {"captchagid", g},
                                                                    {"captcha_text", captcha},
                                                                    // User preference
                                                                    {"loginfriendlyname", "TimePath java test"},
                                                                    {"remember_login", true},
                                                                    // oauth
                                                                    {"oauth_client_id", "DE45CD61"}, // 3638BFB1, DE45CD61
                                                                    {"oauth_scope",
                                                                     "read_profile write_profile read_client write_client"},}
                                                            ).toString()));

        if(ret.getBoolean("success")) {
            if(ret.has("oauth")) {
                long umqid = (long) (Math.random() * Long.MAX_VALUE);
                JSONObject dict = new JSONObject(ret.getString("oauth"));
                System.out.println(dict);
                final String ret2 = login.postget("ISteamWebUserPresenceOAuth/Logon/v0001", RequestBuilder
                                                  .fromArray(
                                                      new Object[][] {
                                                          {"access_token", dict.getString("oauth_token")},
                                                          {"umqid", umqid}
                                                      }
                                                  ).toString());
                for(;;) {
                    Logger.getLogger(com.timepath.web.api.base.Connection.class.getName()).setLevel(Level.WARNING);
                    System.out.println(login.postget("ISteamWebUserPresenceOAuth/Poll/v0001", RequestBuilder
                                  .fromArray(
                                      new Object[][] {
                                          {"access_token", dict.getString("oauth_token")},
                                          {"umqid", umqid}
                                      }
                                  ).toString()));
                    Thread.sleep(1000);
                }
            } else if(ret.has("transfer_url")) {
                RequestBuilder rb = new RequestBuilder();
                String trans = ret.getString("transfer_url");
                JSONObject arr = ret.getJSONObject("transfer_parameters");
                Object[] keys = arr.keySet().toArray();
                for(Object k : keys) {
                    String key = k.toString();
                    rb.append(key, arr.get(key).toString());
                }
                System.out.println(trans + "?" + rb.toString());
            }
            return true;
        } else {
            dlg.getMessageLabel().setText(ret.getString("message"));
            if(ret.optBoolean("emailauth_needed")) {
                emailsteamid = ret.getString("emailsteamid");
            } else if(ret.optBoolean("captcha_needed")) {
                gid = ret.getLong("captcha_gid");
                String address = "https://steamcommunity.com/public/captcha.php?gid=" + gid;
                dlg.getCaptchaLabel().setIcon(new ImageIcon(ImageIO.read(URI.create(address).toURL())));
            }
        }
        return false;
    }

}
