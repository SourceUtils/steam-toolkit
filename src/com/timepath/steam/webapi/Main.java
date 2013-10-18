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
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONObject;

/**
 *
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

    public Main() {
        dlg = new LoginDialog(null, true) {
            private Object[] enc;

            String prevattempt = null;

            @Override
            public void login() {
                try {
                    String u = this.getUserInput().getText();
                    byte[] p = new String(this.getPassInput().getPassword()).getBytes();
                    if(prevattempt == null ? u.toLowerCase() != null : !prevattempt.toLowerCase().equals(u.toLowerCase())) {
                        prevattempt = u;
                        enc = encrypt(u, p);
                    }
                    boolean loggedin = Main.this.login(u, gid, enc);
                    if(loggedin) {
                        this.dispose();
                        u = null;
                        Arrays.fill(p, 0, Math.min(0, p.length - 1), (byte) 0);
                        p = null;
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

    long gid = -1L;

    private final LoginDialog dlg;

    private String emailsteamid = "";

    /**
     *
     * @param username
     * @param password
     *                 <p>
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
        final JSONObject ret = new JSONObject(login.postget("login/dologin",
                                                            RequestBuilder.fromArray(
                new Object[][] {
                    {"password", DatatypeConverter.printBase64Binary(cipherData)},
                    {"username", u},
                    {"emailauth", eauth},
                    {"captchagid", g},
                    {"captcha_text", captcha},
                    {"emailsteamid", emailsteamid},
                    {"rsatimestamp", enc[1]},}).toString()));

        if(ret.getBoolean("success")) {
            RequestBuilder rb = new RequestBuilder();
            String trans = ret.getString("transfer_url");
            JSONObject arr = ret.getJSONObject("transfer_parameters");
            Object[] keys = arr.keySet().toArray();
            for(Object k : keys) {
                String key = k.toString();
                rb.append(key, arr.get(key).toString());
            }
            System.out.println(trans + "?" + rb.toString());
            return true;
        } else {
            dlg.getMessageLabel().setText(ret.getString("message"));
            if(ret.optBoolean("emailauth_needed")) {
                emailsteamid = ret.getString("emailsteamid");
            } else if(ret.optBoolean("captcha_needed")) {
                gid = ret.getLong("captcha_gid");
                String address = "https://steamcommunity.com/public/captcha.php?gid=" + gid;
                dlg.getCaptchaLabel().setIcon(new ImageIcon(ImageIO.read(
                        URI.create(address).toURL())));
            }
        }
        return false;
    }

}
